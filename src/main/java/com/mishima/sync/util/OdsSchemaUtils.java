package com.mishima.sync.util;

import com.mishima.sync.model.OracleColumnDefinition;
import com.mishima.sync.model.OracleTableDefinition;
import com.mishima.sync.model.SalesforceFieldDefinition;
import com.mishima.sync.model.SalesforceObjectDescribe;
import java.util.stream.Collectors;

public class OdsSchemaUtils {

  public static String mapObjectTypeToOdsTableName(String objectType) {
    return objectType.toLowerCase().substring(0, Math.min(objectType.length(), 30) -3) + "__c";
  }

  public static String mapObjectNameToOdsColumnName(String objectName) {
    if(objectName.length() > 30 ) {
      return objectName.replace("__c","").substring(0,27) + "_t";
    } else {
      return objectName;
    }
  }

  public static OracleTableDefinition mapToOdsTableDefinition(SalesforceObjectDescribe sfObject) {
    return OracleTableDefinition.builder()
        .name(mapObjectTypeToOdsTableName(sfObject.getName()).toUpperCase())
        .columns( sfObject.getFields().stream().map(
            OdsSchemaUtils::getOdsColumnDefinition).collect(Collectors.toList()))
        .build();
  }


  public static OracleColumnDefinition getOdsColumnDefinition(SalesforceFieldDefinition definition) {
    return OracleColumnDefinition.builder()
        .name(OdsSchemaUtils.mapObjectNameToOdsColumnName(definition.getName()).toUpperCase())
        .dataType(getOracleDataType(definition))
        .isPrimaryKey("Id".equals(definition.getName()))
        .build();
  }

  private static String getOracleDataType(SalesforceFieldDefinition definition) {
    switch(definition.getType()) {
      case "id":
      case "reference":
        return "VARCHAR2(18 BYTE)";
      case "string":
      case "picklist":
      case "url":
        return "VARCHAR2(" + definition.getByteLength() + " BYTE)";
      case "boolean":
      case "double":
      case "percent":
      case "currency":
        return "NUMBER";
      case "datetime":
        return "TIMESTAMP(6)";
      case "date":
        return "DATE";
      case "textarea":
        return definition.getLength() <= 4000? "VARCHAR2(4000 BYTE)": "CLOB";
      default:
        throw new IllegalArgumentException("Unsupported type: " + definition.getType());
    }
  }


}
