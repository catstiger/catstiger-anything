package com.github.catstiger.anything.oper.service.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.ddl.AnyDDL;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.anything.oper.service.AnyComboService;
import com.github.catstiger.common.model.KeyValue;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.BeanPropertyRowMapperEx;
import com.google.common.base.Preconditions;

@Service
public class AnyComboServiceImpl implements AnyComboService {
  private static Logger logger = LoggerFactory.getLogger(AnyComboServiceImpl.class);

  @Autowired
  private JdbcTemplateProxy jdbcTemplate;
  @Autowired
  private AnyDDL anyDDL;

  @Override
  public List<KeyValue<Long, Object>> list(String tableName, String displayField, String value) {
    if (!anyDDL.isColumnExists(tableName, displayField)) {
      return Collections.emptyList();
    }
    StringBuilder sql = new StringBuilder(100).append("SELECT ID \"KEY\", ").append(displayField).append(" \"VALUE\" FROM ")
        .append(tableName);

    List<Object> args = new ArrayList<Object>(1);
    if (StringUtils.isNotBlank(value)) {
      sql.append(" WHERE ").append(displayField).append(" LIKE ? ");
      args.add("%" + value + "%");
    }

    if (anyDDL.isColumnExists(tableName, " UPDATE_TIME")) {
      sql.append(" ORDER BY UPDATE_TIME DESC");
    }

    List<KeyValue<Long, Object>> list = jdbcTemplate.query(sql.toString(), new RowMapper<KeyValue<Long, Object>>() {
      @Override
      public KeyValue<Long, Object> mapRow(ResultSet rs, int idx) throws SQLException {
        KeyValue<Long, Object> keyVal = new KeyValue<Long, Object>();
        keyVal.setKey(rs.getLong("KEY"));
        keyVal.setValue(rs.getObject("VALUE"));

        return keyVal;
      }
    }, args.toArray());
    return list;
  }

  @Override
  public List<KeyValue<Long, Object>> list(AnyDataModelField field, String value) {
    Preconditions.checkNotNull(field);
    Preconditions.checkNotNull(field.getId());

    field = jdbcTemplate.get(AnyDataModelField.class, field.getId());

    if (!field.getIsForeign() || field.getRefDataModel() == null || field.getRefDataModel().getId() == null) {
      return Collections.emptyList();
    }

    String refTableName = jdbcTemplate.queryForObject("select table_name from any_data_model where id=?", String.class,
        field.getRefDataModel().getId());
    refTableName = AnyDataModelConstants.getReadTablename(refTableName); // 表名加前缀
    String refDispFieldName = jdbcTemplate.queryForObject("select field_name from any_data_model_field where id=?", String.class,
        field.getRefFieldDisplay().getId());

    return list(refTableName, refDispFieldName, value);
  }

  @Override
  public List<Map<String, Object>> tree(String tableName, String parentField, String displayField) {
    if (!anyDDL.isColumnExists(tableName, displayField) || !anyDDL.isColumnExists(tableName, parentField)) {
      return Collections.emptyList();
    }
    return tree(tableName, parentField, displayField, null);
  }
  

  /**
   * 根据数据库中自关联的键，构建一个tree结构的List
   * @param tableName 表名
   * @param parentField 指向自身的外键名称
   * @param displayField 显示的字段名称
   * @param parentId 上级ID，如果为{@code null}，则表示顶层
   * @return List of Map， "children" 指向下级列表
   */
  private List<Map<String, Object>> tree(String tableName, String parentField, String displayField, Object parentId) {
    List<Map<String, Object>> treeList = new ArrayList<Map<String, Object>>();
    List<Object> args = new ArrayList<Object>();

    StringBuffer sql = new StringBuffer(100);
    sql.append("SELECT ID ID,").append(displayField).append(" TEXT FROM ").append(tableName);
    if (parentId == null) {
      sql.append(" WHERE ").append(parentField).append(" IS NULL ");
    } else {
      sql.append(" WHERE ").append(parentField).append(" =?");
      args.add(parentId);
    }

    if (anyDDL.isColumnExists(tableName, "UPDATE_TIME")) {
      sql.append(" ORDER BY UPDATE_TIME DESC");
    }

    logger.debug("TREE SQL {}", sql.toString());

    List<Map<String, Object>> list = jdbcTemplate.query(sql.toString(), args.toArray(), new ColumnMapRowMapper());
    for (Map<String, Object> map : list) {
      Map<String, Object> node = new HashMap<String, Object>();
      node.put("id", map.get(AnyDataModelConstants.SYS_COL_PRIMARY));
      node.put("text", map.get("TEXT"));

      List<Map<String, Object>> children = tree(tableName, parentField, displayField, map.get(AnyDataModelConstants.SYS_COL_PRIMARY));
      if (children.isEmpty()) {
        node.put("leaf", true);
        node.put("iconCls", "icon-empty");
      } else {
        node.put("children", children);
        node.put("expanded", true);
      }
      treeList.add(node);
    }
    return treeList;
  }
  
  @Override
  public List<Map<String, Object>> tree(AnyDataModel dataModel) {
    Preconditions.checkNotNull(dataModel);
    Preconditions.checkNotNull(dataModel.getId());
    dataModel = jdbcTemplate.get(AnyDataModel.class, dataModel.getId());
    // 必须是一个树形结构的AnyDataModel
    if (!StringUtils.equalsIgnoreCase(dataModel.getUiStyle(), AnyDataModelConstants.UI_STYLE_TREEGRID_FORM)) {
      logger.warn("Tree DataModel must has a uiStyle width {}, but the value is {}", AnyDataModelConstants.UI_STYLE_TREEGRID_FORM,
          dataModel.getUiStyle());
      return Collections.emptyList();
    }

    // 找到指向自身并且用于TreeCol的那个字段
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append(" WHERE data_model_id=? and as_tree_col=true",
        dataModel.getId());
    AnyDataModelField treeCol = (AnyDataModelField) jdbcTemplate.queryForObject(sqlReady.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sqlReady.getArgs());

    if (treeCol == null) {
      logger.warn("There is no foreign key that references itself.");
      return Collections.emptyList();
    }
    // 引用表中用于显示的字段名称
    String refDispFieldName = jdbcTemplate.queryForObject("select field_name from any_data_model_field where id=?", String.class,
        treeCol.getRefFieldDisplay().getId());
    // 表名(指向自身，因此引用表名=表名)
    String realTable = jdbcTemplate.queryForObject("select table_name from any_data_model where id=?", String.class, dataModel.getId());
    realTable = AnyDataModelConstants.getReadTablename(realTable); // 表名加前缀
    return tree(realTable, treeCol.getFieldName(), refDispFieldName);
  }

  @Override
  public List<Map<String, Object>> plainTree(String tableName, String displayField) {
    if (!anyDDL.isColumnExists(tableName, displayField)) {
      return Collections.emptyList();
    }

    StringBuffer sql = new StringBuffer(100);
    sql.append("SELECT ID ID,").append(displayField).append(" TEXT FROM ").append(tableName);

    if (anyDDL.isColumnExists(tableName, "UPDATE_TIME")) {
      sql.append(" ORDER BY UPDATE_TIME DESC");
    }

    List<Map<String, Object>> list = jdbcTemplate.query(sql.toString(), new ColumnMapRowMapper());
    List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();

    for (Map<String, Object> map : list) {
      Map<String, Object> node = new HashMap<String, Object>();
      node.put("id", map.get(AnyDataModelConstants.SYS_COL_PRIMARY));
      node.put("text", map.get("TEXT"));
      node.put("leaf", true);
      node.put("iconCls", "icon-empty");
      nodes.add(node);
    }
    return nodes;
  }
}
