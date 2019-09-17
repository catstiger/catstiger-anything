package com.github.catstiger.anything.oper.service.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import com.github.catstiger.anything.oper.service.ColumnDescriptor;
import com.github.catstiger.common.sql.limit.MySqlLimitSQL;
import com.github.catstiger.common.web.converter.StringToDateConverter;
import com.google.common.base.Preconditions;

public class MySqlAnySql extends AbstractAnySql {
  private static Logger logger = LoggerFactory.getLogger(MySqlAnySql.class);

  private StringToDateConverter stringToDateConverter = new StringToDateConverter();

  @Override
  protected String limit(String sql, int start, int limit) {
    return new MySqlLimitSQL().getLimitSql(sql, start, limit);
  }

  @Override
  protected List<ColumnDescriptor> columns(String tableName) {
    Preconditions.checkNotNull(tableName);
    // 从字典表中获取字段信息
    String sql = "SELECT TABLE_NAME tableName, COLUMN_NAME columnName, NUMERIC_PRECISION numericPrecision,"
        + "  NUMERIC_SCALE numericScale,DATA_TYPE sqlType, IS_NULLABLE isNullable"
        + "  FROM information_schema.COLUMNS WHERE TABLE_NAME=?";
    List<ColumnDescriptor> colDescs = jdbcTemplate.query(sql, new RowMapper<ColumnDescriptor>() {

      @Override
      public ColumnDescriptor mapRow(ResultSet rs, int index) throws SQLException {
        ColumnDescriptor colDesc = new ColumnDescriptor();
        colDesc.setTableName(rs.getString("tableName"));
        colDesc.setColumnName(rs.getString("columnName"));
        colDesc.setSqlType(rs.getString("sqlType"));
        colDesc.setIsNullable(StringUtils.equals("YES", rs.getString("isNullable")));
        colDesc.setNumericPrecision(rs.getInt("numericPrecision"));
        colDesc.setNumericScale(rs.getInt("numericScale"));
        return colDesc;
      }

    }, tableName.toUpperCase());

    return colDescs;
  }

  @Override
  protected int getJdbcType(ColumnDescriptor columnDescriptor) {
    int type = Types.VARCHAR;
    if (StringUtils.equals(columnDescriptor.getSqlType(), "longtext")) {
      type = Types.VARCHAR;
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "varchar")) {
      type = Types.VARCHAR;
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "bigint")) {
      type = Types.BIGINT;
    } else if (isJdbcDouble(columnDescriptor)) {
      type = Types.DOUBLE;
    } else if (isJdbcInteger(columnDescriptor)) {
      type = Types.INTEGER;
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "tinyint")) {
      type = Types.BOOLEAN;
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "datetime")) {
      type = Types.TIMESTAMP;
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "timestamp")) {
      type = Types.TIMESTAMP;
    }

    return type;
  }
  
  private boolean isJdbcDouble(ColumnDescriptor columnDescriptor) {
    return StringUtils.equals(columnDescriptor.getSqlType(), "double") || StringUtils.equals(columnDescriptor.getSqlType(), "float");
  }
  
  private boolean isJdbcInteger(ColumnDescriptor columnDescriptor) {
    return StringUtils.equals(columnDescriptor.getSqlType(), "int") || StringUtils.equals(columnDescriptor.getSqlType(), "integer");
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
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "longtext")) {
      javaVal = value.toString();
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "varchar")) {
      javaVal = value.toString();
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "bigint")) {
      javaVal = Long.valueOf(value.toString());
    } else if (isInteger(columnDescriptor)) {
      javaVal = Integer.valueOf(value.toString());
    } else if (isDouble(columnDescriptor)) {
      javaVal = Double.valueOf(value.toString());
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "tinyint")) {
      javaVal = Boolean.valueOf(value.toString());
    } else if (isDate(columnDescriptor)) {
      javaVal =  stringToDateConverter.convert(value.toString());
      logger.debug("DATE TIME VALUE {} - {}", value.toString(), ((Date) javaVal));
    }
    return javaVal;
  }
  
  private boolean isInteger(ColumnDescriptor columnDescriptor) {
    return StringUtils.equals(columnDescriptor.getSqlType(), "int") || StringUtils.equals(columnDescriptor.getSqlType(), "integer");
  }

  private boolean isDouble(ColumnDescriptor columnDescriptor) {
    return StringUtils.equals(columnDescriptor.getSqlType(), "double") || StringUtils.equals(columnDescriptor.getSqlType(), "float");
  }
  
  private boolean isDate(ColumnDescriptor columnDescriptor) {
    return StringUtils.equals(columnDescriptor.getSqlType(), "timestamp") || StringUtils.equals(columnDescriptor.getSqlType(), "datetime");
  }
}
