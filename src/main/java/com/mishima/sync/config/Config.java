package com.mishima.sync.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.mishima.sync.auth.OAuth2RestTemplateFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

@Configuration
public class Config {

  @Value("${aws.accessKey}")
  private String awsAccessKey;

  @Value("${aws.secretKey}")
  private String awsSecretKey;

  @Bean
  public AmazonSQS amazonSQS() {
    AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
    return AmazonSQSClientBuilder.standard()
        .withCredentials(awsCredentialsProvider)
        .withRegion(Regions.US_EAST_1)
        .build();
  }

  @Bean
  @Profile("test")
  public OAuth2RestTemplate restTemplate(
      @Value("${oauth.tokenuri}") String tokenUri,
      @Value("${oauth.clientId}") String clientId,
      @Value("${oauth.clientSecret}") String clientSecret,
      @Value("${oauth.username}") String username,
      @Value("${oauth.password}") String password) {
    return OAuth2RestTemplateFactory
        .oauth2RestTemplate(tokenUri, clientId, clientSecret, username, password);
  }

}
