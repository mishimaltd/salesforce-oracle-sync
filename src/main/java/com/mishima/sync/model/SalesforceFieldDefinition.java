package com.mishima.sync.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SalesforceFieldDefinition {

  private String name;
  private String type;
  private long length;
  private long byteLength;
  private long digits;

}
