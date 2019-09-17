package com.github.catstiger.anything.designer.model;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.catstiger.common.sql.BaseEntity;

/**
 * 用于代表表单上的组件
 * @author lizhenshan
 *
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "any_cmp")
public class AnyCmp extends BaseEntity {
  private AnyCt anyCt;
  private Integer orders;
  private AnyDataModelField field;
  private AnyDataModelField simpleField;
  /**
   * 界面元素ID
   */
  private String cmpId; 

  /**
   * 排列顺序
   */
  public Integer getOrders() {
    return orders;
  }

  public void setOrders(Integer orders) {
    this.orders = orders;
  }

  @ManyToOne
  @JoinColumn(name = "any_ct_id")
  @JsonIgnore 
  @JSONField(serialize = false)
  public AnyCt getAnyCt() {
    return anyCt;
  }

  public void setAnyCt(AnyCt anyCt) {
    this.anyCt = anyCt;
  }

  @ManyToOne
  @JoinColumn(name = "data_model_field_id")
  @JsonIgnore 
  @JSONField(serialize = false)
  public AnyDataModelField getField() {
    return field;
  }

  public void setField(AnyDataModelField field) {
    this.field = field;
  }
  
  /**
   * 返回一个与Session脱离的AnyDataModelField对象，防止json循环解析
   */
  @Transient
  public AnyDataModelField getSimpleField() {
    return simpleField;
  }
  
  /**
   * @param simpleField the simpleField to set
   */
  public void setSimpleField(AnyDataModelField simpleField) {
    this.simpleField = simpleField;
  }
  
  @Transient
  public Long getFieldId() {
    return field != null ? field.getId() : null;
  }
  
  public void setFieldId(Long fieldId) {
    this.field = new AnyDataModelField();
    this.field.setId(fieldId);
  }

  public String getCmpId() {
    return cmpId;
  }

  public void setCmpId(String cmpId) {
    this.cmpId = cmpId;
  }

}
