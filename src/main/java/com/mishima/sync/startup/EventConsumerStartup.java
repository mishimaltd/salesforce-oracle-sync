package com.mishima.sync.startup;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.mishima.sync.exception.MessageParseException;
import com.mishima.sync.exception.MessageSequencingException;
import com.mishima.sync.model.ChangeCaptureEvent;
import com.mishima.sync.repository.OracleRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cloud")
@Slf4j
public class EventConsumerStartup implements ApplicationRunner {

  // Maximum number of retries for an update before giving up
  private static final int UPDATE_RETRY_ATTEMPTS = 3;

  @Value("${sqs.url}")
  private String sqsUrl;

  @Autowired
  private AmazonSQS amazonSQS;

  @Autowired
  private OracleRepository oracleRepository;

  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private final Gson gson = new Gson();
  private final Type listType = new TypeToken<ArrayList<ChangeCaptureEvent>>(){}.getType();

  private final Map<String,Integer> updateRetryMessagesCache = new HashMap<>();
  private final Set<String> deletedItemsCache = new HashSet<>();

  private boolean runnable = true;

  @PreDestroy
  private void destroy() {
    log.info("Shutting down");
    amazonSQS.shutdown();
    runnable = false;
  }

  @Override
  public void run(ApplicationArguments args) {

    ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(sqsUrl)
        .withWaitTimeSeconds(10)
        .withMaxNumberOfMessages(1);

    new Thread(() -> {
      while(runnable) {
        try {
          amazonSQS.receiveMessage(receiveMessageRequest).getMessages().forEach(message -> {
            log.info("Received message id {}", message.getMessageId());
            try {
              List<ChangeCaptureEvent> changeCaptureEvents = deserialize(message);
              if (isBatchUpdate(changeCaptureEvents)) {
                processInBulk(message, changeCaptureEvents);
              } else {
                processIndividually(message, changeCaptureEvents);
              }
            } catch( MessageParseException ex ) {
              log.error("Error parsing message: {} -> {}", message.getBody(), ex.getMessage());
            }
            deleteMessage(message);
          });
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }).start();
  }

  private List<ChangeCaptureEvent> deserialize(Message message) throws MessageParseException {
    try {
      return gson.fromJson(message.getBody(), listType);
    } catch(Exception ex) {
      throw new MessageParseException(ex);
    }
  }

  private void processInBulk(Message message, List<ChangeCaptureEvent> events) {
    log.info("Processing bulk update of {} events", events.size());
    try {
      oracleRepository.bulkDoCreate(events);
    } catch( PersistenceException ex ) {
      if(ex.getCause() instanceof ConstraintViolationException) {
        log.warn("Caught duplicate key exception, will process batch individually...");
        processIndividually(message, events);
      }
    }
  }

  private void processIndividually(Message message, List<ChangeCaptureEvent> events) {
    events.forEach(e -> {
      log.info("Processing event {}", e);
      switch(e.getAction()) {
        case "C":
          handleCreate(e);
          break;
        case "U":
          try {
            handleUpdate(e);
          } catch (MessageSequencingException ex) {
            log.info("Error occurred processing update of message id {}", message.getMessageId());
            attemptRetry(message.getBody());
          }
          break;
        case "D":
          handleDelete(e);
          break;
        default:
          log.error("Invalid action {}", e.getAction());
      }
    });
  }

  private void handleCreate(ChangeCaptureEvent e) {
    try {
      log.info("Processing create for object type {} id {} from user {}", e.getObjectType(), e.getObjectId(), e.getUserName());
      oracleRepository.doCreate(e);
    } catch(PersistenceException ex) {
      if(ex.getCause() instanceof ConstraintViolationException) {
        log.warn("Already found object type {} with id {}, updating instead", e.getObjectType(), e.getObjectId());
        oracleRepository.doUpdate(e);
      } else {
        throw ex;
      }
    }
  }

  private void handleUpdate(ChangeCaptureEvent e) throws MessageSequencingException {
    log.info("Processing update for object type {} id {} from user {}", e.getObjectType(), e.getObjectId(), e.getUserName());
    if(deletedItemsCache.contains(e.getObjectId())) {
      log.info("Object id {} already deleted, discarding update..", e.getObjectId());
    } else if(oracleRepository.doUpdate(e) == 0) {
      throw new MessageSequencingException();
    }
  }

  private void handleDelete(ChangeCaptureEvent e) {
    log.info("Processing delete for object type {} id {} from user {}", e.getObjectType(), e.getObjectId(), e.getUserName());
    if(oracleRepository.doDelete(e) == 0) {
      log.warn("No matching record found for delete, discarding..");
    } else {
      deletedItemsCache.add(e.getObjectId()); // Add delete to cache
    }
  }

  private void attemptRetry(String messageBody) {
    int retries = updateRetryMessagesCache.getOrDefault(messageBody, 1);
    if( retries < UPDATE_RETRY_ATTEMPTS) {
      log.info("Will push message back onto the queue, retry attempt {}", retries);
      executorService.schedule(() -> {amazonSQS.sendMessage(sqsUrl, messageBody);}, 250, TimeUnit.MILLISECONDS);
      updateRetryMessagesCache.put(messageBody, ++retries);
    } else {
      log.warn("Could not process update message after {} retries, discarding.", retries);
    }
  }

  private void deleteMessage(Message message) {
    amazonSQS.deleteMessage(new DeleteMessageRequest().withQueueUrl(sqsUrl).withReceiptHandle(message.getReceiptHandle()));
    log.info("Deleted processed message id {}", message.getMessageId());
  }

  private boolean isBatchUpdate(List<ChangeCaptureEvent> events) {
    return events.stream().allMatch(e -> "batch".equals(e.getUserName()) && "C".equals(e.getAction()));
  }

}
