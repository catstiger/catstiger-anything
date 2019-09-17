package com.github.catstiger.anything.meta;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import com.github.catstiger.anything.annotation.Meta;
import com.github.catstiger.common.sql.naming.SnakeCaseNamingStrategy;
import com.github.catstiger.common.util.Exceptions;
import com.google.common.base.Preconditions;

@Service
public class MetaService {
  private static Logger logger = LoggerFactory.getLogger(MetaService.class);
  public static final String ID_PREFIX = "field-";
  private static List<Map<String, Object>> MODELS = new CopyOnWriteArrayList<>();
  private static final ConcurrentMap<String, Class<?>> ENTITIES = new ConcurrentHashMap<>(100);
  @Value(value = "classpath*:com/github/catstiger/**/model/*.class")
  private org.springframework.core.io.Resource[] classResources;

  /**
   * 搜索所有的Model类，根据@Meta标注，提取其特征，用作表单绑定等。
   */
  @PostConstruct
  public void initModels() {
    // 加载所有Action类
    String classRoot = getClass().getResource("/").getPath();
    for (Resource classResource : classResources) {
      String classFile;
      try {
        classFile = classResource.getURL().getPath();
      } catch (IOException e) {
        continue;
      }
      String className = StringUtils.replace(classFile, classRoot, "");
      className = StringUtils.replace(className, "/", ".");
      className = StringUtils.replace(className, ".class", "");
      Class<?> modelCls = null;
      try {
        modelCls = ClassUtils.forName(className, getClass().getClassLoader()); // ReflectUtil.classForName(className);
      } catch (Exception e) {
        logger.error("{} Not found!", className);
        continue;
      }
      Entity entity = (Entity) modelCls.getAnnotation(Entity.class);
      if (entity == null) {
        continue;
      }
      ENTITIES.put(modelCls.getSimpleName(), modelCls); //记录SimpleName和Class的关系
      
      Meta meta = (Meta) modelCls.getAnnotation(Meta.class);
      if (meta == null || !meta.visible()) {
        continue;
      }

      Map<String, Object> map = new HashMap<String, Object>();
      if (StringUtils.isBlank(meta.view())) {
        map.put("id", new StringBuilder(100).append("JTiger.modules.").append(modelCls.getSimpleName().toLowerCase()).append(".")
            .append(modelCls.getSimpleName()).append("Form").toString());
      } else {
        map.put("id", meta.view());
      }
      map.put("text", StringUtils.isBlank(meta.description()) ? meta.value() : meta.description());
      map.put("className", modelCls);
      map.put("iconCls", "icon-model");
      map.put("leaf", true);

      if (findClassByFormKey((String) map.get("id")) == null) {
        MODELS.add(map);
      }
    }
  }
  
  /**
   * 根据短类名，获取实体类Class对象
   * @param simpleClassName 短类名，不可为{@code null}
   * @return 实体类Class对象
   * @throws RuntimeException 如果短类名所代表的类不存在
   */
  public Class<?> getEntityBySimpleName(String simpleClassName) {    
    Preconditions.checkNotNull(simpleClassName, "类" + simpleClassName + "不存在");
    if (ENTITIES.containsKey(simpleClassName)) {
      return ENTITIES.get(simpleClassName);
    } else {
      throw Exceptions.unchecked("类" + simpleClassName + "不存在！");
    }
  }
  
  /**
   * 根据实体类的短类名，获取实体类对应的表名，如果实体类被Table标注，并且规定了表名，则取Table规定的表名。
   * 如果没有，则按照snake_case命名规则获取表名。
   * @param simpleClassName 短类名，不可为{@code null}
   * @return 表名
   * @throws RuntimeException 如果短类名所代表的类不存在
   */
  public String getTablenameBySimpleName(String simpleClassName) {
    Class<?> entityClass = getEntityBySimpleName(simpleClassName);
    return new SnakeCaseNamingStrategy().tablename(entityClass);
  }

  /**
   * 加载所有标注了@Entity和@Meta的实体类，将类名作为id，description作为text。
   */
  public List<Map<String, Object>> loadModels() {
    if (MODELS.isEmpty()) {
      initModels();
    }
    return MODELS;
  }

  /**
   * 根据FormKey（Form表单js类），找到对应的Model类
   * 
   * @return 如果找不到，返回null
   */
  public Class<?> findClassByFormKey(String formKey) {
    if (MODELS.isEmpty()) {
      return null;
    }
    for (Map<String, Object> model : MODELS) {
      if (model.get("id").equals(formKey)) {
        Class<?> clazz = (Class<?>) model.get("className");
        return clazz;
      }
    }
    return null;
  }

  /**
   * 根据Model Class, 列出所有被@Meta标注的字段
   */
  public List<FieldDescriptor> getFieldsByModel(Class<?> modelClass) {
    if (modelClass == null) {
      return null;
    }
    List<FieldDescriptor> modelFields = new ArrayList<FieldDescriptor>(30);
    Field[] fields = modelClass.getDeclaredFields();
    for (Field field : fields) {
      Meta meta = field.getAnnotation(Meta.class);
      if (meta == null || !meta.visible()) {
        continue;
      }
      FieldDescriptor fieldDesc = new FieldDescriptor();
      if (StringUtils.isBlank(meta.view())) {
        fieldDesc.setId(ID_PREFIX + modelClass.getSimpleName().toUpperCase() + "-" + field.getName());
        fieldDesc.setFieldName(field.getName());
      } else {
        fieldDesc.setId(meta.view());
        fieldDesc.setFieldName(meta.view().split("-")[2]);
      }
      fieldDesc.setDisplayName(StringUtils.isBlank(meta.description()) ? meta.value() : meta.description());
      fieldDesc.setDataType(field.getType().getSimpleName());
      fieldDesc.setField(field);
      modelFields.add(fieldDesc);
    }
    return modelFields;
  }
}
