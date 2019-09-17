package com.github.catstiger.anything.designer.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.anything.designer.service.AnyDataModelFieldService;
import com.github.catstiger.common.model.KeyValue;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.sql.Page;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.BeanPropertyRowMapperEx;
import com.github.catstiger.common.web.controller.BaseController;

/**
 * 数据模型字段管理
 * 
 * @author catstiger@gmail.com
 */
@Controller
@RequestMapping("/anything/anydatamodelfield")
public class AnyDataModelFieldController extends BaseController {
  @Autowired
  private JdbcTemplateProxy jdbcTemplate;
  @Autowired
  private AnyDataModelFieldService anyDataModelFieldService;

  /**
   * 根据AnyDatamodel ID，查询所有的字段信息，按照orders字段排序.
   * @param dataModelId ID of {@link AnyDataModel}
   * @param inputTypeExc 输入方式
   * @param isSys 是否系统字段（ID，updater, update_time）
   * @param refDataModelId 引用的AnyDataModel，适用于外键的情况
   */
  @RequestMapping("/index")
  @ResponseBody
  public void index(@RequestParam(value = "dataModelId", required = false) Long dataModelId,
      @RequestParam(value = "inputTypeExc", required = false) String inputTypeExc,
      @RequestParam(value = "isSys", required = false) Boolean isSys,
      @RequestParam(value = "refDataModelId", required = false) Long refDataModelId) {
    if (dataModelId == null) { // 必须根据某个DataModel查询
      logger.debug("必须根据某个DataModel查询");
      return;
    }
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append(" WHERE data_model_id=?", dataModelId)
        .and(" AND is_sys=?", isSys != null, isSys)
        .append(" AND is_sys=?", isSys != null, new Object[] {isSys})
        .append(" AND input_type<>?", StringUtils.isNotBlank(inputTypeExc), new Object[] {inputTypeExc})
        .append(" AND ref_data_model_id=?", refDataModelId != null, new Object[] {refDataModelId}).orderBy("orders", "asc");
    Page page = page();
    List<AnyDataModelField> rows = jdbcTemplate.query(sqlReady.limitSql(page.getStart(), page.getLimit()),
        new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sqlReady.getArgs());
    // 加载dataModel详情
    for (AnyDataModelField row : rows) {
      if (row.getDataModel() == null || row.getDataModel().getId() == null) {
        continue;
      }
      AnyDataModel dataModel = jdbcTemplate.get(AnyDataModel.class, row.getDataModel().getId());
      row.setDataModel(dataModel);
    }
    // 总数据量
    Long total = jdbcTemplate.queryTotal(sqlReady);

    page.setRows(rows);
    page.setTotal(total);

    renderJson(JSON.toJSONString(page));
  }

  /**
   * 保存字段信息
   */
  @RequestMapping("/save")
  @ResponseBody
  public void save(AnyDataModelField model) {
    try {
      if (model.getRefDataModel() != null && model.getRefDataModel().getId() == null) {
        model.setRefDataModel(null);
      }
      if (model.getRefField() != null && model.getRefField().getId() == null) {
        model.setRefField(null);
      }
      if (model.getRefFieldDisplay() != null && model.getRefFieldDisplay().getId() == null) {
        model.setRefFieldDisplay(null);
      }
      // WebUtil.printParams(getRequest());
      model = anyDataModelFieldService.merge(model);
      renderJson(JSON.toJSONString(forExt(model, true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("保存字段信息失败！")));
    }
  }

  /**
   * 保存系统字段
   */
  @RequestMapping("/saveSys")
  @ResponseBody
  public void saveSys(AnyDataModelField model) {
    try {
      anyDataModelFieldService.saveSys(model);
      renderJson(JSON.toJSONString(forExt(model, true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("保存字段信息失败！")));
    }
  }

  /**
   * 保存一对多字段
   */
  @RequestMapping("/saveOneToMany")
  @ResponseBody
  public void saveOneToMany(AnyDataModelField model) {
    try {
      anyDataModelFieldService.saveOneToMany(model);
      renderJson(JSON.toJSONString(forExt(model, true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("保存字段信息失败！")));
    }
  }

  /**
   * 保存一对多字段，"多"的一方的外键字段
   */
  @RequestMapping("/saveReverseField")
  @ResponseBody
  public void saveReverseField(AnyDataModelField model) {
    try {
      model = anyDataModelFieldService.saveReverseField(model);
      renderJson(JSON.toJSONString(forExt(model, true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("保存字段信息失败！")));
    }
  }

  @RequestMapping("/edit")
  @ResponseBody
  public void edit(AnyDataModelField model) {
    model = anyDataModelFieldService.get(model.getId());
    renderJson(JSON.toJSONString(forExt(model, true)));
  }

  /**
   * 删除字段
   */
  @RequestMapping("/remove")
  @ResponseBody
  public void remove(AnyDataModelField model) {
    if (model.getId() == null) { // 这个算成功，rowediting会造成ID为null的状态
      return;
    }
    try {
      jdbcTemplate.update("delete from any_data_model_field where id=?", model.getId());
      renderJson(JSON.toJSONString(forExt(true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("删除字段信息失败！")));
    }
  }

  /**
   * 用于Ajax验证字段显示名称是否重复
   */
  @RequestMapping("/validateDispName")
  @ResponseBody
  public void validateDispName(AnyDataModelField model) {
    Long count = 0L;
    if (model.getId() == null) {
      count = jdbcTemplate.queryForObject("select count(*) from any_data_model_field a where a.display_name=? and a.data_model_id=?",
          Long.class, model.getDisplayName(), model.getDataModel().getId());
    } else {
      count = jdbcTemplate.queryForObject(
          "select count(*) from any_data_model_field a where a.id <> ? and a.display_name=? and a.data_model_id=?", Long.class,
          model.getId(), model.getDisplayName(), model.getDataModel().getId());
    }

    if (count != null && count > 0L) {
      render(getResponse(), "字段显示名称已经存在！", "text/plain");
    } else {
      render(getResponse(), "true", "text/plain");
    }
  }

  /**
   * 用于Ajax验证字段名称是否重复
   */
  @RequestMapping("/validateFieldName")
  @ResponseBody
  public void validateFieldName(AnyDataModelField model) {
    // 验证是否为空
    if (StringUtils.isBlank(model.getFieldName())) {
      renderText("字段显示名称是必填的！");
      return;
    }
    if (StringUtils.isNumericSpace(model.getFieldName())) {
      renderText("字段显示名称不可都为数字！");
      return;
    }
    // 验证是否重复
    Long count = 0L;
    if (model.getId() == null) {
      count = jdbcTemplate.queryForObject("select count(*) from any_data_model_field a where a.field_name=? and a.data_model_id=?",
          Long.class, model.getFieldName(), model.getDataModel().getId());
    } else {
      count = jdbcTemplate.queryForObject(
          "select count(*) from any_data_model_field a where a.id <> ? and a.field_name=? and a.data_model_id=?", Long.class, model.getId(),
          model.getFieldName(), model.getDataModel().getId());
    }

    if (count != null && count > 0) {
      renderText("字段显示名称已经存在！");
    } else {
      renderText("true");
    }
  }

  /**
   * 字段排序
   */
  @RequestMapping("/sort")
  @ResponseBody
  public void sort(@RequestParam("sourceId") Long sourceId, @RequestParam("destId") Long destId) {
    String direction = getRequest().getParameter("direction");
    if (StringUtils.isBlank(direction)) {
      renderJson(JSON.toJSONString("没有提供拖动位置！"));
      return;
    }

    try {
      if (StringUtils.equals(direction, "before")) {
        this.anyDataModelFieldService.before(sourceId, destId);
        renderJson(JSON.toJSONString(forExt("操作成功！", true)));
      } else if (StringUtils.equals(direction, "after")) {
        this.anyDataModelFieldService.after(sourceId, destId);
        renderJson(JSON.toJSONString(forExt("操作成功！", true)));
      } else {
        renderJson(JSON.toJSONString(forExt("错误的拖动位置！")));
      }
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("操作失败！")));
    }
  }

  /**
   * 所有的外键，除了指向自身的外键
   */
  @RequestMapping("/fkExcludeSelf")
  @ResponseBody
  public void fkExcludeSelf(AnyDataModelField model) {
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append(
        "WHERE is_foreign=true and ref_data_model_id<>? and data_model_id=?", model.getDataModel().getId(), model.getDataModel().getId());

    List<AnyDataModelField> fields = jdbcTemplate.query(sqlReady.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sqlReady.getArgs());
    List<KeyValue<Long, String>> kvs = new ArrayList<KeyValue<Long, String>>();
    for (AnyDataModelField field : fields) {
      kvs.add(new KeyValue<Long, String>(field.getId(), field.getDisplayName()));
    }
    renderJson(JSON.toJSONString(kvs));
  }

  /**
   * 得到指向自身的外键
   */
  @RequestMapping("/fkExcludeOther")
  @ResponseBody
  public void fkExcludeOther(AnyDataModelField model) {
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append(
        "WHERE is_foreign=true and ref_data_model_id=? and data_model_id=?", model.getDataModel().getId(), model.getDataModel().getId());

    List<AnyDataModelField> fields = jdbcTemplate.query(sqlReady.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sqlReady.getArgs());

    List<KeyValue<Long, String>> kvs = new ArrayList<KeyValue<Long, String>>();
    for (AnyDataModelField field : fields) {
      kvs.add(new KeyValue<Long, String>(field.getId(), field.getDisplayName()));
    }
    renderJson(JSON.toJSONString(kvs));
  }

  /**
   * 将某个字段作为TreeCol
   * 
   * @param id Identity of AnyDataModelField
   */
  @RequestMapping("/asTreeCol")
  @ResponseBody
  public void asTreeCol(@RequestParam("id") Long id) {
    anyDataModelFieldService.asTreeCol(id);
    renderJson(JSON.toJSONString(true));
  }

  /**
   * 将某个字段作为分类列表的数据源
   */
  @RequestMapping("/asTreeDs")
  @ResponseBody
  public void asTreeDs(@RequestParam("id") Long id) {
    anyDataModelFieldService.asTreeDs(id);
    renderJson(JSON.toJSONString(true));
  }

  /**
   * 取得指定AnyDataModel的Tree Column
   */
  @RequestMapping("/getTreeCol")
  @ResponseBody
  public void getTreeCol(AnyDataModelField model) {
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append("WHERE as_tree_col=true and data_model_id=?",
        model.getDataModel().getId());

    AnyDataModelField field = jdbcTemplate.queryForObject(sqlReady, AnyDataModelField.class);
    KeyValue<Long, String> kv = new KeyValue<Long, String>();
    if (field != null) {
      kv = new KeyValue<Long, String>(field.getId(), field.getDisplayName());
    }

    renderJson(JSON.toJSONString(kv));
  }

  /**
   * 取得指定AnyDataModel的分类列表数据源字段
   */
  @RequestMapping("/getTreeDs")
  @ResponseBody
  public void getTreeDs(AnyDataModelField model) {
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append("WHERE as_tree_ds=true and data_model_id=?",
        model.getDataModel().getId());

    AnyDataModelField field = jdbcTemplate.queryForObject(sqlReady, AnyDataModelField.class);
    KeyValue<Long, String> kv = new KeyValue<Long, String>();
    if (field != null) {
      kv = new KeyValue<Long, String>(field.getId(), field.getDisplayName());
    }

    renderJson(JSON.toJSONString(kv));
  }

  /**
   * 返回所有指向USERS表的外键
   */
  @RequestMapping("/getUserFk")
  @ResponseBody
  public void getUserFk(AnyDataModelField model) {
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().where()
        .and("((is_foreign=true and ref_table_name='USERS' and ref_data_model_id is null) or input_type=?) and data_model_id=?", 
            AnyDataModelConstants.XTYPE_USERCOMBO, model.getDataModel().getId());
    List<AnyDataModelField> fields = jdbcTemplate.query(sqlReady.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sqlReady.getArgs());

    List<KeyValue<Long, String>> kvs = new ArrayList<KeyValue<Long, String>>();
    for (AnyDataModelField field : fields) {
      kvs.add(new KeyValue<Long, String>(field.getId(), field.getDisplayName()));
    }
    renderJson(JSON.toJSONString(kvs));
  }

}
