package com.github.catstiger.anything.ddl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.google.common.base.Preconditions;

public class OracleAnyDDL extends AbstractAnyDDL {

  @Override
  public boolean isTableExists(String tableName) {
    Preconditions.checkNotNull(tableName);

    Long count = getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME=?", Long.class,
        StringUtils.upperCase(tableName));

    return (count != null && count > 0L);
  }

  @Override
  public boolean isColumnExists(String tableName, String columnName) {
    if (!isTableExists(tableName)) {
      return false;
    }
    Long count = getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM USER_TAB_COLS WHERE TABLE_NAME=? AND COLUMN_NAME=?", Long.class,
        new Object[] { StringUtils.upperCase(tableName), StringUtils.upperCase(columnName) });

    return (count != null && count > 0L);
  }

  @Override
  public List<String> tableNameLike(String tableName) {
    Preconditions.checkNotNull(tableName);

    List<Map<String, Object>> tables = getJdbcTemplate().queryForList("SELECT TABLE_NAME USER_TABLES WHERE TABLE_NAME LIKE ?",
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
      doVarchar(field, bsql);
    }
    if (field.getIsForeign() || field.getIsPrimary()) {
      bsql.append(" NUMBER(19, 0)");
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_NUMBER, field.getDataType())) { // 数字
      doNumber(field, bsql);
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_TIME, field.getDataType())) { // 时间
      bsql.append(" TIMESTAMP");
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_DATE, field.getDataType())) { // 日期
      bsql.append(" DATE");
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_CLOB, field.getDataType())) { // CLUB
      bsql.append(" CLOB");
    } else if (StringUtils.equalsIgnoreCase(AnyDataModelConstants.DATA_TYPE_BOOL, field.getDataType())) {
      bsql.append(" NUMBER(1,0)");
    }

    if (!field.getNullable()) {
      bsql.append(" not null");
    }

    return bsql.toString();
  }

  private void doVarchar(AnyDataModelField field, StringBuilder bsql) {
    bsql.append(" VARCHAR2");
    if (field.getDataLength() != null && field.getDataLength() > 0) {
      bsql.append("(").append(field.getDataLength()).append(" CHAR)");
    } else {
      bsql.append("(255 CHAR)");
    }
  }

  private void doNumber(AnyDataModelField field, StringBuilder bsql) {
    bsql.append(" NUMBER");
    if (field.getDataLength() != null && field.getDataLength() > 0) {
      bsql.append("(").append(field.getDataLength());
    } else {
      bsql.append("(32");
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

  /**
   * Oracle的Add Column语法与其他不同，没有Column
   */
  @Override
  public void addColumn(AnyDataModelField field) {
    Preconditions.checkNotNull(field);

    if (AnyDataModelConstants.XTYPE_ONETOMANY.equals(field.getInputType())
        || AnyDataModelConstants.XTYPE_MULTIFILEUPLOAD.equals(field.getInputType())) {
      return;
    }

    field = getJdbcTemplate().get(AnyDataModelField.class, field.getId());
    AnyDataModel dataModel = field.getDataModel();
    // 添加字段
    StringBuilder sql = new StringBuilder(100).append("ALTER TABLE ").append(dataModel.getRealTableName()).append(" ADD (")
        .append(insertSnippet(field)).append(")");

    getJdbcTemplate().execute(sql.toString());
    // 如果是外键
    if (field.getIsForeign() && field.getRefDataModel() != null) {
      getJdbcTemplate().execute(foreignKey(field));
    }
  }

}
