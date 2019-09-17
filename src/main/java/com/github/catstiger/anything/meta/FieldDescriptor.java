package com.github.catstiger.anything.meta;

import java.lang.reflect.Field;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.catstiger.common.web.converter.StringToDateConverter;

/**
 * Field 描述对象
 * 
 * @author lizhenshan
 *
 */
public class FieldDescriptor {
  private String fieldName;
  private String displayName;
  private String id;
  private String dataType;
  private Field field;

  public FieldDescriptor() {
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  @JsonIgnore
  public Field getField() {
    return field;
  }

  public void setField(Field field) {
    this.field = field;
  }

  /**
   * 根据Field对象的类型，将一个String转换为相应的类型
   * 
   * @param value String value
   */
  public Object getValue(String value) {
    if (value == null || StringUtils.isBlank(value)) {
      return null;
    }
    Object val = null;
    try {
      if (field.getType().equals(String.class)) {
        val = value;
      } else if (field.getType().equals(Integer.class)) {
        val = Integer.valueOf(value);
      } else if (field.getType().equals(Long.class)) {
        val = Long.valueOf(value);
      } else if (field.getType().equals(Double.class)) {
        val = Double.valueOf(value);
      } else if (field.getType().equals(Float.class)) {
        val = Float.valueOf(value);
      } else if (field.getType().equals(Date.class)) {
        val = StringToDateConverter.getInstance().convert(value);
      }
    } catch (Exception e) {
      //Do nothing
    }

    return val;
  }
}
