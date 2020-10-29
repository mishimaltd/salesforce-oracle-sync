package com.mishima.sync.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class OracleTableSchemaRequest {

  private String objectName;
  private String salesforceBaseUri;

  private String tokenUri;
  private String clientId;
  private String clientSecret;
  private String username;
  private String password;

}
