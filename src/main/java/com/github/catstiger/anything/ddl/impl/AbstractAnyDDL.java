package com.github.catstiger.anything.ddl.impl;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.ddl.AnyDDL;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.BeanPropertyRowMapperEx;
import com.github.catstiger.common.util.Exceptions;
import com.google.common.base.Preconditions;

public abstract class AbstractAnyDDL implements AnyDDL {
  private static Logger logger = LoggerFactory.getLogger(AbstractAnyDDL.class);

  /**
   * 是否创建createForeignKey，如果为{@code true}则根据不同的数据库语法，创建外键。否则，只是创建一个索引。
   */
  private boolean createForeignKey = false;

  /**
   * 用于执行DDL和SQL
   */
  private JdbcTemplateProxy jdbcTemplate;

  /**
   * 根据Field，构建一个Insert语句中的片段，例如 USER_NAME VARCHAR(255) NOT NULL
   * 
   * @param field 给定的AnyDataModelField
   * @return String of SQL snippet.
   */
  protected abstract String insertSnippet(AnyDataModelField field);

  /**
   * 根据Field，构建一个创建主键的SQL，例如ALTER TABLE TABLE_NAME ADD PRIMARY KEY (SSN)
   * 
   * @param field 给定的AnyDataModelField
   * @return String of SQL snippet.
   */
  protected abstract String primaryKey(AnyDataModelField field);

  /**
   * 根据Field对象，构建一个创建外键的SQL，例如ALTER TABLE TABLE_NAME ADD FOREIGN KEY (OTHER_TABLE_ID) REFERENCES OTHER_TABLE_ID(ID)
   * 
   * @param field AnyDataModelField 用于描述字段特征
   * @return DDL SQL
   */
  protected abstract String foreignKey(AnyDataModelField field);

  @Override
  public final boolean isTableExists(AnyDataModel dataModel) {
    Preconditions.checkNotNull(dataModel);
    Preconditions.checkNotNull(dataModel.getRealTableName());

    return isTableExists(dataModel.getRealTableName());
  }

  @Override
  public void createTable(AnyDataModel dataModel) {
    Preconditions.checkNotNull(dataModel);
    Preconditions.checkNotNull(dataModel.getId());
    // 重新加载AnyDataModel
    StringBuilder sql = new StringBuilder(1000).append("CREATE TABLE ").append(dataModel.getRealTableName()).append("(\n");

    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append(" WHERE data_model_id=?", dataModel.getId())
        .append(" AND input_type<>?", AnyDataModelConstants.XTYPE_MULTIFILEUPLOAD)
        .append(" AND input_type<>? ", AnyDataModelConstants.XTYPE_ONETOMANY).orderBy(" orders ", "asc");

    List<AnyDataModelField> fields = jdbcTemplate.query(sqlReady.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sqlReady.getArgs());
    // 创建表
    for (Iterator<AnyDataModelField> itr = fields.iterator(); itr.hasNext();) {
      AnyDataModelField field = itr.next();
      sql.append(insertSnippet(field)); // SQL 片段
      if (itr.hasNext()) {
        sql.append(",\n");
      }
    }

    sql.append(")");
    logger.debug("建表语句 : {}", sql);
    jdbcTemplate.execute(sql.toString()); // 建表

    // 创建主键
    for (Iterator<AnyDataModelField> itr = fields.iterator(); itr.hasNext();) {
      AnyDataModelField field = itr.next();
      if (field.getIsPrimary() && field.getIsSys()) {
        field.setDataModel(dataModel);
        jdbcTemplate.execute(primaryKey(field)); // 主键
      }
    }
    // 创建外键
    for (Iterator<AnyDataModelField> itr = fields.iterator(); itr.hasNext();) {
      AnyDataModelField field = itr.next();
      if (field.getIsForeign() && field.getRefDataModel() != null) {
        field.setDataModel(dataModel);
        if (!isTableExists(field.getRefDataModel())) { // 如果外键指向的表不存在，则创建
          createTable(field.getRefDataModel());
        }
        jdbcTemplate.execute(foreignKey(field)); // 外键
      }
    }

    // 标记AnyDataModel实例
    jdbcTemplate.update("update any_data_model set has_table=? where id=?", true, dataModel.getId());
  }

  @Override
  public void dropTable(AnyDataModel dataModel) {
    Preconditions.checkNotNull(dataModel);
    Preconditions.checkNotNull(dataModel.getRealTableName());

    StringBuilder sql = new StringBuilder(100).append("DROP TABLE ").append(dataModel.getRealTableName()).append(" CASCADE");
    try {
      jdbcTemplate.execute(sql.toString());
    } catch (Exception e) {
      e.printStackTrace();
      throw Exceptions.unchecked("删除数据表失败！");
    }

    // 标记AnyDataModel实例
    jdbcTemplate.update("update any_data_model set has_table=? where id=?", false, dataModel.getId());
  }

  @Override
  public void dropTable(String tableName) {
    StringBuilder sql = new StringBuilder(100).append("DROP TABLE ").append(tableName).append(" CASCADE");
    jdbcTemplate.execute(sql.toString());
  }

  @Override
  public void addColumn(AnyDataModelField field) {
    Preconditions.checkNotNull(field);
    Preconditions.checkNotNull(field.getId());

    if (AnyDataModelConstants.XTYPE_ONETOMANY.equals(field.getInputType())
        || AnyDataModelConstants.XTYPE_MULTIFILEUPLOAD.equals(field.getInputType())) {
      return;
    }

    field = jdbcTemplate.get(AnyDataModelField.class, field.getId());
    if (field.getDataModel() != null && field.getDataModel().getId() != null) {
      field.setDataModel(jdbcTemplate.get(AnyDataModel.class, field.getDataModel().getId()));
    }
    AnyDataModel dataModel = field.getDataModel();
    // 添加字段
    StringBuilder sql = new StringBuilder(100).append("ALTER TABLE ").append(dataModel.getRealTableName()).append(" ADD COLUMN (")
        .append(insertSnippet(field)).append(")");

    logger.debug("添加字段 {}", sql);

    jdbcTemplate.execute(sql.toString());
    // 如果是外键
    if (field.getIsForeign() && field.getRefDataModel() != null) {
      field.setRefDataModel(jdbcTemplate.get(AnyDataModel.class, field.getRefDataModel().getId()));
      jdbcTemplate.execute(foreignKey(field));
    }
  }

  @Override
  public void dropColumn(AnyDataModelField field) {
    Preconditions.checkNotNull(field);
    Preconditions.checkNotNull(field.getId());

    field = jdbcTemplate.get(AnyDataModelField.class, field.getId());

    Preconditions.checkNotNull(field.getDataModel());
    Preconditions.checkNotNull(field.getDataModel().getId());
    AnyDataModel dataModel = jdbcTemplate.get(AnyDataModel.class, field.getDataModel().getId());

    // 删除字段
    StringBuilder sql = new StringBuilder(100).append("ALTER TABLE ").append(dataModel.getRealTableName()).append(" DROP COLUMN ")
        .append(field.getFieldName());

    logger.debug("删除字段 {}", sql);
    try {
      jdbcTemplate.execute(sql.toString());
    } catch (Exception e) {
      logger.warn(e.getMessage());
    }

  }

  public JdbcTemplateProxy getJdbcTemplate() {
    return jdbcTemplate;
  }

  public void setJdbcTemplate(JdbcTemplateProxy jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }
  
  public boolean isCreateForeignKey() {
    return createForeignKey;
  }

  public void setCreateForeignKey(boolean createForeignKey) {
    this.createForeignKey = createForeignKey;
  }

}
