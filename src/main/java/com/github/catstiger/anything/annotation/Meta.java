package com.github.catstiger.anything.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 用于描述一个类和类的属性，包括描述信息和对应的View层的信息.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface Meta {
  /**
   * 实体类描述，或者字段描述.
   */
  @AliasFor("value")
  String description() default "";
  
  /**
   * 实体类描述，或者字段描述
   * @return
   */
  @AliasFor("description")
  String value() default "";
  /**
   * 对应的View，对于字段，这个值是字段对应的输入框在表单中的ID；
   * 对于模块，这个值是模块对应的表单类的js类名
   */
  String view() default "";
  
  /**
   * 是否可见
   */
  boolean visible() default true;
  
  /**
   * 模块对应的freemarker模板，缺省为对应的类名(标注Class的时候使用)
   */
  String tpl() default "";
  
  /**
   * 是否作为标题，如果作为标题，那么数据转换的时候，将所在实体的ID转换为这个Field的
   */
  boolean asCaption() default false;
}
