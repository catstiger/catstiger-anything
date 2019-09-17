package com.github.catstiger.anything.designer.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.ddl.AnyDDL;
import com.github.catstiger.anything.designer.model.AnyCmp;
import com.github.catstiger.anything.designer.model.AnyCt;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.anything.designer.model.AnyDataModelType;
import com.github.catstiger.anything.oper.service.AnySql;
import com.github.catstiger.common.service.GenericsService;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.BeanPropertyRowMapperEx;
import com.github.catstiger.websecure.user.model.Role;
import com.github.catstiger.websecure.user.model.User;
import com.github.catstiger.websecure.user.service.UserService;
import com.google.common.base.Preconditions;

@Service
public class AnyDataModelService extends GenericsService<AnyDataModel> {
  @Resource
  private AnyDDL anyDDL;
  @Resource
  private AnySql anySql;
  @Resource
  private AnyDataModelFieldService fieldService;
  @Autowired
  private UserService userService;
 
  /**
   * 创建或者更新AnyDataModel
   * @param entity 给出AnyDataModel的属性
   * @param createUser 创建用户
   * @return
   */
  @Transactional
  public AnyDataModel merge(AnyDataModel entity, User createUser) {
    entity.setCreateUser(createUser);
    entity.setCreateTime(DateTime.now().toDate());

    Boolean isNew = false;
    if (entity.getId() == null) { // 新增模块
      entity.setIsActive(true);
      entity.setHasCode(false);
      entity.setHasTable(false);
      entity.setOrders(0);
      isNew = true;
    }
    
    fixPrivileges(entity); // 修正空白的角色，设置为null
    defaultValues(entity); // 缺省的UI设置
    
    entity.setTableName(entity.getTableName().toLowerCase()); // 表名都转换为小写
    entity = super.merge(entity);
    // 如果是树形列表布局，要求必须指定一个树形字段，否则还原为普通列表布局
    if (StringUtils.equals(entity.getUiStyle(), AnyDataModelConstants.UI_STYLE_TREEGRID_FORM)) {
      Long treeCols = jdbcTemplate.queryForObject(
          "select count(*) from any_data_model_field f where f.data_model_id=? and f.as_tree_col=true", Long.class, entity.getId());
      if (treeCols == 0) {
        jdbcTemplate.update("update any_data_model set ui_style=? where id=?", AnyDataModelConstants.UI_STYLE_GRID_FORM, entity.getId());
      }
    } else if (StringUtils.equals(entity.getUiStyle(), AnyDataModelConstants.UI_STYLE_BORDER_FORM)) { // 如果是border布局则必须指定一个数据源，否则还原为普通列表布局
      Long treeDs = jdbcTemplate.queryForObject("select count(*) from any_data_model_field f where f.data_model_id=? and f.as_tree_ds=true",
          Long.class, entity.getId());
      if (treeDs == 0) {
        jdbcTemplate.update("update any_data_model set ui_style=? where id=?", AnyDataModelConstants.UI_STYLE_GRID_FORM, entity.getId());
      }
    }

    // 为新增字段创建主键、创建时间字段、操作人字段
    if (isNew) {
      // 主键
      AnyDataModelField pk = new AnyDataModelField();
      pk.setFieldName(AnyDataModelConstants.SYS_COL_PRIMARY);
      pk.setDisplayName(AnyDataModelConstants.SYS_COL_PRIMARY);
      pk.setIsPrimary(true);
      pk.setIsSys(true);
      pk.setDataType(AnyDataModelConstants.DATA_TYPE_NUMBER);
      pk.setDataTypeName(AnyDataModelConstants.dataTypeName(AnyDataModelConstants.DATA_TYPE_NUMBER));
      pk.setDataLength(28);
      pk.setDataScale(0);
      pk.setHidden(true);
      pk.setNullable(false);
      pk.setOrders(0);
      pk.setInputType(AnyDataModelConstants.XTYPE_HIDDEN);
      pk.setDataModel(entity);
      pk.setId(idGen.nextId());

      // 操作时间
      AnyDataModelField updateTime = new AnyDataModelField();
      updateTime.setFieldName(AnyDataModelConstants.SYS_COL_UPDATE_TIME);
      updateTime.setDisplayName("操作时间");
      updateTime.setIsPrimary(false);
      updateTime.setIsSys(true);
      updateTime.setDataType(AnyDataModelConstants.DATA_TYPE_DATE);
      updateTime.setDataTypeName(AnyDataModelConstants.dataTypeName(AnyDataModelConstants.DATA_TYPE_DATE));
      updateTime.setHidden(true);
      updateTime.setInGrid(false);
      updateTime.setInputType(AnyDataModelConstants.XTYPE_HIDDEN);
      updateTime.setDataModel(entity);
      updateTime.setId(idGen.nextId());
      // 操作人
      AnyDataModelField updateUser = new AnyDataModelField();
      updateUser.setFieldName(AnyDataModelConstants.SYS_COL_USER);
      updateUser.setDisplayName("操作人员");
      updateUser.setIsPrimary(false);
      updateUser.setIsSys(true);
      updateUser.setDataType(AnyDataModelConstants.DATA_TYPE_NUMBER);
      updateUser.setDataTypeName(AnyDataModelConstants.dataTypeName(AnyDataModelConstants.DATA_TYPE_NUMBER));
      updateUser.setHidden(true);
      updateUser.setInGrid(false);
      updateUser.setInputType(AnyDataModelConstants.XTYPE_HIDDEN);
      updateUser.setDataModel(entity);
      updateUser.setIsForeign(true);
      updateUser.setRefTableName("STAFF");
      updateUser.setRefDisplayCol("NAME");
      updateUser.setId(idGen.nextId());

      SQLReady sqlPk = new SQLRequest(pk).insert();
      jdbcTemplate.update(sqlPk.getSql(), sqlPk.getArgs());

      SQLReady sqlUpdateTime = new SQLRequest(updateTime).insert();
      jdbcTemplate.update(sqlUpdateTime.getSql(), sqlUpdateTime.getArgs());

      SQLReady sqlUpdateUser = new SQLRequest(updateUser).insert();
      jdbcTemplate.update(sqlUpdateUser.getSql(), sqlUpdateUser.getArgs());
    }

    return entity;
  }
  
  private void fixPrivileges(AnyDataModel entity) {
    if (StringUtils.isBlank(entity.getReadRoles())) {
      entity.setReadRoles(null);
    }
    if (StringUtils.isBlank(entity.getCreateRoles())) {
      entity.setCreateRoles(null);
    }
    if (StringUtils.isBlank(entity.getUpdateRoles())) {
      entity.setUpdateRoles(null);
    }
    if (StringUtils.isBlank(entity.getDeleteRoles())) {
      entity.setDeleteRoles(null);
    }
    // 权限类别
    if (StringUtils.isNotBlank(entity.getPrivType())) {
      Set<String> typeSet = new HashSet<String>(); // 用于重新构建权限类别字符串

      String[] types = entity.getPrivType().split(",");
      if (types != null) {
        for (String type : types) {
          if (StringUtils.isNotBlank(type)) {
            typeSet.add(type);
          }
        }
      }

      // 修正权限字符串，如果不是数字，则设置为null
      if (StringUtils.isNumeric(entity.getPrivStr())) {
        typeSet.add(AnyDataModelConstants.DATA_PRIV_FIELD);
      } else {
        entity.setPrivStr(null);
      }

      entity.setPrivType(StringUtils.join(typeSet.iterator(), ","));
    }
  }

  private void defaultValues(AnyDataModel entity) {
    if (StringUtils.isBlank(entity.getFormPos())) {
      entity.setFormPos(AnyDataModelConstants.FORM_POS_WIN);
    }
    if (entity.getFormWidth() == null || entity.getFormWidth() <= 0) {
      entity.setFormWidth(600);
    }
    if (entity.getFormHeight() == null || entity.getFormHeight() <= 0) {
      entity.setFormHeight(450);
    }
    if (StringUtils.isBlank(entity.getUiStyle())) {
      entity.setUiStyle(AnyDataModelConstants.UI_STYLE_GRID_FORM);
    }
  }

  /**
   * 创建表
   */
  public void createTable(AnyDataModel dataModel) {
    Preconditions.checkNotNull(dataModel);
    Preconditions.checkNotNull(dataModel.getId());

    dataModel = byId(dataModel.getId(), true);
    if (!anyDDL.isTableExists(dataModel)) {
      anyDDL.createTable(dataModel);
    }
  }

  /**
   * 删除表
   */
  public void dropTable(AnyDataModel dataModel) {
    Preconditions.checkNotNull(dataModel);
    Preconditions.checkNotNull(dataModel.getId());

    dataModel = byId(dataModel.getId(), true);
    if (anyDDL.isTableExists(dataModel)) {
      anyDDL.dropTable(dataModel);
    }
  }

  /**
   * 更新AnyDataModel的UI风格
   * @param id id of anyDataModel
   * @param uiStyle {@link AnyDataModelConstants#UI_STYLES}
   */
  @Transactional
  public void updateUiStyle(Long id, String uiStyle) {
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(uiStyle);

    jdbcTemplate.update("update any_data_model set ui_style=? where id=?", uiStyle, id);
  }

  /**
   * 更新表单位置
   */
  @Transactional
  public void updateFormPos(AnyDataModel model) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(model.getId());

    jdbcTemplate.update("update any_data_model set form_width=?, form_height=?, form_pos=? where id=?", model.getFormWidth(),
        model.getFormHeight(), model.getFormPos(), model.getId());
  }

  /**
   * 构建一棵由AnyDataModelType和AnyDataModel组成的树形列表
   * 
   * @param rootTypeId 根AnyDataModelType对象
   * @param currUser 当前登录用户
   * @param restrictMenu 只列出在菜单显示的AnyDataModel
   * @param restrictTable 只列出生成了Table的
   */
  public List<Map<String, Object>> dataModelTree(Long rootTypeId, User currUser, Boolean restrictMenu, Boolean restrictTable) {
    List<Map<String, Object>> tree = new ArrayList<Map<String, Object>>();
    SQLReady sql = new SQLRequest(AnyDataModelType.class, true).select().append("WHERE parent_id=?", rootTypeId).orderBy("orders", "asc");
    List<AnyDataModelType> childTypes = jdbcTemplate.query(sql.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelType>(AnyDataModelType.class), sql.getArgs());

    for (AnyDataModelType type : childTypes) {
      Map<String, Object> node = new HashMap<String, Object>();
      node.put("id", type.getId());
      node.put("text", type.getName());
      node.put("name", type.getName());
      node.put("path", type.getPath());
      if (StringUtils.isNotBlank(type.getIcon())) {
        node.put("icon", type.getIcon());
      }
      node.put("expanded", true);

      List<Map<String, Object>> children = dataModelTree(type.getId(), currUser, restrictMenu, restrictTable);
      if (!children.isEmpty()) {
        node.put("children", children);
      }
      // node.put("leaf", children.isEmpty());

      tree.add(node);
    }

    sql = new SQLRequest(AnyDataModel.class, true).select().append(" WHERE type_id=? ", rootTypeId);
    if (restrictMenu) {
      sql.append(" and (in_menu = true or in_menu is null)");
    }
    if (restrictTable) {
      sql.append("  and has_table=true ");
    }
    sql.orderBy("orders", "asc");

    List<AnyDataModel> models = jdbcTemplate.query(sql.getSql(), new BeanPropertyRowMapperEx<AnyDataModel>(AnyDataModel.class),
        sql.getArgs());

    for (AnyDataModel model : models) {
      if (!canAccess(model, currUser)) { // 如果权限不允许
        logger.debug("模块 {},不可被{}访问", model.getDisplayName(), currUser.getName());
        continue;
      }
      Map<String, Object> node = new HashMap<String, Object>();
      node.put("id", model.getId());
      node.put("table", model.getRealTableName());
      node.put("text", model.getDisplayName());
      node.put("name", model.getDisplayName());
      node.put("leaf", true);
      node.put("viewable", model.getViewable());
      node.put("exportable", model.getExportable());
      node.put("importable", model.getImportable());
      node.put("searchable", model.getSearchable());
      if (StringUtils.isBlank(model.getIcon())) {
        node.put("iconCls", "icon-module");
      } else {
        node.put("icon", model.getIcon());
      }
      node.put("expanded", true);

      node.put("_parentId", rootTypeId);
      node.put("uiStyle", model.getUiStyle());
      node.put("formPos", model.getFormPos());
      node.put("formWidth", model.getFormWidth());
      node.put("formHeight", model.getFormHeight());
      node.put("readRoles", model.getReadRoles());
      node.put("updateRoles", model.getUpdateRoles());
      node.put("deleteRoles", model.getDeleteRoles());
      node.put("hasTable", model.getHasTable());
      node.put("hasCode", model.getHasCode());
      if (model.getHasCode()) {
        node.put("jsView", model.getJsView());
      }

      tree.add(node);
    }

    return tree;
  }

  /**
   * 判断某个模块是否可以被指定用户访问
   * 
   * @param model 给出模块
   * @param user 给出用户
   * @return 如果可以访问，返回true，否则返回false
   */
  public boolean canAccess(AnyDataModel model, User user) {
    // 如果模块没有定义权限，则都可以访问
    if (StringUtils.isBlank(model.getReadRoles()) && StringUtils.isBlank(model.getUpdateRoles())) {
      return true;
    }

    Collection<Role> roles = Collections.emptySet();
    if (user != null && user.getId() != null) {
      user = jdbcTemplate.get(User.class, user.getId());
      roles = userService.getRolesByUser(user);
    }
    // 如果模块定义了权限，而用户没有任何权限，则不可访问
    if (roles.isEmpty()) {
      return false;
    }
    //读取权限或者写权限有一个满足要求，则返回true, 否则返回false
    return (isRoleMatches(roles, model.getReadRoles()) || isRoleMatches(roles, model.getUpdateRoles()));
  }
  
  private boolean isRoleMatches(Collection<Role> roles, String rolesList) {
    if (StringUtils.isNotBlank(rolesList)) { // 如果定义了权限
      for (Role role : roles) {
        if (rolesList.indexOf(role.getName()) >= 0) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 删除一个AnyDataModel，同时删除field和对应的数据表
   */
  @Transactional
  public void remove(AnyDataModel entity) {
    Preconditions.checkNotNull(entity);
    Preconditions.checkNotNull(entity.getId());
    entity = this.byId(entity.getId(), false);

    // 首先要删除模块的字段
    SQLReady sql = new SQLRequest(AnyDataModelField.class, true).select().append(" WHERE data_model_id=? ", entity.getId());
    List<AnyDataModelField> fields = jdbcTemplate.query(sql.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sql.getArgs());
    for (int i = 0; i < fields.size(); i++) {
      fieldService.remove(fields.get(i));
    }

    dropTable(entity); // 删除表
    jdbcTemplate.update("delete from any_data_model where id=?", entity.getId());
    /*
     * 流程上线就可以用了，by catstiger try { Long formKey = entity.getId(); processFormBindingService.unbindForm(formKey.toString());
     * } catch (Exception e) { e.getMessage(); throw Exceptions.unchecked("解除流程绑定错误！"); }
     */
  }

  /**
   * AnyDataModel排序
   * 
   * @param sourceId 源对象
   * @param destId 目标对象
   * @param position 位置 before\after
   */
  @Transactional
  public void sort(Long sourceId, Long destId, String position) {
    Preconditions.checkNotNull(sourceId);
    Preconditions.checkNotNull(destId);
    AnyDataModel source = byId(sourceId, false);
    AnyDataModel dest = byId(destId, false);

    Preconditions.checkNotNull(source);
    Preconditions.checkNotNull(dest);
    Preconditions.checkState(source.getType().getId().equals(dest.getType().getId()), "必须在同一个类别下排序。");

    int targetOrder = dest.getOrders();
    if ("after".equals(position)) {
      jdbcTemplate.update("update any_data_model f set f.orders = f.orders + 1 where f.orders>? and f.type_id=? and f.id<>?",
          new Object[] { targetOrder, dest.getType().getId(), source.getId() });
      jdbcTemplate.update("update any_data_model f set f.orders=? where id=?", targetOrder + 1, source.getId());
    } else if ("before".equals(position)) {
      jdbcTemplate.update("update any_data_model f set f.orders = f.orders + 1 where f.orders>=? and f.type_id=? and f.id<>?",
          new Object[] { targetOrder, dest.getType().getId(), source.getId() });
      jdbcTemplate.update("update any_data_model f set f.orders=? where id=?", targetOrder, source.getId());
    }
  }

 
  private AnyDataModel byId(Long id, boolean cascade) {
    SQLReady sql = new SQLRequest(AnyDataModel.class, true).selectById().addArg(id);
    AnyDataModel model = jdbcTemplate.queryForObject(sql.getSql(), new BeanPropertyRowMapperEx<AnyDataModel>(AnyDataModel.class),
        sql.getArgs());
    if (cascade) {
      // Fields
      sql = new SQLRequest(AnyDataModelField.class, true).select().append(" WHERE data_model_id=? ", id).orderBy(" orders ", "asc");
      List<AnyDataModelField> fields = jdbcTemplate.query(sql.getSql(),
          new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sql.getArgs());
      model.setFields(fields);
      for (AnyDataModelField field : fields) {
        fieldService.fullField(field);
        field.setDataModel(null); // 防止JSON递归，或者太多冗余
      }
      // containers
      sql = new SQLRequest(AnyCt.class, true).select().append("WHERE data_model_id=?", id).orderBy("orders", "asc");
      List<AnyCt> anyCts = jdbcTemplate.query(sql.getSql(), new BeanPropertyRowMapperEx<AnyCt>(AnyCt.class), sql.getArgs());
      // cmps of ct
      for (AnyCt anyCt : anyCts) {
        sql = new SQLRequest(AnyCmp.class, true).select().append("WHERE any_ct_id=?", anyCt.getId()).orderBy("orders", "asc");
        List<AnyCmp> anyCmps = jdbcTemplate.query(sql.getSql(), new BeanPropertyRowMapperEx<AnyCmp>(AnyCmp.class), sql.getArgs());
        anyCt.setAnyCmps(anyCmps);
      }
      model.setAnyCts(anyCts);
    }

    return model;
  }

  /**
   * 加载AnyDataModel数据，同时加载字段数据
   * 
   * @param id AnyDataModel id
   * @return
   */
  public AnyDataModel withFields(Long id) {
    SQLReady sql = new SQLRequest(AnyDataModel.class, true).selectById().addArg(id);
    AnyDataModel model = jdbcTemplate.queryForObject(sql.getSql(), new BeanPropertyRowMapperEx<AnyDataModel>(AnyDataModel.class),
        sql.getArgs());
    sql = new SQLRequest(AnyDataModelField.class, true).select().append(" WHERE data_model_id=? ", id).orderBy(" orders ", "asc");
    List<AnyDataModelField> fields = jdbcTemplate.query(sql.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sql.getArgs());
    model.setFields(fields);

    return model;
  }

  /**
   * @see com.github.catstiger.common.service.GenericsService#get(java.lang.Long)
   */
  @Override
  public AnyDataModel get(Long id) {
    return byId(id, true);
  }
}
