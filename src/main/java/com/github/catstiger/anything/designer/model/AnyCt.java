package com.github.catstiger.anything.designer.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.catstiger.common.sql.BaseEntity;

/**
 * 用于表达Form表单中的一个Container
 * 
 * @author lizhenshan
 *
 */
@Entity
@Table(name = "any_ct")
public class AnyCt extends BaseEntity {
  private static final long serialVersionUID = -6642756165078717766L;
  /**
   * 容器包含几列组件，contianer采用column布局
   */
  private Integer cols;
  private Integer height;
  private Integer width;
  private Integer orders;
  private String cmpId;

  private AnyDataModel dataModel;
  /**
   * 容器包括的组件
   */
  private List<AnyCmp> anyCmps;

  public Integer getCols() {
    return cols;
  }

  public void setCols(Integer cols) {
    this.cols = cols;
  }

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  public Integer getWidth() {
    return width;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }

  public Integer getOrders() {
    return orders;
  }

  public void setOrders(Integer orders) {
    this.orders = orders;
  }

  @JsonIgnore
  @JSONField(serialize = false)
  @JoinColumn(name = "data_model_id")
  public AnyDataModel getDataModel() {
    return dataModel;
  }

  public void setDataModel(AnyDataModel dataModel) {
    this.dataModel = dataModel;
  }

  @Transient
  public List<AnyCmp> getAnyCmps() {
    return anyCmps;
  }

  public void setAnyCmps(List<AnyCmp> anyCmps) {
    this.anyCmps = anyCmps;
  }

  /**
   * 返回组件的数量 
   */
  @Transient
  public int getSize() {
    if (CollectionUtils.isEmpty(anyCmps)) {
      return 1;
    }

    return anyCmps.size();
  }

  public void setDataModelId(Long dataModelId) {
    this.dataModel = new AnyDataModel();
    dataModel.setId(dataModelId);
  }

  @Column(name = "cmp_id")
  public String getCmpId() {
    return cmpId;
  }

  public void setCmpId(String cmpId) {
    this.cmpId = cmpId;
  }
}
