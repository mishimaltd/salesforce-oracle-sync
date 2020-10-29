package com.mishima.sync.controller;

import com.mishima.sync.auth.OAuth2RestTemplateFactory;
import com.mishima.sync.model.OracleTableDefinition;
import com.mishima.sync.model.OracleTableSchemaRequest;
import com.mishima.sync.model.SalesforceObjectDescribe;
import com.mishima.sync.service.SalesforceObjectSchemaService;
import com.mishima.sync.util.OdsSchemaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OdsTableMappingController {

  @RequestMapping(value = "/ods/ddl/generate", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  public String generateOdsTableSchema(@RequestBody OracleTableSchemaRequest oracleTableSchemaRequest) {
    log.info("Generating ods table defintion for object {}", oracleTableSchemaRequest.getObjectName());
    OAuth2RestTemplate template = OAuth2RestTemplateFactory.oauth2RestTemplate(
        oracleTableSchemaRequest.getTokenUri(),
        oracleTableSchemaRequest.getClientId(),
        oracleTableSchemaRequest.getClientSecret(),
        oracleTableSchemaRequest.getUsername(),
        oracleTableSchemaRequest.getPassword());
    SalesforceObjectSchemaService service = new SalesforceObjectSchemaService(template, oracleTableSchemaRequest
        .getSalesforceBaseUri());
    SalesforceObjectDescribe describe = service.extractSchemaForObject(oracleTableSchemaRequest.getObjectName());
    OracleTableDefinition oracleTableDefinition = OdsSchemaUtils.mapToOdsTableDefinition(describe);
    return oracleTableDefinition.generateDDL();
  }

}
