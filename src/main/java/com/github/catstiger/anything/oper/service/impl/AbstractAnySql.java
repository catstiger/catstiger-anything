package com.github.catstiger.anything.oper.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.ddl.AnyDDL;
import com.github.catstiger.anything.oper.service.AnyIdService;
import com.github.catstiger.anything.oper.service.AnySql;
import com.github.catstiger.anything.oper.service.ColumnDescriptor;
import com.github.catstiger.common.sql.Page;
import com.github.catstiger.common.sql.filter.DynaSpec;
import com.github.catstiger.common.sql.filter.DynaSpecImpl;
import com.github.catstiger.common.sql.filter.QueryPart;
import com.github.catstiger.common.util.Exceptions;
import com.google.common.base.Preconditions;

public abstract class AbstractAnySql implements AnySql {
  private static Logger logger = LoggerFactory.getLogger(AbstractAnySql.class);

  protected AnyDDL anyDDL;

  protected JdbcTemplate jdbcTemplate;

  protected AnyIdService anyIdService;

  @Transactional
  @Override
  public Long insert(String tableName, Map<String, Object> args) {
    if (!anyDDL.isTableExists(tableName)) {
      throw Exceptions.unchecked("Table " + tableName + " is not exists.");
    }

    Preconditions.checkNotNull(args);

    final List<ColumnDescriptor> cols = columns(tableName);

    // VALUES前半段
    StringBuilder sqlInsert = new StringBuilder(200).append("INSERT INTO ").append(tableName).append("(");
    // VALUES后半段
    StringBuilder sqlValues = new StringBuilder(200).append(" VALUES ( ");

    for (Iterator<ColumnDescriptor> itr = cols.iterator(); itr.hasNext();) {
      ColumnDescriptor cd = itr.next();
      sqlInsert.append(cd.getColumnName());
      sqlValues.append("?");

      if (itr.hasNext()) {
        sqlInsert.append(", \n");
        sqlValues.append(",\n");
      } else {
        sqlInsert.append(") ");
        sqlValues.append(")");
      }
    }

    sqlInsert.append(sqlValues);

    logger.debug("SQL : {}", sqlInsert.toString());

    // 生成主键
    Long id = anyIdService.genId();
    args.put(AnyDataModelConstants.SYS_COL_PRIMARY, id);

    Object[] values = getJavaValues(cols, args, true);
    logger.debug("arguments : {}", Arrays.toString(values));
    int[] types = getJdbcTypes(cols);
    int count = jdbcTemplate.update(sqlInsert.toString(), values, types);

    logger.debug("新增记录{} - {}", count, id);

    return id;
  }

  @Override
  public List<Map<String, Object>> select(String tableName, Map<String, String[]> references, List<QueryPart> args, String sortName,
      String sortOrder, String querySnippets) {
    if (references == null) {
      references = Collections.emptyMap();
    }
    String sql = queryString(tableName, references, args, sortName, sortOrder, querySnippets);
    logger.debug("查询SQL: \n {}", sql.toString());

    Object[] params = new Object[] {};
    // 查询条件
    if (args != null && !args.isEmpty()) {
      DynaSpec dynaSpec = new DynaSpecImpl(args);
      params = dynaSpec.getQueryParams();
    }
    logger.debug("查询参数：{}", JSON.toJSONString(params));
    List<Map<String, Object>> list = jdbcTemplate.query(sql.toString(), new ColumnMapRowMapper(), params);

    return list;
  }

  @Override
  public Page pagedQuery(Page page, String tableName, Map<String, String[]> references, List<QueryPart> args, String sortName,
      String sortOrder, String querySnippets) {
    if (references == null) {
      references = Collections.emptyMap();
    }
    String sql = queryString(tableName, references, args, sortName, sortOrder, querySnippets);
    sql = limit(sql, page.getStart(), page.getLimit());

    logger.debug("分页查询SQL : \n {}", sql);

    Object[] params = new Object[] {};
    // 查询条件
    if (args != null && !args.isEmpty()) {
      DynaSpec dynaSpec = new DynaSpecImpl(args);
      params = dynaSpec.getQueryParams();
    }

    List<Map<String, Object>> list = jdbcTemplate.query(sql.toString(), new ColumnMapRowMapper(), params);
    Long c = count(tableName, args);

    page.setRows(list);
    page.setTotal(c);

    return page;
  }

  @Override
  public Map<String, Object> load(String tableName, Long id, String... cols) {
    if (!anyDDL.isTableExists(tableName)) {
      logger.warn("Table {} does not exists!", tableName);
      return null;
    }

    List<ColumnDescriptor> colDescs = columns(tableName);
    StringBuilder sql = new StringBuilder(500).append("SELECT ");

    for (Iterator<ColumnDescriptor> itr = colDescs.iterator(); itr.hasNext();) {
      ColumnDescriptor colDesc = itr.next();
      if (ArrayUtils.isEmpty(cols)) {
        sql.append("\n").append(colDesc.getColumnName().toUpperCase());
        if (itr.hasNext()) {
          sql.append(",");
        }
      } else {
        if (ArrayUtils.contains(cols, colDesc.getColumnName().toUpperCase())) {
          sql.append("\n").append(colDesc.getColumnName().toUpperCase());
          if (itr.hasNext()) {
            sql.append(",");
          }
        }
      }
    }
    // 删除末尾的,
    if (sql.toString().endsWith(",")) {
      sql.deleteCharAt(sql.lastIndexOf(","));
    }

    sql.append(" \n FROM ").append(tableName).append(" WHERE ID=?");

    List<Map<String, Object>> rows = jdbcTemplate.query(sql.toString(), new ColumnMapRowMapper(), id);

    Map<String, Object> row = Collections.emptyMap();
    if (rows != null && !rows.isEmpty()) {
      row = rows.get(0);
    }

    return row;
  }

  @Override
  public void update(String tableName, Long id, Map<String, Object> args) {
    if (!anyDDL.isTableExists(tableName)) {
      logger.warn("Table {} does not exists!", tableName);
      return;
    }
    List<ColumnDescriptor> colDescs = columns(tableName);
    List<Object> updateObjects = new ArrayList<Object>(args.size() + 1);
    List<Integer> sqlTypes = new ArrayList<Integer>();
    ColumnDescriptor idColDesc = null;
    StringBuilder sql = new StringBuilder(500).append("UPDATE ").append(tableName).append(" SET \n");

    for (Iterator<ColumnDescriptor> itr = colDescs.iterator(); itr.hasNext();) {
      ColumnDescriptor colDesc = itr.next();
      // Update 语句不包括主键
      if (colDesc.isPrimary() && StringUtils.equalsIgnoreCase(colDesc.getColumnName(), "id")) {
        idColDesc = colDesc;
        continue;
      }
      // 只包含参数中的字段，如果表字段没在参数中，则忽略
      if (!args.containsKey(colDesc.getColumnName())) {
        continue;
      }

      sql.append("\n").append(colDesc.getColumnName()).append("=?");
      if (itr.hasNext()) {
        sql.append(",");
      }

      updateObjects.add(getJavaValue(colDesc, args));
      sqlTypes.add(getJdbcType(colDesc));
    }

    // 删除末尾的,
    if (sql.toString().endsWith(",")) {
      sql.deleteCharAt(sql.lastIndexOf(","));
    }

    sql.append(" WHERE ID=?");
    logger.debug("更新SQL : \n {}", sql);
    // 末尾加入主键
    updateObjects.add(id);
    sqlTypes.add(getJdbcType(idColDesc));

    int[] types = new int[sqlTypes.size()];
    for (int i = 0; i < sqlTypes.size(); i++) {
      types[i] = sqlTypes.get(i);
    }

    jdbcTemplate.update(sql.toString(), updateObjects.toArray(), types);
  }

  @Override
  public void delete(String tableName, Long id) {
    if (!anyDDL.isTableExists(tableName)) {
      logger.warn("Table {} does not exists!", tableName);
      return;
    }

    if (id == null) {
      logger.warn("ID must not be null!", tableName);
      return;
    }

    StringBuilder sql = new StringBuilder(100).append("DELETE FROM ");
    sql.append(tableName).append(" WHERE ID=?");

    jdbcTemplate.update(sql.toString(), id);
  }

  private String queryString(String tableName, Map<String, String[]> references, List<QueryPart> args, String sortName, String sortOrder,
      String querySnippets) {
    if (!anyDDL.isTableExists(tableName)) {
      logger.warn("Table {} does not exists!", tableName);
      return null;
    }
    
    List<ColumnDescriptor> colDescs = columns(tableName);
    String select = buildSelect(tableName, references, colDescs); //构建SELECT ... FROM TABLE
    StringBuilder sql = new StringBuilder(select);
    sql.append(" WHERE 1=1 ");
    // 查询条件
    if (args != null && !args.isEmpty()) {
      DynaSpec dynaSpec = new DynaSpecImpl(args);
      sql.append(dynaSpec.buildQueryString(AnyDataModelConstants.DEFAULT_TABLE_ALIAS));
    }
    // 附加的查询条件
    if (StringUtils.isNotBlank(querySnippets)) {
      if (querySnippets.toLowerCase().trim().startsWith("and") || querySnippets.toLowerCase().trim().startsWith("or")) {
        sql.append(" ").append(querySnippets).append(" ");
      } else {
        sql.append(" AND ").append(querySnippets).append(" ");
      }
    }

    // 排序
    if (sortName != null && anyDDL.isColumnExists(tableName, sortName)) {
      sql.append(" ORDER BY " + AnyDataModelConstants.DEFAULT_TABLE_ALIAS_DOT).append(sortName);
      if (sortOrder != null) {
        sql.append(" ").append(sortOrder);
      }
    }

    return sql.toString();
  }
  
  /**
   * 构建Select部分，包括关联查询的Join语句
   */
  private String buildSelect(String tableName, Map<String, String[]> references, List<ColumnDescriptor> colDescs) {
    StringBuilder sql = new StringBuilder(500).append("SELECT ");

    for (Iterator<ColumnDescriptor> itr = colDescs.iterator(); itr.hasNext();) {
      ColumnDescriptor colDesc = itr.next();
      String col = colDesc.getColumnName().toUpperCase();
      if (!references.containsKey(col)) { // 如果不是外键
        sql.append(AnyDataModelConstants.DEFAULT_TABLE_ALIAS_DOT).append(col).append(" AS ").append(col);
      } else { // 如果是外键
        String[] tableAndCol = references.get(col);
        if (tableAndCol != null && tableAndCol.length >= 2) {
          sql.append(col.toLowerCase()).append("_"); // 外键表的别名（使用主表中的字段名称前面加下划线）
          sql.append(".").append(tableAndCol[1]).append(" AS ").append(col); // 使用外键显示字段替代主表字段
        } else {
          continue;
        }
      }
      if (itr.hasNext()) {
        sql.append(",");
      }
    }

    // 删除末尾的,
    if (sql.toString().endsWith(",")) {
      sql.deleteCharAt(sql.lastIndexOf(","));
    }

    sql.append(" \n FROM \n ");
    sql.append(tableName).append(AnyDataModelConstants.DEFAULT_TABLE_ALIAS_BLANK + " \n"); // 主表及其别名
    
    // 关联查询
    for (String key : references.keySet()) {
      String[] tableAndCol = references.get(key);
      if (tableAndCol != null && tableAndCol.length >= 2) {
        String alias = key.toLowerCase() + "_"; // 别名

        sql.append(" LEFT JOIN ").append(tableAndCol[0]).append(" ").append(alias).append(" ON (")
            .append(AnyDataModelConstants.DEFAULT_TABLE_ALIAS_DOT).append(key).append("=").append(alias).append(".ID)");

        sql.append("\n");
      }
    }
    return sql.toString();
  }

  /**
   * 执行Count查询
   * 
   * @param tableName 表名
   * @param args 查询参数
   */
  protected Long count(String tableName, List<QueryPart> args) {
    StringBuilder sql = new StringBuilder(100);
    sql.append("SELECT COUNT(*) FROM ").append(tableName.toUpperCase()).append(AnyDataModelConstants.DEFAULT_TABLE_ALIAS_BLANK);
    // 查询条件
    Object[] params = new Object[] {};
    if (args != null && !args.isEmpty()) {
      sql.append(" WHERE 1=1 ");
      DynaSpec dynaSpec = new DynaSpecImpl(args);
      sql.append(dynaSpec.buildQueryString(AnyDataModelConstants.DEFAULT_TABLE_ALIAS_BLANK));

      params = dynaSpec.getQueryParams();
    }

    Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);

    return count;
  }

  /**
   * 从某个表的字段描述信息中按照顺序提取JDBC数据类型
   * 
   * @param columnDescriptors 一组字段描述信息
   * @see java.sql.Types
   */
  protected int[] getJdbcTypes(List<ColumnDescriptor> columnDescriptors) {
    int[] types = new int[columnDescriptors.size()];
    int index = 0;

    for (ColumnDescriptor cd : columnDescriptors) {
      types[index] = getJdbcType(cd);
      index++;
    }

    return types;
  }

  /**
   * 从单个ColumnDescriptor对象中获取对应的JDBC 数据类型
   * 
   * @param columnDescriptor 给出ColumnDescriptor的实例
   * @see java.sql.Types
   */
  protected abstract int getJdbcType(ColumnDescriptor columnDescriptor);

  /**
   * 将一个普通的SQL语句转换为带有limit查询的sql语句
   * 
   * @param sql SQL
   * @param start 起始位置
   * @param limit extracted rows
   */
  protected abstract String limit(String sql, int start, int limit);

  /**
   * 返回给定表的字段名称和字段描述对象对应关系
   * 
   * @param tableName 给出表名
   */
  protected abstract List<ColumnDescriptor> columns(String tableName);

  /**
   * 根据数据库对应某个表的字段描述信息，从<code>args</code>参数中按照<code>columnDescriptors</code>的顺序
   * 提取数据，并将提取出的数据按照相应的<code>ColumnDescriptor</code>所提供的类型进行数据转换。如果<code>args</code>
   * 没有包含<code>columnDescriptors</code>中的某个字段，则该字段对应的数据为<code>null</code>
   * 
   * @param columnDescriptors 某个表的字段描述对象
   * @param args 一组Key-Value，Key为字段名称，Value为对应的字符串类型的数据。args通常从客户端提交的数据中获得
   * @param incPK 是否包括主键，如果为<code>true</code>则不转换主键字段
   */
  protected Object[] getJavaValues(List<ColumnDescriptor> columnDescriptors, Map<String, Object> args, boolean incPK) {
    List<Object> values = new ArrayList<Object>(columnDescriptors.size());
    logger.debug("Original params {}", args);
    for (ColumnDescriptor cd : columnDescriptors) {
      if (!incPK && cd.isPrimary()) { // 如果不必处理主键
        continue;
      }
      Object javaVal = getJavaValue(cd, args);
      values.add(javaVal);
      logger.debug("Original key = {}; value={}", cd.getColumnName(), javaVal);
    }
    return values.toArray();
  }

  /**
   * 根据单个<code>ColumnDescriptor</code>对，从<code>args</code>参数中提取数据，并进行数据类型转换， 如果<code>args</code>没有对应的数据，则返回<code>null</code>
   * 
   * @param columnDescriptor 某个表的字段描述对象
   * @param args 一组Key-Value，Key为字段名称，Value为对应的字符串类型的数据。args通常从客户端提交的数据中获得
   */
  protected abstract Object getJavaValue(ColumnDescriptor columnDescriptor, Map<String, Object> args);

  public void setAnyDDL(AnyDDL anyDDL) {
    this.anyDDL = anyDDL;
  }

  public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void setAnyIdService(AnyIdService anyIdService) {
    this.anyIdService = anyIdService;
  }

}
