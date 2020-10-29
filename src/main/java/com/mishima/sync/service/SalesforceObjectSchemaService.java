package com.mishima.sync.service;

import com.mishima.sync.model.SalesforceObjectDescribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

@Slf4j
public class SalesforceObjectSchemaService {

  private final OAuth2RestTemplate oAuth2RestTemplate;

  private final String salesforceQueryUri;

  public SalesforceObjectSchemaService(@Autowired OAuth2RestTemplate oAuth2RestTemplate, @Value("${salesforce.query.uri}") String salesforceQueryUri) {
    this.oAuth2RestTemplate = oAuth2RestTemplate;
    this.salesforceQueryUri = salesforceQueryUri;
  }

  public SalesforceObjectDescribe extractSchemaForObject(String objectName) {
    log.info("Retrieving schema description for objectType: {}", objectName);
    String uri = salesforceQueryUri + "/services/data/v42.0/sobjects/" + objectName + "/describe";
    return oAuth2RestTemplate.getForObject(uri, SalesforceObjectDescribe.class);
  }

}
