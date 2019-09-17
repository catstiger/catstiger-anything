package com.github.catstiger.anything.meta.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.meta.FieldDescriptor;
import com.github.catstiger.anything.meta.MetaService;
import com.github.catstiger.common.web.controller.BaseController;

/**
 * 用于获取模块信息，提供给表单绑定使用
 *
 */
@Controller
@RequestMapping("/anything/meta")
public class MetaController extends BaseController {

  @Resource
  private MetaService metaService;

  @RequestMapping("/listModels")
  @ResponseBody
  public void listModels() {
    List<Map<String, Object>> models = metaService.loadModels();
    renderJson(JSON.toJSONString(models));
  }
  
  /**
   * 根据Form key 列出字段
   * @param formKey form Key ，在Any状态下，是一个AnyDataModel ID;在实体状态下，是一个Ext View, 
   *     需要在对应的Model类中标注
   */
  @RequestMapping("/listFields")
  @ResponseBody
  public void listFields(@RequestParam(name = "formKey", required = false) String formKey) {
    logger.debug(formKey);
    if (StringUtils.isBlank(formKey)) {
      return;
    }
    Class<?> clazz = metaService.findClassByFormKey(formKey);

    if (clazz != null) {
      logger.debug("{}", clazz.getName());
      List<FieldDescriptor> fields = metaService.getFieldsByModel(clazz);
      renderJson(JSON.toJSONString(fields));
    }
  }

}
