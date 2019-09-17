package com.github.catstiger.anything.oper.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.oper.service.AnyOneToManyService;
import com.github.catstiger.common.web.controller.BaseController;

@Controller
@RequestMapping("/anything/onetomany")
public class AnyOneToManyController extends BaseController {
  @Autowired
  private AnyOneToManyService anyOneToManyService;

  /**
   * 将OneToMany中的Many的一方与One的一方关联
   */
  @RequestMapping("/makeConstraintJdbc")
  @ResponseBody
  public void makeConstraintJdbc(@RequestParam(value = "table") String table, @RequestParam(value = "ownerId") Long ownerId,
      @RequestParam(value = "reverseColumn") String reverseColumn, @RequestParam(value = "manyIds") String manyIds) {
    try {
      if (StringUtils.isNotBlank(manyIds)) {
        String[] strIds = StringUtils.split(manyIds, ",");
        if (strIds != null && strIds.length > 0) {
          List<Long> ids = new ArrayList<Long>(strIds.length);
          for (String strId : strIds) {
            ids.add(Long.valueOf(strId));
          }
          anyOneToManyService.makeConstraintJdbc(table, ownerId, reverseColumn, ids.toArray(new Long[] {}));
        }
      }
      renderJson(JSON.toJSONString(forExt(true)));
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt("")));
    }
  }
}
