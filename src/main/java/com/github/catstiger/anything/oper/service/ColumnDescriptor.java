package com.github.catstiger.anything.oper.service;

import org.apache.commons.lang3.StringUtils;

import com.github.catstiger.anything.AnyDataModelConstants;

/**
 * 用于描述SQL中的字段信息
 * 
 * @author lizhenshan
 * 
 */
public class ColumnDescriptor {
  /**
   * 所属表名
   */
  private String tableName;
  /**
   * 字段名
   */
  private String columnName;
  /**
   * SQL数据类型，即数据库中的数据类型
   */
  private String sqlType;
  /**
   * 字段精度
   */
  private Integer numericPrecision;
  /**
   * 数字类型字段的小数点位
   */
  private Integer numericScale;
  /**
   * 是否可以为null
   */
  private Boolean isNullable;

  public ColumnDescriptor() {

  }
  
  /**
   * 初始化列描述对象
   * @param tableName 表名
   * @param columnName 字段名
   * @param sqlType SQL类型
   * @param numericPrecision 数字精度
   * @param numericScale 小数点
   * @param isNullable 是否可为null
   */
  public ColumnDescriptor(String tableName, String columnName, String sqlType,
      Integer numericPrecision, Integer numericScale, Boolean isNullable) {
    this.tableName = tableName;
    this.columnName = columnName;
    this.sqlType = sqlType;
    this.numericPrecision = numericPrecision;
    this.numericScale = numericScale;
    this.isNullable = isNullable;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public String getSqlType() {
    return sqlType;
  }

  public void setSqlType(String sqlType) {
    this.sqlType = sqlType;
  }

  public Integer getNumericPrecision() {
    return numericPrecision;
  }

  public void setNumericPrecision(Integer numericPrecision) {
    this.numericPrecision = numericPrecision;
  }

  public Integer getNumericScale() {
    return numericScale;
  }

  public void setNumericScale(Integer numericScale) {
    this.numericScale = numericScale;
  }
  
  /**
   * 是否是一个自动获取的当前时间
   */
  public Boolean isCurrentTime() {
    return StringUtils.equalsIgnoreCase(AnyDataModelConstants.SYS_COL_UPDATE_TIME, columnName);
  }
  
  /**
   * 是否主键，columnName为ID即为主键
   */
  public Boolean isPrimary() {
    return StringUtils.equalsIgnoreCase(AnyDataModelConstants.SYS_COL_PRIMARY, columnName);
  }

  public Boolean getIsNullable() {
    return isNullable;
  }

  public void setIsNullable(Boolean isNullable) {
    this.isNullable = isNullable;
  }

  @Override
  public String toString() {
    return "ColumnDescriptor [tableName=" + tableName + ", columnName=" + columnName + ", sqlType="
        + sqlType + ", numericPrecision=" + numericPrecision + ", numericScale=" + numericScale
        + ", isNullable=" + isNullable + "]";
  }
}
