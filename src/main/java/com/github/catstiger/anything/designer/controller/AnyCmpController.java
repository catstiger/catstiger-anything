package com.github.catstiger.anything.designer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.designer.model.AnyCmp;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.web.controller.BaseController;

/**
 * 表单组件管理
 * @author lizhenshan
 *
 */
@Controller
@RequestMapping("/anything/anycmp")
public class AnyCmpController extends BaseController {
  @Autowired
  private JdbcTemplateProxy jdbcTemplate;
  
  /**
   * 删除一个表单组件
   */
  @RequestMapping("/remove")
  @ResponseBody
  public void remove(AnyCmp model) {
    try {
      jdbcTemplate.update("delete from any_cmp where id=?", model.getId());
      renderJson(JSON.toJSONString(forExt(true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("删除失败！")));
    }
  }
}
