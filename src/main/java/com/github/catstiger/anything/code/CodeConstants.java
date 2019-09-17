package com.github.catstiger.anything.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.catstiger.common.model.KeyValue;

public abstract class CodeConstants {
  /**
   * 源代码类型
   */
  public static final String TYPE_JAVA_MODEL = "java_model";
  public static final String TYPE_JAVA_SERVICE = "java_service";
  public static final String TYPE_JAVA_CONTROLLER = "java_controller";
  public static final String TYPE_JS_MODEL = "js_model";
  public static final String TYPE_JS_STORE = "js_store";
  public static final String TYPE_JS_FORM = "js_form";
  public static final String TYPE_JS_VIEW = "js_view";
  public static final String TYPE_TEMPLATE = "ftl_template";

  /**
   * 文件后缀
   */
  public static final Map<String, String> SUFFIXES = new HashMap<String, String>();

  static {
    SUFFIXES.put(TYPE_JAVA_MODEL, ".java");
    SUFFIXES.put(TYPE_JAVA_SERVICE, "Service.java");
    SUFFIXES.put(TYPE_JAVA_CONTROLLER, "Controller.java");
    SUFFIXES.put(TYPE_JS_MODEL, "Model.js");
    SUFFIXES.put(TYPE_JS_STORE, "Store.js");
    SUFFIXES.put(TYPE_JS_FORM, "Form.js");
    SUFFIXES.put(TYPE_JS_VIEW, "View.js");
    SUFFIXES.put(TYPE_TEMPLATE, ".ftl");
  }
  
  /**
   * 对应的包名
   */
  public static final Map<String, String> PACKAGES = new HashMap<String, String>();
 
  static {
    PACKAGES.put(TYPE_JAVA_MODEL, "model");
    PACKAGES.put(TYPE_JAVA_SERVICE, "service");
    PACKAGES.put(TYPE_JAVA_CONTROLLER, "controller");
    PACKAGES.put(TYPE_JS_MODEL, "");
    PACKAGES.put(TYPE_JS_STORE, "");
    PACKAGES.put(TYPE_JS_FORM, "");
    PACKAGES.put(TYPE_JS_VIEW, "");
  }
  
  /**
   * 文件类型名称
   */
  public static final List<KeyValue<String, String>> TYPE_NAMES = new ArrayList<KeyValue<String, String>>();
  
  static {
    TYPE_NAMES.add(new KeyValue<String, String>(TYPE_JAVA_MODEL, "Java实体类"));
    TYPE_NAMES.add(new KeyValue<String, String>(TYPE_JAVA_SERVICE, "Java服务类"));
    TYPE_NAMES.add(new KeyValue<String, String>(TYPE_JAVA_CONTROLLER, "JavaController类"));
    // TYPE_NAMES.add(new KeyValue<String, String>(TYPE_JAVA_CONTROLLER, "Freemarker模板")); //暂不提供静态查看功能
    TYPE_NAMES.add(new KeyValue<String, String>(TYPE_JS_MODEL, "Js Model"));
    TYPE_NAMES.add(new KeyValue<String, String>(TYPE_JS_STORE, "Js Store"));
    TYPE_NAMES.add(new KeyValue<String, String>(TYPE_JS_FORM, "Js表单"));
    TYPE_NAMES.add(new KeyValue<String, String>(TYPE_JS_VIEW, "Js视图"));
  }
}
