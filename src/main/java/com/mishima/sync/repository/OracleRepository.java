package com.mishima.sync.repository;

import static com.mishima.sync.util.OdsSchemaUtils.mapObjectNameToOdsColumnName;
import static com.mishima.sync.util.OdsSchemaUtils.mapObjectTypeToOdsTableName;

import com.mishima.sync.model.ChangeCaptureEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


@Component
@Transactional
@Profile("!test")
@Slf4j
public class OracleRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Value("${spring.datasource.username}")
  private String odsSchemaName;

  private final Map<String, SortedSet<ColumnMetadata>> oracleSchemaMetadata = new HashMap<>();
  private final Map<String, Map<String,String>> oracleSchemaColumnDatatypes = new HashMap<>();

  @PostConstruct
  private void init() {
    initializeOracleSchemaColumns();
  }

  public int doCreate(ChangeCaptureEvent e) {
    log.debug("Processing create for objectType {} id {}", e.getObjectType(), e.getObjectId());
    Map<String,Object> payload = convertPayload(e);
    String odsTableName = mapObjectTypeToOdsTableName(e.getObjectType());
    final String[] delimiter = {""};
    final StringBuilder queryBuilder = new StringBuilder("INSERT INTO " + odsTableName + "(");
    oracleSchemaMetadata.get(odsTableName).forEach(metadata -> {
      if(payload.getOrDefault(metadata.getColumnName(), null) != null) {
        queryBuilder.append(delimiter[0]).append(metadata.getColumnName());
        delimiter[0] = ", ";
      }
    });
    queryBuilder.append(") VALUES (");
    delimiter[0] = "";
    final Integer[] paramIndex = {0};
    oracleSchemaMetadata.get(odsTableName).forEach(metadata -> {
      if(payload.getOrDefault(metadata.getColumnName(), null) != null) {
        queryBuilder.append(delimiter[0]).append(wrapParameter(odsTableName, metadata.getColumnName(), paramIndex[0]++));
        delimiter[0] = ", ";
      }
    });
    queryBuilder.append(")");
    String queryString = queryBuilder.toString();
    log.debug("Generated query: {}", queryString);
    paramIndex[0] = 0;
    final Query query = entityManager.createNativeQuery(queryString);
    oracleSchemaMetadata.get(mapObjectTypeToOdsTableName(e.getObjectType())).forEach(metadata -> {
        if(payload.getOrDefault(metadata.getColumnName(), null) != null) {
          query.setParameter("param_" + paramIndex[0]++, convertBooleanToNumber(payload.get(metadata.getColumnName())));
        }
      });
      return query.executeUpdate();
  }

  public int doUpdate(ChangeCaptureEvent e) {
    log.debug("Processing update for objectType {} id {}", e.getObjectType(), e.getObjectId());
    Map<String,Object> payload = convertPayload(e);
    String odsTableName = mapObjectTypeToOdsTableName(e.getObjectType());
    final String[] delimiter = {""};
    final StringBuilder queryBuilder = new StringBuilder("UPDATE " + odsTableName + " SET ");
    final Integer[] paramIndex = {0};
    oracleSchemaMetadata.get(odsTableName).forEach(metadata -> {
      queryBuilder.append(delimiter[0]).append(metadata.getColumnName()).append(" = ");
      if(payload.getOrDefault(metadata.getColumnName(), null) == null) {
        queryBuilder.append("null");
      } else {
        queryBuilder.append(wrapParameter(odsTableName, metadata.getColumnName(), paramIndex[0]++));
      }
      delimiter[0] = ", ";
    });
    queryBuilder.append(" WHERE id = :objectId");
    String queryString = queryBuilder.toString();
    log.debug("Generated query: " + queryString);
    paramIndex[0] = 0;
    Query query = entityManager.createNativeQuery(queryString);
    oracleSchemaMetadata.get(mapObjectTypeToOdsTableName(e.getObjectType())).forEach(metadata -> {
      if(payload.getOrDefault(metadata.getColumnName(), null) != null) {
        query.setParameter("param_" + paramIndex[0]++, convertBooleanToNumber(payload.get(metadata.getColumnName())));
      }
    });
    query.setParameter("objectId", e.getObjectId());

    return query.executeUpdate();
  }

  public int doDelete(ChangeCaptureEvent e) {
    log.debug("Processing delete for objectType {} id {}", e.getObjectType(), e.getObjectId());
    return entityManager.createNativeQuery(
        "DELETE FROM " + mapObjectTypeToOdsTableName(e.getObjectType()) + " WHERE id = :objectId")
        .setParameter("objectId", e.getObjectId())
        .executeUpdate();
  }

  public void bulkDoCreate(List<ChangeCaptureEvent> events) {
    log.debug("Processing bulk update of {} events", events.size());
    events.forEach(this::doCreate);
  }

  private void initializeOracleSchemaColumns() {
    log.info("Initializing schema mapping");
    @SuppressWarnings("unchecked")
    List<Object[]> results = entityManager.createNativeQuery("SELECT\n"
        + "LOWER(t.table_name) tableName,\n"
        + "LOWER(c.column_name) columnName,\n"
        + "LOWER(c.data_type) dataType\n"
        + "FROM\n"
        + "all_tables t,\n"
        + "all_tab_cols c\n"
        + "WHERE t.owner = :schemaOwner\n"
        + "AND t.table_name = c.table_name\n")
        .setParameter("schemaOwner", odsSchemaName)
        .getResultList();
    results.forEach(objects -> {
      String tableName = (String)objects[0];
      String columnName = (String)objects[1];
      String dataType = (String)objects[2];
      if(!columnName.startsWith("_")) {
        oracleSchemaMetadata.putIfAbsent(tableName, new TreeSet<>());
        oracleSchemaMetadata
            .get(tableName).add(ColumnMetadata.builder().columnName(columnName).dataType(dataType).build());
        oracleSchemaColumnDatatypes.putIfAbsent(tableName, new HashMap<>());
        oracleSchemaColumnDatatypes.get(tableName).put(columnName, dataType);
      }
    });
    log.info("Loaded column metadata for {} tables", oracleSchemaMetadata.size());
  }

  private Map<String,Object> convertPayload(ChangeCaptureEvent e) {
    Map<String,Object> convertedPayload = new HashMap<>();
    e.getFullPayload().keySet().forEach(objectName ->
        convertedPayload.put(mapObjectNameToOdsColumnName(objectName), e.getFullPayload().get(objectName)));
    return convertedPayload;
  }

  private Object convertBooleanToNumber(Object value) {
    return value == null? null: value instanceof Boolean? (Boolean)value? 1: 0: value;
  }

  private String wrapParameter(String odsTableName, String odsColumnName, int paramIndex) {
    String dataType = oracleSchemaColumnDatatypes.get(odsTableName).get(odsColumnName);
    if(dataType.startsWith("timestamp")) {
      return "TO_TIMESTAMP(:param_" + paramIndex + ",'YYYY-MM-DD\"T\"HH24:MI:SS.FF\"Z\"')";
    } else if(dataType.equals("date")) {
      return "TO_DATE(:param_" + paramIndex + ",'YYYY-MM-DD')";
    } else {
      return ":param_" + paramIndex;
    }
  }

  @Getter
  @Builder
  private static class ColumnMetadata implements Comparable<ColumnMetadata> {

    private final String columnName;
    private final String dataType;

    public int compareTo(ColumnMetadata o) {
      if (dataType.equals("clob") && o.dataType.equals("clob")) {
        return columnName.compareTo(o.columnName);
      } else if (dataType.equals("clob")) {
        return 1; // Always last;
      } else if (o.dataType.equals("clob")) {
        return -1; // Always last;
      } else {
        return columnName.compareTo(o.columnName);
      }
    }

  }

}
