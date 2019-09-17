package com.github.catstiger.anything.designer.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.designer.model.AnyCt;
import com.github.catstiger.anything.designer.service.AnyCtService;
import com.github.catstiger.common.model.KeyValue;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.web.controller.BaseController;

/**
 * 表单组件容器管理
 * @author lizhenshan
 */
@Controller
@RequestMapping("/anything/anyct")
public class AnyCtController extends BaseController {

  @Autowired
  private AnyCtService anyCtService;
  @Autowired
  private JdbcTemplateProxy jdbcTemplate;
 
  
  /**
   * 接收json数据，转换为AnyCt数组，然后保存这一组容器组件
   */
  @RequestMapping("/save")
  @ResponseBody
  public void save() {
    try {
      String json = getRequest().getParameter("json");
      logger.debug("JSON of form {}", json);
      if (json != null) {
        List<AnyCt> anyCts = JSON.parseArray(json, AnyCt.class);
        if (anyCts != null) {
          List<KeyValue<String, String>> cmpIds = anyCtService.save(anyCts);
          renderJson(JSON.toJSONString(cmpIds));
        }
      }
    } catch (Exception e) {
      renderJson(JSON.toJSONString(forExt("保存表单失败！")));
      e.printStackTrace();
    }
  }
  
  /**
   * 删除一个组件容器
   */
  @RequestMapping("/remove")
  @ResponseBody
  public void remove(AnyCt model) {
    try {
      jdbcTemplate.update("delete from any_ct where id=?", model.getId());
      renderJson(JSON.toJSONString(forExt(true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString("操作失败！"));
    }
  }
  
  /**
   * 加载Form表单
   */
  @RequestMapping("/loadForm") 
  @ResponseBody
  public void loadForm(@RequestParam("dataModelId") Long dataModelId,
      @RequestParam(value = "incHidden", defaultValue = "false") Boolean incHidden) {
    List<AnyCt> cts = anyCtService.loadByDataModel(dataModelId, incHidden);
    renderJson(JSON.toJSONString(cts));
  }

}
