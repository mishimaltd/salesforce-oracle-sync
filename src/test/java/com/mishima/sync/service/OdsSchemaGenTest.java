package com.mishima.sync.service;

import com.mishima.sync.model.OracleTableDefinition;
import com.mishima.sync.model.SalesforceObjectDescribe;
import com.mishima.sync.util.OdsSchemaUtils;
import java.util.Arrays;
import java.util.List;
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
public class OdsSchemaGenTest {

  @Autowired
  OAuth2RestTemplate oAuth2RestTemplate;

  @Value("${salesforce.query.uri}")
  private String salesforceQueryUri;

  private final List<String> objectNames = Arrays.asList(
      "Identifier__c",
      "Inclusion_Set_Collection__c",
      "Item_to_Offering_Relationships__c",
      "Offering_to_Price_Plan__c",
      "Offering_to_Upgrade_Offering__c",
      "Party__c",
      "PDLM__Assembly__c",
      "PDLM__Category__c",
      "PDLM__Item__c",
      "PDLM__Item_Revision__c",
      "Price_Plan_to_Onetime_Charges__c",
      "Product_to_LV_Association__c",
      "Product_to_Party_Association__c",
      "Related_Item_Association__c"
  );

  @Test
  public void testGenerateOdsSchemaScript() {
    SalesforceObjectSchemaService service = new SalesforceObjectSchemaService(oAuth2RestTemplate, salesforceQueryUri);
    StringBuilder sb = new StringBuilder("-- SCRIPT TO REGENERATE ODS SCHEMA TABLES");
    objectNames.forEach(objectName -> {
      sb.append("\n\n\nDROP TABLE ").append(OdsSchemaUtils.mapObjectTypeToOdsTableName(objectName).toUpperCase()).append(";\n\n");
      SalesforceObjectDescribe describe = service.extractSchemaForObject(objectName);
      OracleTableDefinition tableDefinition = OdsSchemaUtils.mapToOdsTableDefinition(describe);
      sb.append(tableDefinition.generateDDL());
    });
    sb.append("\n\n\n-- END SCRIPT");
    log.info("Generated Script:\n\n{}", sb.toString());
  }

}
