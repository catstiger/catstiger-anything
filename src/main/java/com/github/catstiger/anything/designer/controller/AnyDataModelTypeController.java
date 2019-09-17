package com.github.catstiger.anything.designer.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.designer.model.AnyDataModelType;
import com.github.catstiger.anything.designer.service.AnyDataModelTypeService;
import com.github.catstiger.common.web.controller.BaseController;

/**
 * 数据模型类别管理
 * 
 * @author catstiger@gmail.com
 */
@Controller
@RequestMapping("/anything/anydatamodeltype")
public class AnyDataModelTypeController extends BaseController {
  @Autowired
  private AnyDataModelTypeService anyDataModelTypeService;

  /**
   * 保存
   */
  @RequestMapping("/save")
  @ResponseBody
  public Map<String, Object> save(AnyDataModelType model) {
    try {
      if (model.getParent() == null) {
        model.setParent(null);
      }
      anyDataModelTypeService.merge(model);
      return forExt("保存模块类别成功！", true);
    } catch (Exception e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      return forExt("保存模块类别失败！");
    }
  }

  /**
   * Loading for edit
   */
  @RequestMapping("/edit")
  @ResponseBody
  public Map<String, Object> edit(AnyDataModelType model) {
    model = anyDataModelTypeService.get(model.getId());
    return forExt(model, true);
  }

  /**
   * 删除
   */
  @RequestMapping("/remove")
  @ResponseBody
  public Map<String, Object> remove(AnyDataModelType model) {
    try {
      anyDataModelTypeService.remove(model);
      return forExt("删除模块类别成功！", true);
    } catch (Exception e) {
      e.printStackTrace();
      return forExt(e.getMessage());
    }
  }

  /**
   * 输出模块类别属性列表
   */
  @RequestMapping("/tree")
  @ResponseBody
  public void tree(@RequestParam("incRoot") Boolean incRoot) {
    List<Map<String, Object>> tree = anyDataModelTypeService.tree(null);
    if (incRoot) {
      Map<String, Object> root = new HashMap<String, Object>();
      root.put("id", "root");
      root.put("text", "自定义模块");
      root.put("name", "自定义模块");
      root.put("leaf", false);
      root.put("iconCls", "icon-test");
      root.put("expanded", true);
      root.put("children", tree);
      List<Map<String, Object>> fromRoot = new ArrayList<Map<String, Object>>();
      fromRoot.add(root);
      renderJson(JSON.toJSONString(fromRoot));
    } else {
      renderJson(JSON.toJSONString(tree));
    }
  }

  /**
   * 更改位置
   */
  @RequestMapping("/positioning")
  @ResponseBody
  public void positioning(@RequestParam("sourceId") Long sourceId, @RequestParam("destId") Long destId,
      @RequestParam("position") String position) {
    try {
      anyDataModelTypeService.positioning(sourceId, destId, position);
      renderJson(JSON.toJSONString(forExt("操作成功！", true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("操作失败！")));
    }
  }
}
