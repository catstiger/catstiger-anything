package com.github.catstiger.anything.json;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.catstiger.anything.annotation.Meta;
import com.github.catstiger.modules.api.model.Department;
import com.github.catstiger.websecure.user.model.User;

/**
 * 外键转换。转换规则：<br>
 * <ul>
 * <li>外键指向User，转换为User.name</li>
 * <li>外键指向Department，转换为Department.getName()，即部门名称。</li>
 * <li>外键指向其他实体，那么，如果实体类中的属性或者Getter方法有Meta标注，其Meta的asCaption为true，<br>
 * 那么转换为这个属性或者Getter；否则，找到实体类中带有"name","title"或者"caption"的属性，将字段转换为这个属性的值。</li>
 * </ul>
 *
 */
public class ForeignKeyJsonSerializer extends JsonSerializer<Object> {
  // private static Logger logger = LoggerFactory.getLogger(ForeignKeyJsonSerializer.class);

  @Override
  public void serialize(Object foreignKey, JsonGenerator jsonGen, SerializerProvider provider) throws IOException, JsonProcessingException {
    if (foreignKey == null) {
      jsonGen.writeString(StringUtils.EMPTY);
      return;
    }

    String caption = "";
    if (foreignKey instanceof User) {
      User user = (User) foreignKey;
      caption = user.getUsername();
    } else if (foreignKey instanceof Department) {
      Department dept = (Department) foreignKey;
      caption = dept.getName();
    } else {
      Field[] fields = foreignKey.getClass().getDeclaredFields();
      // 从标注了@Meta(caption = true)的的field中找到
      CaptionFounder  captionFounder = fetchCaptionFromField(fields, foreignKey);
      
      // 从标注了@Meta(caption = true)的的method中找到
      if (!captionFounder.isFieldFound()) {
        captionFounder = fetchCaptionFromMethod(foreignKey);
      }
      
      // 根据字段名猜测
      if (!captionFounder.isFieldFound()) {
        captionFounder = guessCaption(fields, foreignKey);
      }
      
      caption = captionFounder.getCaption();
    }
    jsonGen.writeString(caption);
  }
  
  private CaptionFounder fetchCaptionFromField(Field[] fields, Object foreignKey) {
    CaptionFounder  captionFounder = new CaptionFounder();
    
    for (Field field : fields) {
      Meta meta = field.getAnnotation(Meta.class);
      if (meta != null && meta.asCaption()) {
        ReflectionUtils.makeAccessible(field);
        Object value = ReflectionUtils.getField(field, foreignKey);
        if (value != null) {
          captionFounder.setCaption(value.toString());
          captionFounder.setFieldFound(true);
          break;
        }
      }
    }
    return captionFounder;
  }
  
  
  private CaptionFounder fetchCaptionFromMethod(Object foreignKey) {
    CaptionFounder  captionFounder = new CaptionFounder();
    
    PropertyDescriptor[] propertyDescs = BeanUtils.getPropertyDescriptors(foreignKey.getClass());
    for (PropertyDescriptor propertyDesc : propertyDescs) {
      Method method = propertyDesc.getReadMethod();
      Meta meta = method.getAnnotation(Meta.class);
      if (meta != null && meta.asCaption()) {
        Object value = ReflectionUtils.invokeMethod(method, foreignKey);
        if (value != null) {
          captionFounder.setCaption(value.toString());
          captionFounder.setFieldFound(true);
          break;
        }
      }
    }
    
    return captionFounder;
  }
  
  private CaptionFounder guessCaption(Field[] fields, Object foreignKey) {
    CaptionFounder  captionFounder = new CaptionFounder();
    
    for (Field field : fields) {
      if (Objects.equals(field.getType(), String.class)
          && (StringUtils.equalsIgnoreCase(field.getName(), "title") 
              || StringUtils.equalsIgnoreCase(field.getName(), "caption") 
              || field.getName().toLowerCase().indexOf("name") >= 0)) {
        ReflectionUtils.makeAccessible(field);
        captionFounder.setCaption((String) ReflectionUtils.getField(field, foreignKey));
        captionFounder.setFieldFound(true);
      }
    }
    
    return captionFounder;
  }
  
  private static class CaptionFounder {
    private String caption = StringUtils.EMPTY;
    private boolean fieldFound = false;
    
    public CaptionFounder() {
    }
    
    public String getCaption() {
      return caption;
    }
    
    public void setCaption(String caption) {
      this.caption = caption;
    }
    
    public boolean isFieldFound() {
      return fieldFound;
    }
    
    public void setFieldFound(boolean fieldFound) {
      this.fieldFound = fieldFound;
    }
  }

}
