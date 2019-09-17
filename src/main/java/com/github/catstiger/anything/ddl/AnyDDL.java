package com.github.catstiger.anything.ddl;

import java.util.List;

import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;

/**
 * 用于执行数据库DDL语句
 * 
 * @author lizhenshan
 * 
 */
public interface AnyDDL {
  /**
   * 表是否存在
   */
  boolean isTableExists(AnyDataModel dataModel);
 
  /**
   * 表是否存在
   */
  boolean isTableExists(String tableName);
  
  /**
   * 指定的字段是否存在
   * @param tableName 表名
   * @param columnName 字段名
   */
  boolean isColumnExists(String tableName, String columnName);

  /**
   * 根据给定的dataModel创建数据表
   */
  void createTable(AnyDataModel dataModel);

  /**
   * 删除AnyDataModel对应的数据表
   */
  void dropTable(AnyDataModel dataModel);
  
  /**
   * 删除数据表
   */
  void dropTable(String tableName);

  /**
   * 添加一个字段
   */
  void addColumn(AnyDataModelField field);

  /**
   * 删除一个字段
   */
  void dropColumn(AnyDataModelField field);
  
  /**
   * 查询符合条件的表名
   */
  List<String> tableNameLike(String tableName);

}
