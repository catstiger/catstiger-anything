package com.github.catstiger.anything.json;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public final class FieldExtractor {
  private static Logger logger = LoggerFactory.getLogger(FieldExtractor.class);

  /**
   * 在不使用Json的场合，例如导出为Excel，使用字段标注的JsonSerialize将字段值进行转换； 目前支持日期、时间、文件、外键的转换。
   * 
   * @param obj 宿主对象
   * @param field 字段
   * @return 如果对应的字段值为null，则返回StringUtils#EMPTY;否则返回转换后的字段。
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static String extract(Object obj, Field field) {
    ReflectionUtils.makeAccessible(field);
    Object value;
    try {
      value = ReflectionUtils.getField(field, obj);
    } catch (Exception e) {
      logger.error("无法获取字段值 {}, {}, cause: \n{}", field.getName(), obj.getClass().getSimpleName(), e.getMessage());
      return StringUtils.EMPTY;
    }
    if (value == null) {
      return StringUtils.EMPTY;
    }

    String str = value.toString();
    PropertyDescriptor propDesc = BeanUtils.getPropertyDescriptor(field.getDeclaringClass(), field.getName());
    if (propDesc.getReadMethod() != null) {
      JsonSerialize ann = propDesc.getReadMethod().getAnnotation(JsonSerialize.class);
      if (ann != null) {
        Class jsonSerializerClass = ann.using();
        if (jsonSerializerClass != null) {
          try {
            Object jsonSerializer = BeanUtils.instantiateClass(jsonSerializerClass);
            if (jsonSerializer != null && (jsonSerializer instanceof JsonSerializer)) {
              XlsJsonGenerator jsonGen = new XlsJsonGenerator();
              ((JsonSerializer) jsonSerializer).serialize(value, jsonGen, new ObjectMapper().getSerializerProvider());
              str = jsonGen.getStr();
            }
          } catch (Exception e) {
            logger.info(e.getMessage());
          }
        }
      }
    }

    return str;
  }

  private FieldExtractor() {

  }
}
