package com.github.catstiger.anything.designer.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.code.util.CodeHelper;
import com.github.catstiger.common.sql.BaseEntity;
import com.github.catstiger.common.sql.filter.DataType;
import com.github.catstiger.common.sql.filter.Operator;

@SuppressWarnings("serial")
@Entity
@Table(name = "any_data_model_field")
public class AnyDataModelField extends BaseEntity {
  private String fieldName;
  private String displayName;
  private String dataType;
  private String dataTypeName;
  private Integer dataLength;
  private Integer dataScale;
  private Boolean nullable = true;
  private Integer orders = 0;
  private Integer colWidth;
  private Boolean hidden = false;
  private Boolean isPrimary = false;
  private Boolean fixed = false;
  private Boolean isForeign = false;
  private String refType;
  private AnyDataModel dataModel = new AnyDataModel();
  private AnyDataModel refDataModel;
  private AnyDataModelField refField;
  private AnyDataModelField refFieldDisplay;
  private AnyDataModel reverseDataModel;
  private AnyDataModelField reverseField;
  private String inputType;
  private String jscode;
  private Boolean isSys = false;
  private Boolean isFile = false;
  private Boolean inGrid = true;
  private Boolean inForm = true;
  private Boolean inQuery = false;
  private Date createTime;
  private transient Object value;
  /**
   * 外键指向的其他表的表名，通常是USERS表和DEPTS表
   */
  private String refTableName;

  /**
   * 外键指向USERS或者DEPTS的时候，显示字段名
   */
  private String refDisplayCol;

  /**
   * 外键指向USERS或者DEPTS的时候，引用字段名，通常是ID
   */
  private String refCol;
  /**
   * 在“treegrid”界面中，作为tree列
   */
  private Boolean asTreeCol = false;
  /**
   * 在“树+表格”界面中，作为数据源
   */
  private Boolean asTreeDs = false;

  /**
   * 字段名
   */
  @Column(name = "field_name", length = 64)
  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  /**
   * 显示名
   */
  @Column(name = "display_name", nullable = false, length = 64)
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * 数据类型
   */
  @Column(name = "data_type", nullable = false, length = 30)
  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  /**
   * 数据类型名称
   */
  @Column(name = "data_type_name", nullable = false, length = 50)
  public String getDataTypeName() {
    return dataTypeName;
  }

  public void setDataTypeName(String dataTypeName) {
    this.dataTypeName = dataTypeName;
  }

  /**
   * 长度
   */
  @Column(name = "data_length")
  public Integer getDataLength() {
    return dataLength;
  }

  public void setDataLength(Integer dataLength) {
    this.dataLength = dataLength;
  }

  /**
   * 小数位
   */
  @Column(name = "data_scale")
  public Integer getDataScale() {
    return dataScale;
  }

  public void setDataScale(Integer dataScale) {
    this.dataScale = dataScale;
  }

  /**
   * 是否可以为null
   */
  public Boolean getNullable() {
    return nullable;
  }

  public void setNullable(Boolean nullable) {
    this.nullable = nullable;
  }

  /**
   * 顺序
   */
  public Integer getOrders() {
    return orders;
  }

  public void setOrders(Integer orders) {
    this.orders = orders;
  }

  /**
   * 是否隐藏
   */
  public Boolean getHidden() {
    return hidden;
  }

  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  /**
   * 是否主键
   */
  @Column(name = "is_primary")
  public Boolean getIsPrimary() {
    return isPrimary;
  }

  public void setIsPrimary(Boolean isPrimary) {
    this.isPrimary = isPrimary;
  }

  /**
   * 是否固定字段
   */
  public Boolean getFixed() {
    return fixed;
  }

  public void setFixed(Boolean fixed) {
    this.fixed = fixed;
  }

  /**
   * 是否外键
   */
  @Column(name = "is_foreign")
  public Boolean getIsForeign() {
    return isForeign;
  }

  public void setIsForeign(Boolean isForeign) {
    this.isForeign = isForeign;
  }

  /**
   * 外键引用类型（备用）
   */
  @Column(name = "ref_type")
  public String getRefType() {
    return refType;
  }

  public void setRefType(String refType) {
    this.refType = refType;
  }

  /**
   * 所属数据模型
   */
  @ManyToOne
  @JoinColumn(name = "data_model_id")
  public AnyDataModel getDataModel() {
    return dataModel;
  }

  public void setDataModel(AnyDataModel dataModel) {
    this.dataModel = dataModel;
  }

  /**
   * 引用数据模型
   */
  @ManyToOne
  @JoinColumn(name = "ref_data_model_id")
  public AnyDataModel getRefDataModel() {
    return refDataModel;
  }

  public void setRefDataModel(AnyDataModel refDataModel) {
    this.refDataModel = refDataModel;
  }

  /**
   * 引用字段
   */
  @ManyToOne
  @JoinColumn(name = "ref_field_id")
  public AnyDataModelField getRefField() {
    return refField;
  }

  public void setRefField(AnyDataModelField refField) {
    this.refField = refField;
  }

  /**
   * 引用字段对应的显示字段
   */
  @ManyToOne
  @JoinColumn(name = "ref_field_display_id")
  public AnyDataModelField getRefFieldDisplay() {
    return refFieldDisplay;
  }

  public void setRefFieldDisplay(AnyDataModelField refFieldDisplay) {
    this.refFieldDisplay = refFieldDisplay;
  }

  /**
   * 输入模式
   */
  @Column(length = 30)
  public String getInputType() {
    return inputType;
  }

  public void setInputType(String inputType) {
    this.inputType = inputType;
  }

  /**
   * 源代码
   */
  @Lob
  @Column
  public String getJscode() {
    return jscode;
  }

  public void setJscode(String jscode) {
    this.jscode = jscode;
  }

  /**
   * 是否文件上传
   */
  @Column(name = "is_file")
  public Boolean getIsFile() {
    return isFile;
  }

  public void setIsFile(Boolean isFile) {
    this.isFile = isFile;
  }

  public Boolean getInGrid() {
    return inGrid;
  }

  public void setInGrid(Boolean inGrid) {
    this.inGrid = inGrid;
  }

  public Boolean getInForm() {
    return inForm;
  }

  public void setInForm(Boolean inForm) {
    this.inForm = inForm;
  }

  public Boolean getInQuery() {
    return inQuery;
  }

  public void setInQuery(Boolean inQuery) {
    this.inQuery = inQuery;
  }

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Boolean getIsSys() {
    return isSys;
  }

  public void setIsSys(Boolean isSys) {
    this.isSys = isSys;
  }

  public Boolean getAsTreeCol() {
    return asTreeCol;
  }

  public void setAsTreeCol(Boolean asTreeCol) {
    this.asTreeCol = asTreeCol;
  }

  public Boolean getAsTreeDs() {
    return asTreeDs;
  }

  public void setAsTreeDs(Boolean asTreeDs) {
    this.asTreeDs = asTreeDs;
  }

  @Column(length = 64)
  public String getRefTableName() {
    return refTableName;
  }

  public void setRefTableName(String refTableName) {
    this.refTableName = refTableName;
  }

  @Column(length = 64)
  public String getRefDisplayCol() {
    return refDisplayCol;
  }

  public void setRefDisplayCol(String refDisplayCol) {
    this.refDisplayCol = refDisplayCol;
  }

  @Column(length = 64)
  public String getRefCol() {
    return refCol;
  }

  /**
   * 在表格中所占列宽
   * 
   * @return
   */
  public Integer getColWidth() {
    return colWidth;
  }

  public void setColWidth(Integer colWidth) {
    this.colWidth = colWidth;
  }

  public void setRefCol(String refCol) {
    this.refCol = refCol;
  }

  /**
   * 一对多字段，引用的AnyDataModel
   */
  @ManyToOne
  @JoinColumn(name = "reverse_data_model_id")
  public AnyDataModel getReverseDataModel() {
    return reverseDataModel;
  }

  public void setReverseDataModel(AnyDataModel reverseDataModel) {
    this.reverseDataModel = reverseDataModel;
  }

  /**
   * 一对多字段，引用的AnyDataModelField
   * 
   * @return
   */
  @ManyToOne
  @JoinColumn(name = "reverse_field_id")
  public AnyDataModelField getReverseField() {
    return reverseField;
  }

  public void setReverseField(AnyDataModelField reverseField) {
    this.reverseField = reverseField;
  }

  @Transient
  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AnyDataModelField other = (AnyDataModelField) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }

    return true;
  }

  /**
   * 列名转换为属性名
   * 
   * @return
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public String getPropertyName() {
    return StringUtils.isNotBlank(fieldName) ? CodeHelper.getPropertyName(fieldName, isForeign) : StringUtils.EMPTY;
  }

  /**
   * 返回Field对应类型的短类名
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public String getJavaShortName() {
    Class<?> type = CodeHelper.getJavaType(this, false);
    if (type == null) {
      if (getIsForeign() && getRefDataModel() != null) {
        return getRefDataModel().getClassName();
      } else if (getIsForeign() && StringUtils.isNotBlank(getRefTableName())) {
        return CodeHelper.getClassName(getRefTableName());
      } else {
        return Void.class.getSimpleName();
      }
    } else {
      return type.getSimpleName();
    }
  }

  /**
   * 返回Field对应JS的类型，缺省为‘auto’
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public String getJsType() {
    if (AnyDataModelConstants.JS_TYPES.containsKey(getJavaType())) {
      return AnyDataModelConstants.JS_TYPES.get(getJavaType());
    }
    return "auto";
  }

  /**
   * 返回数字类型的最大值
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public Object getMaxValue() {
    if (StringUtils.equals(dataType, AnyDataModelConstants.DATA_TYPE_NUMBER)) {
      Double maxVal = Double.MAX_VALUE;
      if (dataLength != null && dataScale != null) {
        if (dataLength > 0 && dataScale >= 0 && dataLength - dataScale > 0) {
          maxVal = Math.pow(10, dataLength - dataScale) - 1;
        }
      }
      if (dataLength != null && dataScale != null) {
        if (dataLength > 0) {
          maxVal = Math.pow(10, dataLength) - 1;
        }
      }
      return maxVal;
    }
    return null;
  }

  /**
   * 在外键情况下，子表不但需要主表的ID，还需要主表的一个字段用于显示“人类可以看懂的内容（ID只是数字而已）”， <code>getRefDisplayPropertyName</code>返回这个字段在主表中的属性名称。
   * 
   * @return
   */
  @Transient
  // @JsonIgnore @JSONField(serialize = false)
  public String getRefDisplayPropertyName() {
    if (!isForeign) {
      return null;
    }
    if (getRefDataModel() != null && StringUtils.isNotBlank(this.getRefFieldDisplay().getFieldName())) {
      return CodeHelper.getPropertyName(this.getRefFieldDisplay().getFieldName(), false);
    }

    if (StringUtils.isNotBlank(getRefTableName()) && StringUtils.isNotBlank(getRefDisplayCol())) {
      return CodeHelper.getPropertyName(getRefDisplayCol(), false);
    }
    return null;
  }

  /**
   * 返回符合DynaSpec规范的名字
   */
  @Transient
  public String getDynaSpecName() {
    if (AnyDataModelConstants.XTYPE_MULTIFILEUPLOAD.equals(inputType) || AnyDataModelConstants.XTYPE_ONETOMANY.equals(inputType)) {
      return StringUtils.EMPTY;
    }

    StringBuilder specName = new StringBuilder(20).append("Q_").append(getPropertyName());
    if (getIsForeign()) {
      specName.append(".id");
    }
    specName.append("_");
    if (StringUtils.equals(dataType, AnyDataModelConstants.DATA_TYPE_VERCHAR)) {
      specName.append(DataType.S).append("_").append(Operator.LK);
    } else if (StringUtils.equals(dataType, AnyDataModelConstants.DATA_TYPE_NUMBER)) {
      doDynaSpecNumber(specName);
    } else if (StringUtils.equals(dataType, AnyDataModelConstants.DATA_TYPE_DATE)) {
      specName.append(DataType.D).append("_").append(Operator.EQ);
    } else if (StringUtils.equals(dataType, AnyDataModelConstants.DATA_TYPE_BOOL)) {
      specName.append(DataType.B).append("_").append(Operator.EQ);
    }
    return specName.toString();
  }

  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public Class<?> getJavaType() {
    return CodeHelper.getJavaType(this, true);
  }

  /**
   * 返回Field对应的Ext组件的jsCode（自定义字段特征），如果没有返回{}
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public String getJs() {
    if (StringUtils.isBlank(getJscode())) {
      return null;
    }
    String js = StringUtils.trim(getJscode());
    if (js.startsWith("{")) {
      js = js.substring(1);
    }
    if (js.endsWith("}")) {
      js = js.substring(0, js.length() - 1);
    }
    js = StringUtils.trim(js);
    if (js.endsWith(",")) {
      js = js.substring(0, js.length() - 1);
    }
    return js;
  }

  /**
   * 返回引用表的表名，如果不是外键，则返回null,如果引用的是AnyDataModel，则返回对应的表名。 如果引用的十一个表，则直接返回表名。
   * 
   * @return
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public String getRefTable() {
    if (!getIsForeign()) {
      return null;
    }
    if (getRefDataModel() != null) {
      return getRefDataModel().getRealTableName();
    }
    if (StringUtils.isNotBlank(getRefTableName())) {
      return getRefTableName();
    }
    return null;
  }

  /**
   * 返回外键显示字段名称
   * 
   * @return
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public String getRefDispFieldName() {
    if (!getIsForeign()) {
      return null;
    }
    if (getRefFieldDisplay() != null) {
      return getRefFieldDisplay().getFieldName();
    }
    if (StringUtils.isNotBlank(getRefDisplayCol())) {
      return getRefDisplayCol();
    }
    return null;
  }
  

  private void doDynaSpecNumber(StringBuilder specName) {
    if (dataScale != null && dataScale > 0) {
      specName.append(DataType.BD).append("_").append(Operator.EQ);
    } else {
      if (getIsForeign()) { // 整数，外键作为Long
        specName.append(DataType.L).append("_").append(Operator.EQ);
      } else { // 整数，普通字段作为Integer
        specName.append(DataType.N).append("_").append(Operator.EQ);
      }
    }
  }
}
