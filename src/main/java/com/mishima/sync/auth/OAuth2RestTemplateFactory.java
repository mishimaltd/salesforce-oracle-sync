package com.mishima.sync.auth;

import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;

public class OAuth2RestTemplateFactory {

  public static OAuth2RestTemplate oauth2RestTemplate(
          String tokenUri,
          String clientId,
          String clientSecret,
          String username,
          String password) {

    ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
    resourceDetails.setGrantType("password");
    resourceDetails.setAccessTokenUri(tokenUri);
    resourceDetails.setClientAuthenticationScheme(AuthenticationScheme.query);
    resourceDetails.setClientId(clientId);
    resourceDetails.setClientSecret(clientSecret);
    resourceDetails.setUsername(username);
    resourceDetails.setPassword(password);
    OAuth2RestTemplate template = new OAuth2RestTemplate(resourceDetails);
    template.getMessageConverters().add(0, new GsonHttpMessageConverter());
    return template;
  };

}
