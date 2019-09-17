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
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;

import com.github.catstiger.anything.oper.service.ColumnDescriptor;
import com.github.catstiger.common.sql.limit.OracleLimitSQL;
import com.github.catstiger.common.util.Converters;
import com.google.common.base.Preconditions;

public class OracleAnySql extends AbstractAnySql {
  private static Logger logger = LoggerFactory.getLogger(OracleAnySql.class);

  @Override
  protected List<ColumnDescriptor> columns(String tableName) {
    Preconditions.checkNotNull(tableName);
    // 从字典表中获取字段信息
    String sql = "SELECT TABLE_NAME tableName, COLUMN_NAME columnName, DATA_PRECISION numericPrecision,"
        + "  DATA_SCALE numericScale,DATA_TYPE sqlType, NULLABLE isNullable "
        + "  FROM USER_TAB_COLUMNS WHERE TABLE_NAME=?";
    List<ColumnDescriptor> colDescs = jdbcTemplate.query(sql, new RowMapper<ColumnDescriptor>() {

      @Override
      public ColumnDescriptor mapRow(ResultSet rs, int index) throws SQLException {
        ColumnDescriptor colDesc = new ColumnDescriptor();
        colDesc.setTableName(rs.getString("tableName"));
        colDesc.setColumnName(rs.getString("columnName"));
        colDesc.setSqlType(rs.getString("sqlType"));
        colDesc.setIsNullable(StringUtils.equals("Y", rs.getString("isNullable")));
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
    if (StringUtils.equals(columnDescriptor.getSqlType(), "CLOB")) {
      type = Types.CLOB;
    } else if (StringUtils.indexOf(columnDescriptor.getSqlType(), "VARCHAR") >= 0) {
      type = Types.VARCHAR;
    } else if (isJdbcNumeric(columnDescriptor)) {
      type = Types.NUMERIC;
    } else if (isJdbcBigint(columnDescriptor)) {
      type = Types.BIGINT;
    } else if (isJdbcDouble(columnDescriptor)) {
      type = Types.DOUBLE;
    } else if (StringUtils.indexOf(columnDescriptor.getSqlType(), "DATE") >= 0) {
      type = Types.DATE;
    } else if (StringUtils.indexOf(columnDescriptor.getSqlType(), "TIMESTAMP") >= 0) {
      type = Types.TIMESTAMP;
    }

    return type;
  }
  
  private boolean isJdbcNumeric(ColumnDescriptor columnDescriptor) {
    return StringUtils.equals(columnDescriptor.getSqlType(), "NUMBER")
        && (columnDescriptor.getNumericPrecision() != 0 
        && columnDescriptor.getNumericPrecision() == 1 
        && columnDescriptor.getNumericScale() == 0);
  }
  
  private boolean isJdbcBigint(ColumnDescriptor columnDescriptor) {
    return StringUtils.equals(columnDescriptor.getSqlType(), "NUMBER")
        && (columnDescriptor.getNumericScale() == null || columnDescriptor.getNumericScale() == 0);
  }
  
  private boolean isJdbcDouble(ColumnDescriptor columnDescriptor) {
    return StringUtils.equals(columnDescriptor.getSqlType(), "NUMBER")
        && (columnDescriptor.getNumericScale() != null || columnDescriptor.getNumericScale() > 0);
  }

  @Override
  protected String limit(String sql, int start, int limit) {
    return new OracleLimitSQL().getLimitSql(sql, start, limit);
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
        e.printStackTrace();
        logger.warn(e.getMessage());
      }
    }
    return javaVal;
  }
  
  private Object convertValue(ColumnDescriptor columnDescriptor, Object value) {
    Object javaVal = null; // 实际的值
    if (columnDescriptor.isCurrentTime()) { // 操作时间
      javaVal = new Date();
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "CLOB")) {
      javaVal = new SqlLobValue(value.toString(), new DefaultLobHandler()); 
    } else if (StringUtils.indexOf(columnDescriptor.getSqlType(), "VARCHAR") >= 0) {
      javaVal = value.toString();
    } else if (isNumberBoolean(columnDescriptor, value)) {
      javaVal = (Boolean.TRUE.toString().equals(value) ? 1 : 0);
    } else if (isLong(columnDescriptor)) {
      javaVal = Long.valueOf(value.toString());
    } else if (isDouble(columnDescriptor)) {
      javaVal = Double.valueOf(value.toString());
    } else if (StringUtils.equals(columnDescriptor.getSqlType(), "BOOLEAN")) {
      javaVal = Boolean.valueOf(value.toString());
    } else if (isDate(columnDescriptor)) {
      javaVal = Converters.parseDate(value.toString());
    }
    return javaVal;
  }

  private boolean isDate(ColumnDescriptor columnDescriptor) {
    return StringUtils.indexOf(columnDescriptor.getSqlType(), "TIMESTAMP") >= 0
        || StringUtils.indexOf(columnDescriptor.getSqlType(), "DATE") >= 0;
  }

  private boolean isDouble(ColumnDescriptor columnDescriptor) {
    return StringUtils.equals(columnDescriptor.getSqlType(), "NUMBER")
        && (columnDescriptor.getNumericScale() != null || columnDescriptor.getNumericScale() > 0);
  }

  private boolean isLong(ColumnDescriptor columnDescriptor) {
    return StringUtils.equals(columnDescriptor.getSqlType(), "NUMBER")
        && (columnDescriptor.getNumericScale() == null || columnDescriptor.getNumericScale() == 0);
  }
  
  private boolean isNumberBoolean(ColumnDescriptor columnDescriptor, Object value) {
    if (Boolean.TRUE.toString().equals(value) || Boolean.FALSE.toString().equals(value)) {
      if (StringUtils.equals(columnDescriptor.getSqlType(), "NUMBER")
          && (columnDescriptor.getNumericPrecision() == 1 && columnDescriptor.getNumericScale() == 0)) {
        return true;
      }
    }
    return false;
  }

}
