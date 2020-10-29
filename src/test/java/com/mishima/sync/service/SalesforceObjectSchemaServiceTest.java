package com.mishima.sync.service;

import com.mishima.sync.model.OracleTableDefinition;
import com.mishima.sync.model.SalesforceObjectDescribe;
import com.mishima.sync.util.OdsSchemaUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class SalesforceObjectSchemaServiceTest {

  @Autowired
  OAuth2RestTemplate oAuth2RestTemplate;

  @Value("${salesforce.query.uri}")
  private String salesforceQueryUri;

  @Test
  public void testGetSchemaForObject() {
    SalesforceObjectSchemaService service = new SalesforceObjectSchemaService(oAuth2RestTemplate, salesforceQueryUri);
    String objectName = "pdlm__item_revision__c";
    SalesforceObjectDescribe describe = service.extractSchemaForObject(objectName);
    OracleTableDefinition oracleTableDefinition = OdsSchemaUtils.mapToOdsTableDefinition(describe);
    log.info("Table Definition:\n{}", oracleTableDefinition.generateDDL());
  }



}
