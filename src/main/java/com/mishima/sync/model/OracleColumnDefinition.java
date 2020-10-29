package com.mishima.sync.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class OracleColumnDefinition {

  private String name;
  private String dataType;
  private boolean isPrimaryKey;

}
