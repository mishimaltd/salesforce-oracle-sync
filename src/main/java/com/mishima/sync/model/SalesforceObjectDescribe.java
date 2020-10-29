package com.mishima.sync.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class SalesforceObjectDescribe {

  private String name;

  private List<SalesforceFieldDefinition> fields;

}
