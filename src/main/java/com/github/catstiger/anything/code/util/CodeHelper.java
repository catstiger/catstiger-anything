package com.github.catstiger.anything.code.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.common.util.Exceptions;
import com.google.common.base.Preconditions;

public abstract class CodeHelper {
  private static Logger logger = LoggerFactory.getLogger(CodeHelper.class);
  public static final String TABLE_NAME_SPLITOR = "_";
  private static Map<String, String> models = new HashMap<String, String>(200);

  /**
   * 将表名转换为类名
   * 
   * @param tableName 表名
   * @return 类名
   */
  public static String getClassName(String tableName) {
    // 去掉表名前缀
    if (StringUtils.indexOf(tableName, AnyDataModelConstants.TABLE_NAME_PREFIX) == 0
        && tableName.toLowerCase().startsWith(AnyDataModelConstants.TABLE_NAME_PREFIX.toLowerCase())) {
      tableName = StringUtils.replace(tableName.toUpperCase(), AnyDataModelConstants.TABLE_NAME_PREFIX.toUpperCase(), "");
    }
    String[] sections = tableName.split(TABLE_NAME_SPLITOR);
    StringBuilder buf = new StringBuilder(tableName.length());

    for (String section : sections) {
      buf.append(section.substring(0, 1).toUpperCase()).append(section.substring(1).toLowerCase());
    }

    return buf.toString();
  }

  /**
   * 表名转换为变量名
   * 
   * @param tableName 表名
   * @return 变量名
   */
  public static String getVarName(String tableName) {
    String name = getClassName(tableName);
    return name.substring(0, 1).toLowerCase() + name.substring(1);
  }

  /**
   * 将列明转换为属性名
   * 
   * @param columnName 列明
   * @param isForeign 是否是外键
   * @return 属性名
   */
  public static String getPropertyName(String columnName, boolean isForeign) {
    Preconditions.checkArgument(StringUtils.isNotBlank(columnName));
    columnName = columnName.toLowerCase();
    // 外键去掉_id后缀
    if (columnName.endsWith("_id") && isForeign) {
      columnName = columnName.substring(0, columnName.lastIndexOf("_"));
    }
    String[] sections = columnName.split(TABLE_NAME_SPLITOR);
    if (sections == null) {
      throw new IllegalArgumentException("no column name.");
    }
    StringBuilder buf = new StringBuilder(columnName.length()).append(sections[0].toLowerCase());
    if (sections.length > 1) {
      for (int i = 1; i < sections.length; i++) {
        buf.append(sections[i].substring(0, 1).toUpperCase()).append(sections[i].substring(1).toLowerCase());
      }
    }
    return buf.toString();
  }

  /**
   * 得到AnyDataModelField对象对应的Java类Class对象
   * 
   * @param safe 如果为true，那么找不到的类会返回String.class,否则返回null
   */
  public static Class<?> getJavaType(AnyDataModelField field, boolean safe) {
    if (models.isEmpty()) {
      findModels();
    }
    Class<?> javaType = ((safe) ? String.class : null);
    if (StringUtils.equals(field.getDataType(), AnyDataModelConstants.DATA_TYPE_VERCHAR)) {
      javaType = String.class;
    } else if (StringUtils.equals(field.getDataType(), AnyDataModelConstants.DATA_TYPE_DATE)
        || StringUtils.equals(field.getDataType(), AnyDataModelConstants.DATA_TYPE_TIME)) {
      javaType = Date.class;
    } else if (StringUtils.equals(field.getDataType(), AnyDataModelConstants.DATA_TYPE_CLOB)) {
      javaType = String.class;
    } else if (StringUtils.equals(field.getDataType(), AnyDataModelConstants.DATA_TYPE_NUMBER)) {
      javaType = numberType(field, safe);
    } else if (StringUtils.equals(field.getDataType(), AnyDataModelConstants.DATA_TYPE_BOOL)) {
      javaType = Boolean.class;
    }

    return javaType;
  }
  
  private static Class<?> numberType(AnyDataModelField field, boolean safe) {
    Class<?> javaType = ((safe) ? String.class : null);
    
    if (field.getIsForeign()) {
      String clz = null;
      if (StringUtils.isNotBlank(field.getRefTableName())) {
        clz = models.get(field.getRefTableName());
      } else if (field.getRefDataModel() != null) {
        clz = models.get(field.getRefDataModel().getRealTableName());
      }
      if (clz != null) {
        try {
          javaType = ClassUtils.forName(clz, CodeHelper.class.getClassLoader());
        } catch (Throwable e) {
          e.printStackTrace();
          throw Exceptions.unchecked(e);
        }
      }
    } else {
      if (field.getIsPrimary()) {
        javaType = Long.class;
      } else if (field.getDataScale() != null && field.getDataScale() > 0) {
        javaType = Double.class;
      } else {
        javaType = Integer.class;
      }
    }
    
    return javaType;
  }

  /**
   * 找到系统中所有的实体类，即被<code>javax.persistence.Table</code>标注的类
   * 
   * @return Key 是实体类对应的表名（大写），value是实体类的类名
   */
  public static Map<String, String> findModels() {
    if (!models.isEmpty()) {
      return models;
    }
    File file = null;
    try {
      file = ResourceUtils.getFile(CodeHelper.class.getResource("/"));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return findModels(file);
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static Map<String, String> findModels(File root) {
    if (root == null) {
      return models;
    }

    File[] files = root.listFiles();
    for (File file : files) {
      if (file.exists() && file.isDirectory()) {
        findModels(file);
      } else {
        if (file.getName().endsWith(".class")) {
          try {
            File clsPath = ResourceUtils.getFile(CodeHelper.class.getResource("/"));
            String clsName = StringUtils.replace(file.getPath(), clsPath.getPath() + File.separator, "");
            clsName = clsName.substring(0, clsName.lastIndexOf(".class"));
            clsName = StringUtils.replace(clsName, File.separator, ".");
            Class clz = ClassUtils.forName(clsName, CodeHelper.class.getClassLoader());
            if (clz != null) {
              // 标注的实体类
              javax.persistence.Table ann = (javax.persistence.Table) clz.getAnnotation(javax.persistence.Table.class);
              if (ann != null) {
                logger.debug("Handle annoted class {}", clz.getName());
                models.put(ann.name().toUpperCase(), clz.getName());
              } else { // HBM.xml实体类
                String shortName = ClassUtils.getShortName(clsName);
                String hbmName = file.getParentFile().getPath() + File.separator + shortName + ".hbm.xml";
                File hbm = new File(hbmName);
                if (hbm.exists()) {
                  logger.debug("Handle hbm {}", hbm.getName());
                  String cnt = FileCopyUtils.copyToString(new FileReader(hbm));
                  int idxBegin = cnt.indexOf("table=\"") + "table=\"".length();
                  int idxEnd = cnt.indexOf("\"", idxBegin);
                  String tableName = cnt.substring(idxBegin, idxEnd);
                  models.put(tableName.toUpperCase(), clz.getName());
                }
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    return models;
  }
}
