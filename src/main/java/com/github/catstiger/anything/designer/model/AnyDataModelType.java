package com.github.catstiger.anything.designer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.catstiger.common.sql.BaseEntity;

@SuppressWarnings("serial")
@Entity
@Table(name = "any_data_model_type")
public class AnyDataModelType extends BaseEntity {
  private String name;
  private String descn;
  private Integer orders = 0;
  private String path;
  private String icon;
  private AnyDataModelType parent;
  private List<AnyDataModelType> children = Collections.emptyList();
  private List<AnyDataModel> anyDataModels = Collections.emptyList();
  private transient List<Map<String, Object>> menu = new ArrayList<Map<String, Object>>();

  @Column(length = 50)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Column(length = 255)
  public String getDescn() {
    return descn;
  }

  public void setDescn(String descn) {
    this.descn = descn;
  }

  public Integer getOrders() {
    return orders;
  }

  public void setOrders(Integer orders) {
    this.orders = orders;
  }

  @ManyToOne
  public AnyDataModelType getParent() {
    return parent;
  }

  public void setParent(AnyDataModelType parent) {
    this.parent = parent;
  }

  @Column(name = "path", length = 500)
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @JsonIgnore
  @JSONField(serialize = false)
  @Transient
  public List<AnyDataModelType> getChildren() {
    return children;
  }

  public void setChildren(List<AnyDataModelType> children) {
    this.children = children;
  }

  @JsonIgnore
  @JSONField(serialize = false)
  @Transient
  public List<AnyDataModel> getAnyDataModels() {
    return anyDataModels;
  }

  public void setAnyDataModels(List<AnyDataModel> anyDataModels) {
    this.anyDataModels = anyDataModels;
  }

  @Column(length = 180)
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  @Transient
  public Boolean getHasChild() {
    return CollectionUtils.isNotEmpty(getChildren());
  }

  @Transient
  @JSONField(name = "menu")
  public List<Map<String, Object>> getMenu() {
    return menu;
  }

  public void setMenu(List<Map<String, Object>> menu) {
    this.menu = menu;
  }
  
  /**
   * 返回上级类型的ID，如果没有上级，返回{@code null}
   */
  @Transient
  public Long getParentId() {
    if (this.parent != null) {
      return this.parent.getId();
    }
    return null;
  }
  
  /**
   * 设置升级类型的ID，如果{@link #parent} 不存在，则先创建
   */
  public void setParentId(long parentId) {
    if (this.parent == null) {
      this.parent = new AnyDataModelType();
    }

    this.parent.setId(parentId);
  }

}
