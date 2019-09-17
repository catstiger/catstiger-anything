package com.github.catstiger.anything;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.github.catstiger.common.model.KeyValue;

public abstract class AnyDataModelConstants {
  public static final List<KeyValue<String, String>> DATA_TYPES = new ArrayList<KeyValue<String, String>>();
  public static final List<KeyValue<String, String>> XTYPES = new ArrayList<KeyValue<String, String>>();
  /**
   * 数据类型（数字）
   */
  public static final String DATA_TYPE_NUMBER = "NUMBER";
  /**
   * 数据类型（字符）
   */
  public static final String DATA_TYPE_VERCHAR = "VARCHAR";

  /**
   * 数据类型（日期）
   */
  public static final String DATA_TYPE_DATE = "DATE";

  /**
   * 数据类型（时间）
   */
  public static final String DATA_TYPE_TIME = "TIME";

  /**
   * 数据类型（大文本）
   */
  public static final String DATA_TYPE_CLOB = "CLOB";

  /**
   * 数据类型（BOOLEAN）
   */
  public static final String DATA_TYPE_BOOL = "BOOL";

  public static final String DATA_TYPE_ONETOMANY = "ONE_TO_MANY";

  public static final String DATA_TYPE_MULTIFILES = "MULTI_FILES";
  
  /**
   * Ext combo或者treepicker的ROOT参数名称
   */
  public static final String EXT_ROOT_PARAM = "node";

  /**
   * {@link #EXT_ROOT_ID}的字符串格式
   */
  public static final String ROOT_ID = "-";

  static {
    DATA_TYPES.add(new KeyValue<String, String>(DATA_TYPE_VERCHAR, "文本(<500字符)"));
    DATA_TYPES.add(new KeyValue<String, String>(DATA_TYPE_NUMBER, "数字"));
    DATA_TYPES.add(new KeyValue<String, String>(DATA_TYPE_DATE, "日期"));
    DATA_TYPES.add(new KeyValue<String, String>(DATA_TYPE_TIME, "时间"));
    DATA_TYPES.add(new KeyValue<String, String>(DATA_TYPE_CLOB, "大文本对象"));
    DATA_TYPES.add(new KeyValue<String, String>(DATA_TYPE_BOOL, "布尔类型"));
    DATA_TYPES.add(new KeyValue<String, String>(DATA_TYPE_MULTIFILES, "文件列表"));
  }

  /**
   * 返回数据类型名称
   */
  public static String dataTypeName(String dataType) {
    for (KeyValue<String, String> kv : DATA_TYPES) {
      if (StringUtils.equalsIgnoreCase(dataType, kv.getKey())) {
        return kv.getValue();
      }
    }
    if (AnyDataModelConstants.DATA_TYPE_ONETOMANY.equals(dataType)) {
      return "一对多";
    }
    return null;
  }

  public static final Map<Class<?>, String> JS_TYPES = new HashMap<Class<?>, String>();
  
  static {
    JS_TYPES.put(Long.class, "int");
    JS_TYPES.put(Integer.class, "int");
    JS_TYPES.put(Boolean.class, "boolean");
    JS_TYPES.put(Double.class, "float");
    JS_TYPES.put(Float.class, "float");
    JS_TYPES.put(String.class, "string");
  }
  
  /**
   * 文本输入框
   */
  public static final String XTYPE_TEXTFIELD = "textfield";
  /**
   * 数字输入框
   */
  public static final String XTYPE_NUMBERFIELD = "numberfield";
  /**
   * 日期输入框
   */
  public static final String XTYPE_DATEFIELD = "datefield";

  /**
   * 时间输入框
   */
  public static final String XTYPE_DATETIMEFIELD = "datetimefield";
  /**
   * 下拉选择
   */
  public static final String XTYPE_COMBOBOX = "combobox";
  /**
   * 树形列表
   */
  public static final String XTYPE_TREEPICKER = "treepicker";

  /**
   * 多选树形列表
   */
  public static final String XTYPE_TREEFIELD = "treefield";
  /**
   * 文本区域
   */
  public static final String XTYPE_TEXTAREA = "textareafield";
  /**
   * 所见即所得编辑
   */
  public static final String XTYPE_KINDEDITOR = "kindeditor";
  /**
   * 用户选择
   */
  public static final String XTYPE_USER = "userselector";

  /**
   * 用户多选
   */
  public static final String XTYPE_USERCOMBO = "usercombo";
  /**
   * 部门选择
   */
  public static final String XTYPE_DEPT = "deptselector";
  /**
   * 文件上传
   */
  public static final String XTYPE_FILEUPLOAD = "simpleuploader";

  /**
   * 多文件上传
   */
  public static final String XTYPE_MULTIFILEUPLOAD = "multifileuploader";

  /**
   * 一对多
   */
  public static final String XTYPE_ONETOMANY = "onetomanyfield";

  /**
   * 布尔
   */
  public static final String XTYPE_BOOLEAN = "booleanfield";
  /**
   * 隐藏域
   */
  public static final String XTYPE_HIDDEN = "hidden";

  // 修改XTYP的时候，注意同步修改jtiger.js中hideOrDisplayField函数
  static {
    XTYPES.add(new KeyValue<String, String>(XTYPE_TEXTFIELD, "文本输入框"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_NUMBERFIELD, "数字输入框"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_DATEFIELD, "日期输入框"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_DATETIMEFIELD, "时间输入框"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_COMBOBOX, "下拉选择"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_TREEPICKER, "树形选择（单选）"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_TREEFIELD, "树形选择（多选）"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_TEXTAREA, "多行文本输入框"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_KINDEDITOR, "所见即所得编辑器"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_USER, "用户选择（单选）"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_USERCOMBO, "用户选择（多选）"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_DEPT, "部门选择"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_FILEUPLOAD, "单文件上传"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_MULTIFILEUPLOAD, "多文件上传"));
    // XTYPES.add(new KeyValue<String, String>(XTYPE_ONETOMANY, "一对多"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_BOOLEAN, "“是/否”输入"));
    XTYPES.add(new KeyValue<String, String>(XTYPE_HIDDEN, "隐藏域"));
  }

  /**
   * 返回数据类型名称
   */
  public static String inputName(String xtype) {
    for (KeyValue<String, String> kv : XTYPES) {
      if (StringUtils.equalsIgnoreCase(xtype, kv.getKey())) {
        return kv.getValue();
      }
    }
    return null;
  }

  /**
   * 返回所有的数据类型
   */
  public static List<String> allXtypes() {
    List<String> all = new ArrayList<String>(XTYPES.size());
    for (KeyValue<String, String> kv : XTYPES) {
      all.add(kv.getKey());
    }
    return all;
  }

  /**
   * 界面布局（仅表单）
   */
  public static final String UI_STYLE_FORM = "just_form";

  /**
   * 界面布局（表格+表单）
   */
  public static final String UI_STYLE_GRID_FORM = "grid_form";

  /**
   * 界面布局（树形表格+表单）
   */
  public static final String UI_STYLE_TREEGRID_FORM = "treegrid_form";

  /**
   * 界面布局（树形+表格+表单）
   */
  public static final String UI_STYLE_BORDER_FORM = "border_form";

  public static final List<KeyValue<String, String>> UI_STYLES = new ArrayList<KeyValue<String, String>>();

  static {
    UI_STYLES.add(new KeyValue<String, String>(UI_STYLE_FORM, "仅表单"));
    UI_STYLES.add(new KeyValue<String, String>(UI_STYLE_GRID_FORM, "表格+表单"));
    UI_STYLES.add(new KeyValue<String, String>(UI_STYLE_TREEGRID_FORM, "树形列表+表单"));
    UI_STYLES.add(new KeyValue<String, String>(UI_STYLE_BORDER_FORM, "树+表格+表单"));
  }

  /**
   * 表单弹出方式（对话框）
   */
  public static final String FORM_POS_WIN = "win";
  /**
   * 表单弹出方式（左侧滑出）
   */
  public static final String FORM_POS_LEFT = "left";
  /**
   * 表单弹出方式（右侧滑出）
   */
  public static final String FORM_POS_RIGHT = "right";
  /**
   * 表单弹出方式（下方滑出）
   */
  public static final String FORM_POS_BOTTOM = "bottom";

  public static final List<KeyValue<String, String>> FORM_POS = new ArrayList<KeyValue<String, String>>();

  /**
   * 表名前缀
   */
  public static final String TABLE_NAME_PREFIX = "t_";

  public static final String DEFAULT_TABLE_ALIAS = "me_";
  public static final String DEFAULT_TABLE_ALIAS_DOT = "me_.";
  public static final String DEFAULT_TABLE_ALIAS_BLANK = " me_ ";

  static {
    FORM_POS.add(new KeyValue<String, String>(FORM_POS_WIN, "在弹出窗口中"));
    FORM_POS.add(new KeyValue<String, String>(FORM_POS_LEFT, "从左侧滑出"));
    FORM_POS.add(new KeyValue<String, String>(FORM_POS_RIGHT, "从右侧滑出"));
    FORM_POS.add(new KeyValue<String, String>(FORM_POS_BOTTOM, "从下方滑出"));
  }

  /**
   * 系统字段名称
   */
  public static final String SYS_COL_UPDATE_TIME = "update_time";
  public static final String SYS_COL_PRIMARY = "id";
  public static final String SYS_COL_USER = "updater_id";
  public static final String SYS_COL_PID = "process_instance_id";

  /**
   * 数据权限，开放，任何人可见
   */
  public static final String DATA_PRIV_OPEN = "PRIV_OPEN";
  /**
   * 数据权限，私人数据
   */
  public static final String DATA_PRIV_SELF = "PRIV_SELF";
  /**
   * 数据权限，同部门可见
   */
  public static final String DATA_PRIV_DEPT = "PRIV_DEPT";

  /**
   * 数据权限，领导可见
   */
  public static final String DATA_PRIV_LEADER = "PRIV_LEADER";
  /**
   * 数据权限，根据指定字段
   */
  public static final String DATA_PRIV_FIELD = "PRIV_FIELD";

  /**
   * 为Tablename加入前缀
   * 
   * @param tablenameWithoutPrefix 不带前缀的tablename
   */
  public static String getReadTablename(String tablenameWithoutPrefix) {
    String tn = AnyDataModelConstants.TABLE_NAME_PREFIX + tablenameWithoutPrefix;
    if (tn.length() > 30) {
      tn = tn.substring(0, 30);
    }
    return tn;
  }

}
