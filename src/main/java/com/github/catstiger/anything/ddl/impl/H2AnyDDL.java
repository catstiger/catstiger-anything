package com.github.catstiger.anything.ddl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.google.common.base.Preconditions;

public class H2AnyDDL extends AbstractAnyDDL {

  @Override
  public boolean isTableExists(String tableName) {
    Preconditions.checkNotNull(tableName);

    Long count = getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME=?", Long.class,
        StringUtils.upperCase(tableName));

    return (count != null && count > 0L);
  }

  @Override
  public List<String> tableNameLike(String tableName) {
    Preconditions.checkNotNull(tableName);

    List<Map<String, Object>> tables = getJdbcTemplate().queryForList("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE ?",
        "%" + tableName + "%");
    List<String> tablenames = new ArrayList<String>(tables.size());
    for (Map<String, Object> item : tables) {
      tablenames.add((String) item.get("TABLE_NAME"));
    }
    return tablenames;
  }

  @Override
  protected String insertSnippet(AnyDataModelField field) {
    StringBuilder bsql = new StringBuilder(100).append(field.getFieldName());

    if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_VERCHAR, field.getDataType())) { // 字符
      bsql.append(" VARCHAR");
      if (field.getDataLength() != null && field.getDataLength() > 0) {
        bsql.append("(").append(field.getDataLength()).append(")");
      } else {
        bsql.append("(255)");
      }
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_NUMBER, field.getDataType())) {
      doNumber(field, bsql);
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_DATE, field.getDataType())
        || StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_TIME, field.getDataType())) { // 日期，时间
      bsql.append(" TIMESTAMP");
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_CLOB, field.getDataType())) { // CLOB
      bsql.append(" CLOB");
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_BOOL, field.getDataType())) {
      bsql.append(" BOOL");
    }

    if (!field.getNullable()) {
      bsql.append(" NOT NULL");
    }

    return bsql.toString();
  }
  
  private void doNumber(AnyDataModelField field, StringBuilder bsql) {
    bsql.append(" NUMBER");
    if (field.getDataLength() != null && field.getDataLength() > 0) {
      bsql.append("(").append(field.getDataLength());
    } else {
      bsql.append("(30");
    }
    if (field.getDataScale() != null && field.getDataScale() > 0) {
      bsql.append(",").append(field.getDataScale());
    }
    bsql.append(")");
  }

  @Override
  protected String primaryKey(AnyDataModelField field) {
    Preconditions.checkNotNull(field);

    if (field.getIsPrimary() == null || !field.getIsPrimary()) {
      return null;
    }

    String sql = new StringBuilder(100).append("ALTER TABLE ").append(field.getDataModel().getRealTableName()).append(" ADD PRIMARY KEY (")
        .append(field.getFieldName()).append(")").toString();

    return sql;
  }

  @Override
  protected String foreignKey(AnyDataModelField field) {
    Preconditions.checkNotNull(field);
    if (field.getIsForeign() == null || !field.getIsForeign()) {
      return null;
    }
    String sql = null;
    if (this.isCreateForeignKey()) { // 创建外键
      String refTable = (StringUtils.isNotBlank(field.getRefTableName()) ? field.getRefTableName()
          : field.getRefDataModel().getRealTableName());
      sql = new StringBuilder(200).append("ALTER TABLE ").append(field.getDataModel().getRealTableName()).append(" ADD FOREIGN KEY (")
          .append(field.getFieldName()).append(") REFERENCES ").append(refTable).append("(ID)").toString();
    } else { // 创建索引
      String indexName = new StringBuilder(100).append("idx_").append(field.getDataModel().getRealTableName().toLowerCase()).append("_")
          .append(field.getFieldName().toLowerCase()).toString();
      sql = new StringBuilder(200).append("CREATE INDEX ").append(indexName).append(" ON ").append(field.getDataModel().getRealTableName())
          .append("(").append(field.getFieldName()).append(")").toString();
    }

    return sql;
  }

  @Override
  public boolean isColumnExists(String tableName, String columnName) {
    if (!isTableExists(tableName)) {
      return false;
    }
    Long count = getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=? AND COLUMN_NAME=?",
        Long.class, new Object[] { StringUtils.upperCase(tableName), StringUtils.upperCase(columnName) });

    return (count != null && count > 0L);
  }

}
