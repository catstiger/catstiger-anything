package com.github.catstiger.anything.oper.service.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;

import com.github.catstiger.anything.oper.service.ColumnDescriptor;
import com.github.catstiger.common.sql.limit.H2LimitSQL;
import com.github.catstiger.common.util.Converters;
import com.google.common.base.Preconditions;

public class H2AnySql extends AbstractAnySql {
  private static Logger logger = LoggerFactory.getLogger(H2AnySql.class);

  @Override
  protected List<ColumnDescriptor> columns(String tableName) {
    Preconditions.checkNotNull(tableName);
    // 从字典表中获取字段信息
    String sql = "SELECT TABLE_NAME tableName, COLUMN_NAME columnName, NUMERIC_PRECISION numericPrecision,  "
        + "NUMERIC_SCALE numericScale,TYPE_NAME sqlType, NULLABLE isNullable FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=?";
    List<ColumnDescriptor> colDescs = jdbcTemplate.query(sql, new RowMapper<ColumnDescriptor>() {

      @Override
      public ColumnDescriptor mapRow(ResultSet rs, int index) throws SQLException {
        ColumnDescriptor colDesc = new ColumnDescriptor();
        colDesc.setTableName(rs.getString("tableName"));
        colDesc.setColumnName(rs.getString("columnName"));
        colDesc.setSqlType(rs.getString("sqlType"));
        colDesc.setIsNullable(rs.getInt("isNullable") == 1);
        colDesc.setNumericPrecision(rs.getInt("numericPrecision"));
        colDesc.setNumericScale(rs.getInt("numericScale"));
        return colDesc;
      }

    }, tableName.toUpperCase());

    return colDescs;
  }

  @Override
  protected int getJdbcType(ColumnDescriptor columnDescriptor) {
    Map<String, Integer> typeMap = new HashMap<>(12);
    typeMap.put("CLOB", Types.CLOB);
    typeMap.put("VARCHAR", Types.VARCHAR);
    typeMap.put("DOUBLE", Types.DOUBLE);
    typeMap.put("FLOAT", Types.FLOAT);
    typeMap.put("BIGINT", Types.BIGINT);
    typeMap.put("INTEGER", Types.INTEGER);
    typeMap.put("BOOLEAN", Types.BOOLEAN);
    typeMap.put("TIMESTAMP", Types.TIMESTAMP);
    
    int type = Types.VARCHAR;
    if (StringUtils.equals(columnDescriptor.getSqlType(), "DECIMAL")
        && (columnDescriptor.getNumericScale() == null || columnDescriptor.getNumericScale() == 0)) {
      type = Types.BIGINT;
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "DECIMAL")
        && (columnDescriptor.getNumericScale() != null || columnDescriptor.getNumericScale() > 0)) {
      type = Types.DOUBLE;
    } else if (typeMap.containsKey(columnDescriptor.getSqlType())) {
      type = typeMap.get(columnDescriptor.getSqlType());
    }

    return type;
  }

  @Override
  protected Object getJavaValue(ColumnDescriptor columnDescriptor, Map<String, Object> args) {
    Object javaVal = null; // 实际的值
    if (args.containsKey(columnDescriptor.getColumnName())) {
      Object value = args.get(columnDescriptor.getColumnName()); // 字符类型的值，通常由浏览器提交
      try {
        if (value != null) {
          javaVal = convertValue(columnDescriptor, value);
        }
      } catch (Exception e) {
        logger.warn(e.getMessage());
      }
    }

    return javaVal;
  }
  
  private Object convertValue(ColumnDescriptor columnDescriptor, Object value) {
    Object javaVal = null;
    if (columnDescriptor.isCurrentTime()) { // 操作时间
      javaVal = new Date();
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "CLOB")) {
      javaVal = new SqlLobValue(value.toString(), new DefaultLobHandler());
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "VARCHAR")) {
      javaVal = value.toString();
    } else if (isLong(columnDescriptor)) {
      javaVal = Long.valueOf(value.toString());
    } else if (isDouble(columnDescriptor)) {
      javaVal = Double.valueOf(value.toString());
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "INTEGER")) {
      javaVal = Integer.valueOf(value.toString());
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "BOOLEAN")) {
      javaVal = Boolean.valueOf(value.toString());
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "TIMESTAMP")
        || StringUtils.equals(columnDescriptor.getSqlType(), "DATETIME")) {
      javaVal = Converters.parseDate(value.toString());
    }
    return javaVal;
  }
  
  private boolean isLong(ColumnDescriptor columnDescriptor) {
    boolean isDecimalLong = StringUtils.equals(columnDescriptor.getSqlType(), "DECIMAL")
        && (columnDescriptor.getNumericScale() == null || columnDescriptor.getNumericScale() == 0);
    return isDecimalLong || StringUtils.equals(columnDescriptor.getSqlType(), "BIGINT");
  }
  
  private boolean isDouble(ColumnDescriptor columnDescriptor) {
    boolean isDecimalDouble = (StringUtils.equals(columnDescriptor.getSqlType(), "DECIMAL")
        && (columnDescriptor.getNumericScale() != null || columnDescriptor.getNumericScale() > 0));
    return isDecimalDouble
        || StringUtils.equals(columnDescriptor.getSqlType(), "DOUBLE")
        || StringUtils.equals(columnDescriptor.getSqlType(), "FLOAT");
  }

  @Override
  protected String limit(String sql, int start, int limit) {
    return new H2LimitSQL().getLimitSql(sql, start, limit);
  }
}
