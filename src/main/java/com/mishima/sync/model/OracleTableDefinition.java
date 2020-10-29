package com.mishima.sync.model;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

@Builder
@Getter
@Setter
@ToString
public class OracleTableDefinition {

  private String name;
  private List<OracleColumnDefinition> columns;

  public String getPrimaryKey() {
    List<String> primaryKeys = columns.stream().filter(
        OracleColumnDefinition::isPrimaryKey).map(
        OracleColumnDefinition::getName).collect(Collectors.toList());
    if(primaryKeys.isEmpty()) {
      throw new IllegalStateException("Table must have a primary key defined");
    }
    return StringUtils.collectionToCommaDelimitedString(primaryKeys);
  }

  public String generateDDL() {
    StringBuilder sb = new StringBuilder("CREATE TABLE ").append(name).append("\n(\n");
    columns.forEach(odsColumnDefinition ->
        sb.append("\t")
        .append(odsColumnDefinition.getName())
        .append(" ").append(odsColumnDefinition.getDataType())
        .append(",\n"));
    sb.append("\tPRIMARY KEY(")
        .append(getPrimaryKey())
        .append(")\n);");
    return sb.toString();
  }

}
