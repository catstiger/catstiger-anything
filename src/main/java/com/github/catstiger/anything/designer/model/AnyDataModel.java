package com.github.catstiger.anything.designer.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.code.util.CodeHelper;
import com.github.catstiger.common.sql.BaseEntity;
import com.github.catstiger.websecure.user.model.User;

@SuppressWarnings("serial")
@Entity
@Table(name = "any_data_model")
public class AnyDataModel extends BaseEntity {
  private String displayName;
  private String tableName;
  private Boolean isActive = true;
  private User createUser;
  private Date createTime;
  private String readRoles;
  private String createRoles;
  private String deleteRoles;
  private String updateRoles;
  private String readRoleNames;
  private String createRoleNames;
  private String deleteRoleNames;
  private String updateRoleNames;
  private Boolean hasTable = false;
  private Boolean hasCode = false;
  private Boolean inMenu = true;

  /**
   * 绑定的流程Key
   */
  private String processKey;

  /**
   * 界面样式：只有表单、表格加表单、树形表格加表单、树+表格
   */
  private String uiStyle;
  /**
   * 表单弹出位置：对话框、从下方滑出、从左侧滑出、从右侧滑出
   */
  private String formPos = AnyDataModelConstants.FORM_POS_WIN;
  /**
   * 表单宽度
   */
  private Integer formWidth = 600;
  /**
   * 表单高度
   */
  private Integer formHeight = 450;
  private Integer orders = 0;
  /**
   * 菜单图标
   */
  private String iconCls;
  private AnyDataModelType type;
  private String descn;
  private Boolean isCompleted = false;
  private List<AnyDataModelField> fields = new ArrayList<AnyDataModelField>(0);
  private List<AnyCt> anyCts;
  /**
   * 模块图标
   */
  private String icon;

  private String packageName;

  private transient List<String> allXtypes;
  /**
   * 可查看
   */
  private Boolean viewable = false;
  /**
   * 可导出
   */
  private Boolean exportable = false;

  /**
   * 可导入
   */
  private Boolean importable = false;
  /**
   * 高级查询
   */
  private Boolean searchable = false;

  /**
   * 权限类别，多种权限用逗号分割
   */
  private String privType;

  /**
   * 权限，如果<code>privType</code>是<code>AnyDataModeConstants#DATA_PRIV_FIELD</code> 那么，privStr是用逗号分割的用户ID
   */
  private String privStr;

  /**
   * 显示名称
   */
  @Column(name = "display_name", length = 64, nullable = false, unique = true)
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * 表名
   */
  @Column(name = "table_name", length = 64, nullable = false, unique = true)
  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  /**
   * 是否激活
   */
  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  /**
   * 创建人
   */
  @ManyToOne
  public User getCreateUser() {
    return createUser;
  }

  public void setCreateUser(User createUser) {
    this.createUser = createUser;
  }

  /**
   * 创建时间
   */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @JSONField(format = "yyyy-MM-dd HH:mm:ss")
  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  /**
   * 列表、查询角色
   */
  @Column(name = "read_roles")
  public String getReadRoles() {
    return readRoles;
  }

  public void setReadRoles(String readRoles) {
    this.readRoles = readRoles;
  }

  /**
   * 创建角色
   */
  @Column(name = "create_roles")
  public String getCreateRoles() {
    return createRoles;
  }

  public void setCreateRoles(String createRoles) {
    this.createRoles = createRoles;
  }

  /**
   * 删除角色
   */
  @Column(name = "delete_roles")
  public String getDeleteRoles() {
    return deleteRoles;
  }

  public void setDeleteRoles(String deleteRoles) {
    this.deleteRoles = deleteRoles;
  }

  /**
   * 更新角色
   */
  @Column(name = "update_roles")
  public String getUpdateRoles() {
    return updateRoles;
  }

  public void setUpdateRoles(String updateRoles) {
    this.updateRoles = updateRoles;
  }

  /**
   * 是否已经生成数据表
   */
  @Column(name = "has_table")
  public Boolean getHasTable() {
    return hasTable;
  }

  public void setHasTable(Boolean hasTable) {
    this.hasTable = hasTable;
  }

  /**
   * 是否已经生成源代码
   */
  @Column(name = "has_code")
  public Boolean getHasCode() {
    return hasCode;
  }

  public void setHasCode(Boolean hasCode) {
    this.hasCode = hasCode;
  }

  @Column(name = "ui_style", length = 30)
  public String getUiStyle() {
    return uiStyle;
  }

  public void setUiStyle(String uiStyle) {
    this.uiStyle = uiStyle;
  }

  /**
   * 排列顺序
   */
  public Integer getOrders() {
    return orders;
  }

  public void setOrders(Integer orders) {
    this.orders = orders;
  }

  /**
   * 图标
   */
  @Column(name = "icon_cls", length = 30)
  public String getIconCls() {
    return iconCls;
  }

  public void setIconCls(String iconCls) {
    this.iconCls = iconCls;
  }

  /**
   * 类别
   */
  @ManyToOne
  public AnyDataModelType getType() {
    return type;
  }

  public void setType(AnyDataModelType type) {
    this.type = type;
  }

  @Column(length = 1000)
  public String getDescn() {
    return descn;
  }

  public void setDescn(String descn) {
    this.descn = descn;
  }

  public Boolean getIsCompleted() {
    return isCompleted;
  }

  @Column(name = "is_completed")
  public void setIsCompleted(Boolean isCompleted) {
    this.isCompleted = isCompleted;
  }

  @Column(name = "read_role_names", length = 500)
  public String getReadRoleNames() {
    return readRoleNames;
  }

  public void setReadRoleNames(String readRoleNames) {
    this.readRoleNames = readRoleNames;
  }

  @Column(name = "create_role_names", length = 500)
  public String getCreateRoleNames() {
    return createRoleNames;
  }

  public void setCreateRoleNames(String createRoleNames) {
    this.createRoleNames = createRoleNames;
  }

  @Column(name = "delete_role_names", length = 500)
  public String getDeleteRoleNames() {
    return deleteRoleNames;
  }

  public void setDeleteRoleNames(String deleteRoleNames) {
    this.deleteRoleNames = deleteRoleNames;
  }

  @Column(name = "update_role_names", length = 500)
  public String getUpdateRoleNames() {
    return updateRoleNames;
  }

  public void setUpdateRoleNames(String updateRoleNames) {
    this.updateRoleNames = updateRoleNames;
  }

  @Transient
  public List<AnyDataModelField> getFields() {
    return fields;
  }

  public void setFields(List<AnyDataModelField> fields) {
    this.fields = fields;
  }

  @Transient
  public List<AnyCt> getAnyCts() {
    return anyCts;
  }

  public void setAnyCts(List<AnyCt> anyCts) {
    this.anyCts = anyCts;
  }

  @Column(length = 30)
  public String getFormPos() {
    return formPos;
  }

  public void setFormPos(String formPos) {
    this.formPos = formPos;
  }

  public Integer getFormWidth() {
    return formWidth;
  }

  public void setFormWidth(Integer formWidth) {
    this.formWidth = formWidth;
  }

  public Integer getFormHeight() {
    return formHeight;
  }

  public void setFormHeight(Integer formHeight) {
    this.formHeight = formHeight;
  }

  @Column(length = 120)
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  @Column(length = 128)
  public String getProcessKey() {
    return processKey;
  }

  public void setProcessKey(String processKey) {
    this.processKey = processKey;
  }

  public Boolean getInMenu() {
    return inMenu;
  }

  public void setInMenu(Boolean inMenu) {
    this.inMenu = inMenu;
  }

  @Column(length = 180)
  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public Boolean getViewable() {
    return viewable;
  }

  public void setViewable(Boolean viewable) {
    this.viewable = viewable;
  }

  public Boolean getExportable() {
    return exportable;
  }

  public void setExportable(Boolean exportable) {
    this.exportable = exportable;
  }

  public Boolean getImportable() {
    return importable;
  }

  public void setImportable(Boolean importable) {
    this.importable = importable;
  }

  public Boolean getSearchable() {
    return searchable;
  }

  public void setSearchable(Boolean searchable) {
    this.searchable = searchable;
  }

  @Column(length = 255)
  public String getPrivType() {
    return privType;
  }

  public void setPrivType(String privType) {
    this.privType = privType;
  }

  @Column(length = 255)
  public String getPrivStr() {
    return privStr;
  }

  public void setPrivStr(String privStr) {
    this.privStr = privStr;
  }

  /**
   * 数据库真实表名
   */
  @Transient
  public String getRealTableName() {
    return AnyDataModelConstants.getReadTablename(tableName);
  }

  /**
   * 将AnyDataModel的tableName，转换为类名
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public String getClassName() {
    return CodeHelper.getClassName(tableName);
  }

  /**
   * 将表名转换为变量名
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public String getVarName() {
    return CodeHelper.getVarName(tableName);
  }

  /**
   * 是否有外键
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public boolean getHasForeignKey() {
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        if (field.getIsForeign() && (field.getRefDataModel() != null || field.getRefTableName() != null)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 是否有日期类型字段
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public boolean getHasDate() {
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        if (StringUtils.equals(field.getDataType(), AnyDataModelConstants.DATA_TYPE_DATE)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 是否有日期类型字段
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public boolean getHasFile() {
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        if (StringUtils.equals(field.getInputType(), AnyDataModelConstants.XTYPE_FILEUPLOAD)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 得到所有fields对应的Java类
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public Set<Class<?>> getFieldTypes() {
    Set<Class<?>> fieldTypes = new HashSet<Class<?>>();
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        fieldTypes.add(field.getJavaType());
      }
    }
    return fieldTypes;
  }

  /**
   * 判断AnyDataModel是否是一个树形结构
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public boolean getIsRecursive() {
    if (!StringUtils.equals(getUiStyle(), AnyDataModelConstants.UI_STYLE_TREEGRID_FORM)) {
      return false;
    }
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        if (field.getIsForeign() && field.getRefDataModel() != null && this.equals(field.getRefDataModel()) && field.getAsTreeCol()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 返回指向自身的外键，如果没有则返回null
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public AnyDataModelField getRecursiveField() {
    if (!StringUtils.equals(getUiStyle(), AnyDataModelConstants.UI_STYLE_TREEGRID_FORM)) {
      return null;
    }
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        if (field.getIsForeign() && field.getRefDataModel() != null && this.equals(field.getRefDataModel()) && field.getAsTreeCol()) {
          return field;
        }
      }
    }
    return null;
  }

  /**
   * 得到显示在表格中的AnyDataModelField
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public List<AnyDataModelField> getGridFields() {
    List<AnyDataModelField> inGridFields = new ArrayList<AnyDataModelField>();
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        if (field.getInGrid() && !StringUtils.equals(field.getInputType(), AnyDataModelConstants.XTYPE_HIDDEN)) {
          inGridFields.add(field);
        }
      }
    }
    return inGridFields;
  }

  /**
   * 得到作为查询条件的字段
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public List<AnyDataModelField> getQueryFields() {
    List<AnyDataModelField> queryFields = new ArrayList<AnyDataModelField>();
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        if (field.getInQuery() && getSearchableFields().contains(field)) {
          queryFields.add(field);
        }
      }
    }

    return queryFields;
  }

  /**
   * 是否有多文件上传字段
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public Boolean getHasMultiFile() {
    Boolean hasMf = false;
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        if (AnyDataModelConstants.XTYPE_MULTIFILEUPLOAD.equals(field.getInputType())) {
          hasMf = true;
          break;
        }
      }
    }

    return hasMf;
  }

  /**
   * 得到多文件上传字段
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public AnyDataModelField getMultiUploadField() {
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        if (AnyDataModelConstants.XTYPE_MULTIFILEUPLOAD.equals(field.getInputType())) {
          return field;
        }
      }
    }
    return null;
  }

  /**
   * 是否有一对多字段
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public Boolean getHasOneToMany() {
    Boolean hasO2M = false;
    if (CollectionUtils.isNotEmpty(fields)) {
      for (AnyDataModelField field : fields) {
        if (AnyDataModelConstants.XTYPE_ONETOMANY.equals(field.getInputType())) {
          hasO2M = true;
          break;
        }
      }
    }

    return hasO2M;
  }

  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public boolean getHasQuery() {
    return CollectionUtils.isNotEmpty(getQueryFields());
  }

  /**
   * 如果是border_form类型的界面，返回用作左侧列表的字段（外键）
   * 
   * @return
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public AnyDataModelField getWestField() {
    if (StringUtils.equals(AnyDataModelConstants.UI_STYLE_BORDER_FORM, getUiStyle())) {
      if (CollectionUtils.isNotEmpty(fields)) {
        for (AnyDataModelField field : fields) {
          if (field.getIsForeign() && field.getAsTreeDs()) {
            return field;
          }
        }
      }
    }
    return null;
  }

  /**
   * 返回对应的JS类
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public String getJsView() {
    if (!this.getHasCode()) {
      return null;
    }
    if (AnyDataModelConstants.UI_STYLE_FORM.equals(getUiStyle())) {
      return new StringBuilder(100).append("JTiger.form.").append(getClassName()).append("Form").toString();
    } else {
      return new StringBuilder(100).append("JTiger.view.").append(getClassName()).append("View").toString();
    }
  }

  @Transient
  public List<String> getAllXtypes() {
    return allXtypes;
  }

  public void setAllXtypes(List<String> allXtypes) {
    this.allXtypes = allXtypes;
  }

  /**
   * 所有可以用作查询条件的字段
   */
  private List<AnyDataModelField> searchableFields = new ArrayList<AnyDataModelField>();

  /**
   * 返回可以作为搜索条件的字段
   */
  @Transient
  @JsonIgnore
  @JSONField(serialize = false)
  public List<AnyDataModelField> getSearchableFields() {
    if (CollectionUtils.isEmpty(searchableFields)) {
      for (AnyDataModelField field : getFields()) {
        if (isSearchable(field)) {
          searchableFields.add(field);
        }
      }
    }

    return searchableFields;
  }
  
  private boolean isSearchable(AnyDataModelField field) {
    StringBuilder types = new StringBuilder(100).append(",")
        .append(AnyDataModelConstants.DATA_TYPE_CLOB).append(",")
        .append(AnyDataModelConstants.XTYPE_FILEUPLOAD).append(",")
        .append(AnyDataModelConstants.XTYPE_KINDEDITOR).append(",")
        .append(AnyDataModelConstants.XTYPE_HIDDEN).append(",")
        .append(AnyDataModelConstants.XTYPE_ONETOMANY).append(",")
        .append(AnyDataModelConstants.XTYPE_MULTIFILEUPLOAD).append(",");
    
    return (types.toString().indexOf(field.getInputType()) < 0
        && !(field.getAsTreeDs() && AnyDataModelConstants.UI_STYLE_BORDER_FORM.equals(getUiStyle())));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof AnyDataModel)) {
      return false;
    }
    return Objects.equals(((AnyDataModel) obj).getId(), this.getId());
  }
  
  @Override
  public int hashCode() {
    return this.getId().hashCode();
  }

  @Transient
  public Boolean getHasProcess() {
    return StringUtils.isNotBlank(processKey);
  }

  /**
   * 返回对应的模块类型ID
   */
  @Transient
  public Long getTypeId() {
    if (this.type != null) {
      return this.type.getId();
    }
    return null;
  }

  /**
   * 设置字段类型ID
   */
  public void setTypeId(Long id) {
    if (this.type == null) {
      this.type = new AnyDataModelType();
    }

    this.type.setId(id);
  }

}
