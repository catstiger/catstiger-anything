package com.github.catstiger.anything.ddl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.google.common.base.Preconditions;

public class MySqlAnyDDL extends AbstractAnyDDL {

  @Override
  public boolean isTableExists(String tableName) {
    Preconditions.checkNotNull(tableName);

    Long count = getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME=?", Long.class,
        StringUtils.upperCase(tableName));

    return (count != null && count > 0L);
  }

  @Override
  public boolean isColumnExists(String tableName, String columnName) {
    if (!isTableExists(tableName)) {
      return false;
    }
    Long count = getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_NAME=? AND COLUMN_NAME=?",
        Long.class, new Object[] { StringUtils.upperCase(tableName), StringUtils.upperCase(columnName) });

    return (count != null && count > 0L);
  }

  @Override
  public List<String> tableNameLike(String tableName) {
    Preconditions.checkNotNull(tableName);

    List<Map<String, Object>> tables = getJdbcTemplate().queryForList("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_NAME LIKE ?",
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
      doVerchar(field, bsql);
    }
    if (field.getIsForeign() || field.getIsPrimary()) {
      bsql.append(" bigint");
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_NUMBER, field.getDataType())) { // 数字
      bsql.append(" double");
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_DATE, field.getDataType())
        || StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_TIME, field.getDataType())) { // 日期，时间
      bsql.append(" datetime");
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_CLOB, field.getDataType())) { // CLOB
      bsql.append(" longtext");
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_BOOL, field.getDataType())) {
      bsql.append(" bool");
    }

    if (!field.getNullable()) {
      bsql.append(" not null");
    }

    return bsql.toString();
  }

  private void doVerchar(AnyDataModelField field, StringBuilder bsql) {
    bsql.append(" varchar");
    if (field.getDataLength() != null && field.getDataLength() > 0) {
      bsql.append("(").append(field.getDataLength()).append(")");
    } else {
      bsql.append("(255)");
    }
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
      sql = new StringBuilder(200)
          .append("ALTER TABLE ")
          .append(field.getDataModel().getRealTableName())
          .append(" ADD FOREIGN KEY (")
          .append(field.getFieldName())
          .append(") REFERENCES ").append(refTable)
          .append("(ID)").toString();
    } else { // 创建索引
      String indexName = new StringBuilder(100)
          .append("idx_")
          .append(field.getDataModel().getRealTableName().toLowerCase())
          .append("_")
          .append(field.getFieldName().toLowerCase()).toString();
      sql = new StringBuilder(200)
          .append("CREATE INDEX ")
          .append(indexName)
          .append(" ON ").append(field.getDataModel().getRealTableName())
          .append("(")
          .append(field.getFieldName())
          .append(")").toString();
    }

    return sql;
  }

}
