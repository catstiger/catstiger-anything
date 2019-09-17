package com.github.catstiger.anything.designer.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelType;
import com.github.catstiger.anything.designer.service.AnyDataModelService;
import com.github.catstiger.common.model.KeyValue;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.sql.LikeMode;
import com.github.catstiger.common.sql.Page;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.BeanPropertyRowMapperEx;
import com.github.catstiger.common.web.controller.BaseController;
import com.github.catstiger.websecure.user.UserHolder;
import com.github.catstiger.websecure.user.model.User;

/**
 * 数据模型管理
 * 
 * @author catstiger@gmail.com
 */
@Controller
@RequestMapping("/anything/anydatamodel")
public class AnyDataModelController extends BaseController {
  @Autowired
  private JdbcTemplateProxy jdbcTemplate;
  @Autowired
  private AnyDataModelService anyDataModelService;

  /**
   * 列出所有AnyDataModel对象，可以根据类型(typeId), 显示名称(displayName)查询
   * @param model Pattern
   */
  @RequestMapping("/index")
  @ResponseBody
  public void index(AnyDataModel model) {
    SQLReady sqlReady = new SQLRequest(AnyDataModel.class, true).select().append("WHERE 1=1")
        .append(" AND type_id=?", model.getTypeId() != null, new Object[] {model.getTypeId()})
        .append(" AND type_id in (select id from any_data_model_type t where t.path like ?) ", getRequest().getParameter("path") != null,
            new Object[] {getRequest().getParameter("path") + "%"})
        .append(" AND display_name like ?", StringUtils.isNotBlank(model.getDisplayName()), 
            new Object[] {LikeMode.FULL.matching(model.getDisplayName())})
        .orderBy("orders", "asc");

    Page page = page();
    // 加载Type详情
    List<AnyDataModel> rows = jdbcTemplate.query(sqlReady.getSql(), new BeanPropertyRowMapperEx<AnyDataModel>(AnyDataModel.class),
        sqlReady.getArgs());
    for (Iterator<AnyDataModel> itr = rows.iterator(); itr.hasNext();) {
      AnyDataModel anyDataModel = itr.next();
      if (anyDataModel.getTypeId() != null) {
        AnyDataModelType type = jdbcTemplate.get(AnyDataModelType.class, anyDataModel.getTypeId());
        anyDataModel.setType(type);
      }
    }
    // 总数据量
    Long total = jdbcTemplate.queryTotal(sqlReady);

    page.setRows(rows);
    page.setTotal(total);

    renderJson(JSON.toJSONString(page));
  }

  /**
   * 保存(更新或者创建)一个AnyDataModel对象
   * @param model 包含AnyDataMode必要的数据。
   */
  @RequestMapping("/save")
  @ResponseBody
  public void save(AnyDataModel model) {
    try {
      model = anyDataModelService.merge(model, UserHolder.getUser());
      renderJson(JSON.toJSONString(forExt(model, true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("操作失败！")));
    }
  }

  /**
   * 查询一个Model（为了修改）
   */
  @RequestMapping("/edit")
  @ResponseBody
  public void edit(AnyDataModel model) {
    try {
      model = anyDataModelService.get(model.getId());
      renderJson(JSON.toJSONString(forExt(model, true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("无法获取数据。")));
    }
  }

  /**
   * 删除一个AnyDataModel，包括表，数据，字段描述，组件等。
   */
  @RequestMapping("/remove")
  @ResponseBody
  public void remove(AnyDataModel model) {
    try {
      anyDataModelService.remove(model);
      renderJson(JSON.toJSONString(forExt(true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt(e.getMessage())));
    }
  }

  /**
   * AnyDataModel排序
   * @param sourceId 源对象ID
   * @param destId 目标对象ID
   * @param position 排序位置，after：源对象排在目标对象后面，before：源对象排在目标对象前面
   */
  @RequestMapping("/sort")
  @ResponseBody
  public void sort(@RequestParam("sourceId") Long sourceId, @RequestParam("destId") Long destId,
      @RequestParam("position") String position) {
    try {
      anyDataModelService.sort(sourceId, destId, position);
      renderJson(JSON.toJSONString(forExt(true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt(e.getMessage())));
    }
  }


  /**
   * 创建一个表
   * @param model 包含ID即可
   */
  @RequestMapping("/createTable")
  @ResponseBody
  public void createTable(AnyDataModel model) {
    try {
      anyDataModelService.createTable(model);
      renderJson(JSON.toJSONString(forExt(true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("创建表失败")));
    }
  }

  /**
   * 物理删除一个Table
   */
  @RequestMapping("/dropTable")
  @ResponseBody
  public void dropTable(AnyDataModel model) {
    try {
      anyDataModelService.dropTable(model);
      renderJson(JSON.toJSONString(forExt(true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("删除表失败")));
    }
  }

  /**
   * 验证显示名称是否正确（存在，并且没有重复）
   * @param model 包含显示名称，id等字段
   */
  @RequestMapping("/validateDispName")
  @ResponseBody
  public void validateDispName(AnyDataModel model) {
    if (StringUtils.isBlank(model.getDisplayName())) {
      renderText("显示名称是必须的！");
      return;
    }
    model.setDisplayName(model.getDisplayName().toUpperCase());

    if (jdbcTemplate.exists("any_data_model", model.getId(),
        Arrays.asList(new KeyValue<String, Object>("display_name", model.getDisplayName())))) {
      renderText("显示名称已经存在！");
    } else {
      renderText("true");
    }
  }

  /**
   * 验证表名是否存在或者重复，如果为否，则，render "true"
   * @param model 包含表名，id等必要的字段
   */
  @RequestMapping("/validateTableName")
  @ResponseBody
  public void validateTableName(AnyDataModel model) {
    if (StringUtils.isBlank(model.getTableName())) {
      renderText("表名称是必须的！");
      return;
    }
    model.setDisplayName(model.getTableName().toUpperCase());
    if (jdbcTemplate.exists("any_data_model", model.getId(),
        Arrays.asList(new KeyValue<String, Object>("table_name", model.getTableName())))) {
      renderText("表名已经存在！");
    } else {
      renderText(Boolean.TRUE.toString());
    }
  }

  /**
   * 更新UiStyle字段
   */
  @RequestMapping("/updateUiStyle")
  @ResponseBody
  public void updateUiStyle(AnyDataModel model) {
    try {
      anyDataModelService.updateUiStyle(model.getId(), model.getUiStyle());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 更新FormPos
   */
  @RequestMapping("/updateFormPos")
  @ResponseBody
  public void updateFormPos(AnyDataModel model) {
    try {
      anyDataModelService.updateFormPos(model);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 将AnyDataModelType和AnyDataModel构建成系统菜单。 每一个顶级AnyDataModelType对应一个accordin的panel，
   * 其中的子AnyDataModelType分类和AnyDataModel作为panel中的树形列表
   */
  @RequestMapping("/anyMenu")
  @ResponseBody
  public void anyMenu() {
    User user = UserHolder.getUser();
    if (user == null) {
      return;
    }
    // 因为菜单是后添加的排在前面，所以这里用desc排序
    SQLReady sqlReady = new SQLRequest(AnyDataModelType.class, true).select().append("WHERE parent_id is null").orderBy("orders", "asc");
    List<AnyDataModelType> types = jdbcTemplate.query(sqlReady.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelType>(AnyDataModelType.class), sqlReady.getArgs());

    List<AnyDataModelType> roots = new ArrayList<AnyDataModelType>(types.size());

    for (AnyDataModelType root : types) {
      List<Map<String, Object>> menu = anyDataModelService.dataModelTree(root.getId(), user, true, true);
      if (!menu.isEmpty()) { // 如果是空的Panel，界面上就不显示了
        logger.debug("Model Tree {}", JSON.toJSONString(menu));
        root.setMenu(menu);
        roots.add(root);
      }
    }
    renderJson(JSON.toJSONString(roots));
  }

  /**
   * 流程绑定的时候，列出所有模块
   */
  @RequestMapping("/treeForPicker")
  @ResponseBody
  public void treeForPicker(@RequestParam(value = "hasTable", defaultValue = "false") Boolean hasTable,
      @RequestParam(value = "node", defaultValue = AnyDataModelConstants.ROOT_ID) Long node) {
    User user = UserHolder.getUser();
    if (user == null) {
      return;
    }

    SQLReady sqlReady = new SQLRequest(AnyDataModelType.class, true).select().append("WHERE 1=1")
        // 解决treepicker递归的办法，就是让它查询不到branches
        .append(" AND parent_id is null", node == null || node < 0L, new Object[] {})
        .append(" AND parent_id=?", node != null && node >= 0, new Object[] {node}) 
        .orderBy("orders", "asc");
    List<AnyDataModelType> types = jdbcTemplate.query(sqlReady.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelType>(AnyDataModelType.class), sqlReady.getArgs());

    List<Map<String, Object>> roots = new ArrayList<Map<String, Object>>();
    for (AnyDataModelType type : types) {
      List<Map<String, Object>> menu = anyDataModelService.dataModelTree(type.getId(), user, false, hasTable);
      Map<String, Object> rootMap = new HashMap<String, Object>();
      rootMap.put("id", type.getId());
      rootMap.put("text", type.getName());
      rootMap.put("children", menu);
      // rootMap.put("leaf", menu.isEmpty());
      rootMap.put("expanded", true);
      roots.add(rootMap);
    }

    renderJson(JSON.toJSONString(roots));
  }

  /**
   * 数据类型
   */
  @RequestMapping("/dataTypes")
  @ResponseBody
  public void dataTypes() {
    renderJson(JSON.toJSONString(AnyDataModelConstants.DATA_TYPES));
  }

  /**
   * 输入方式
   */
  @RequestMapping("/xtypes")
  @ResponseBody
  public void xtypes() {
    renderJson(JSON.toJSONString(AnyDataModelConstants.XTYPES));
  }

  /**
   * 界面风格
   */
  @RequestMapping("/uiStyles")
  @ResponseBody
  public void uiStyles() {
    renderJson(JSON.toJSONString(AnyDataModelConstants.UI_STYLES));
  }

  /**
   * 表单位置
   */
  @RequestMapping("/formPos")
  @ResponseBody
  public void formPos() {
    renderJson(JSON.toJSONString(AnyDataModelConstants.FORM_POS));
  }
}
