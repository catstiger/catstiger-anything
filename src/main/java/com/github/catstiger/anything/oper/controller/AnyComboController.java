package com.github.catstiger.anything.oper.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.anything.oper.service.AnyComboService;
import com.github.catstiger.common.model.KeyValue;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.web.controller.BaseController;

/**
 * 根据任何表的任何字段，提供查询操作，操作结果通常用于combobox 或者treepicker的数据源
 * 
 * @author lizhenshan
 *
 */
@Controller
@RequestMapping("/anything/combo")
public class AnyComboController extends BaseController {
  /**
   * Ext缺省查询参数名称
   */
  public static final String EXT_QUERY_PARAM = "query";

  @Autowired
  private AnyComboService anyComboSvr;
  @Autowired
  private JdbcTemplateProxy jdbcTemplate;

  /**
   * 根据<code>fieldId</code>列出相关AnyDataModelField指向的refDataModel的列表， Key值为AnyDataModel的ID，Value值为AnyDataModelField的refFieldDisplay。
   */
  @RequestMapping("/listByField")
  @ResponseBody
  public void listByField(@RequestParam("fieldId") Long fieldId) {
    String query = getRequest().getParameter(EXT_QUERY_PARAM); // ExtJS combo 自动完成

    AnyDataModelField field = jdbcTemplate.get(AnyDataModelField.class, fieldId);
    List<KeyValue<Long, Object>> list = anyComboSvr.list(field, query);

    renderJson(JSON.toJSONString(list));
  }

  /**
   * 根据<code>dataModelId</code>和<code>column</code>，查询相关Table，并得到KeyValue
   * 
   */
  @RequestMapping("/listByDataModel")
  @ResponseBody
  public void listByDataModel(@RequestParam("dataModelId") Long dataModelId, @RequestParam("column") String column) {
    String query = getRequest().getParameter(EXT_QUERY_PARAM); // ExtJS combo 自动完成
    AnyDataModel dataModel = jdbcTemplate.get(AnyDataModel.class, dataModelId);
    List<KeyValue<Long, Object>> list = anyComboSvr.list(dataModel.getRealTableName(), column, query);

    renderJson(JSON.toJSONString(list));
  }

  /**
   * 根据表名<code>table</code>和显示字段名<code>column</code>，查询相关表， 得到以ID作为Key，以<code>column</code>作为Value的一组数据。
   */
  @RequestMapping("/listByTable")
  @ResponseBody
  public void listByTable(@RequestParam("table") String table, @RequestParam("column") String column) {
    String query = getRequest().getParameter(EXT_QUERY_PARAM); // ExtJS combo 自动完成
    List<KeyValue<Long, Object>> list = anyComboSvr.list(table, column, query);
    renderJson(JSON.toJSONString(list));
  }

  /**
   * 根据<code>fieldId</code>列出相关AnyDataModelField指向的refDataModel的树形列表，
   * 
   */
  @RequestMapping("/treeByField")
  @ResponseBody
  public void treeByField(@RequestParam("fieldId") Long fieldId) {
    AnyDataModelField field = jdbcTemplate.get(AnyDataModelField.class, fieldId);
    if (field.getIsForeign()) {
      List<Map<String, Object>> tree = anyComboSvr.tree(field.getRefDataModel());
      // 如果按照自关联字段查询不到数据，则按照普通表查询
      if (tree.isEmpty()) {
        tree = anyComboSvr.plainTree(field.getRefDataModel().getRealTableName().toUpperCase(), field.getRefFieldDisplay().getFieldName());
      }
      renderJson(JSON.toJSONString(tree));
    } else {
      logger.warn("Tree Field must be a foreign key.");
    }
  }

  /**
   * 根据<code>dataModelId</code>列出相关AnyDataModel指向的table中的数据，并形成 一个树形的数据结构。AnyDataModel的uiStyle必须为treegrid-from，必须有一个treeCol为
   * true的AnyDataModelField，也就是说table中必须有一个指向自身的外键。
   */
  @RequestMapping("/treeByDataModel")
  @ResponseBody
  public void treeByDataModel(@RequestParam("dataModelId") Long dataModelId) {
    AnyDataModel dataModel = jdbcTemplate.get(AnyDataModel.class, dataModelId);
    List<Map<String, Object>> tree = anyComboSvr.tree(dataModel);
    renderJson(JSON.toJSONString(tree));
  }

  /**
   * 根据<code>table</code>，<code>parentColunn</code>（指向自身的外键）和<code>column</code>
   *  （显示字段）列出相关table中的数据，并形成 一个树形的数据结构。Table中必须有一个指向自身的外键。
   */
  @RequestMapping("/treeByTable")
  @ResponseBody
  public void treeByTable(@RequestParam("table") String table, @RequestParam("column") String column,
      @RequestParam("parentColumn") String parentColumn) {
    List<Map<String, Object>> tree = anyComboSvr.tree(table, parentColumn, column);
    renderJson(JSON.toJSONString(tree));
  }
}
