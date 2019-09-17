package com.github.catstiger.anything.designer.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.ddl.AnyDDL;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.common.service.GenericsService;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.Mappers;
import com.github.catstiger.common.util.Exceptions;
import com.google.common.base.Preconditions;

@Service
public class AnyDataModelFieldService extends GenericsService<AnyDataModelField> {
  @Resource
  private AnyDDL anyDDL;

  @Override
  @Transactional
  public AnyDataModelField merge(AnyDataModelField entity) {
    Preconditions.checkNotNull(entity);
    Preconditions.checkNotNull(entity.getDataModel());
    Preconditions.checkNotNull(entity.getDataModel().getId());

    AnyDataModel dataModel = jdbcTemplate.get(AnyDataModel.class, entity.getDataModel().getId());
    entity.setDataModel(dataModel);

    boolean isNew = false;
    AnyDataModelField oldField = null;
    if (entity.getId() == null) {
      isNew = true;
      entity.setCreateTime(new Date());
      if (entity.getOrders() == null || entity.getOrders() < 0) {
        entity.setOrders(0);
      }
      // 如果存在orders相同的数据，则将所有大于等于这个orders的都+1
      Long idxCount = jdbcTemplate.queryForObject("select count(*) from any_data_model_field a where a.orders=? and a.data_model_id=?", Long.class,
          entity.getOrders(), entity.getDataModel().getId());
      if (idxCount > 0) {
        jdbcTemplate.update("update any_data_model_field a set a.orders=a.orders+1 where a.data_model_id=? and a.orders>=?", entity.getDataModel().getId(),
            entity.getOrders());
      }
    } else {
      oldField = jdbcTemplate.get(AnyDataModelField.class, entity.getId());
    }
    
    // 如果既没有指向AnyDataModel，也没有指向STAFF、DEPT等表，则不是外键
    if (!isForeign(entity)) {
      entity.setIsForeign(false);
    } else { //处理隐隐的表名、字段名等
      AnyDataModel refModel = getDataModel(entity.getRefDataModel().getId());
      entity.setRefTableName(refModel.getTableName()); //表名
      AnyDataModelField dispField = this.get(entity.getRefFieldDisplay().getId());
      entity.setRefDisplayCol(dispField.getFieldName());//显示的字段
      entity.setRefCol("id"); //对应的主键
      entity.setIsForeign(true);
    }
    
    // 如果显示方式为用户选择器，则创建一个指向STAFF表的外键
    specialField(entity);
    
    entity.setFieldName(entity.getFieldName().toLowerCase()); // 字段名都转换为小写
    // jscode调整
    fixJsCode(entity);
    logger.info("创建字段 \n {}", JSON.toJSONString(entity, true));
    entity = super.merge(entity);
    // 如果已经生成表，则必须同步字段
    if (oldField != null) {
      syncTable(entity, isNew, oldField);
    }
    
    return entity;
  }
  
  private void syncTable(AnyDataModelField entity, boolean isNew, AnyDataModelField oldField) {
    if (entity.getDataModel().getHasTable() != null && entity.getDataModel().getHasTable()
        && (!AnyDataModelConstants.XTYPE_MULTIFILEUPLOAD.equals(entity.getInputType())
            && !AnyDataModelConstants.XTYPE_ONETOMANY.equals(entity.getInputType()))) {
      if (anyDDL.isTableExists(entity.getDataModel())) {
        if (isNew) { // 新字段，则直接添加
          anyDDL.addColumn(entity);
        } else {
          if (needsDrop(oldField, entity)) { // 如果需要删除字段，那么先删除字段
            anyDDL.dropColumn(entity);
            anyDDL.addColumn(entity);
          }
        }
      }
    }
  }
  
  private void fixJsCode(AnyDataModelField entity) {
    String jscode = entity.getJscode();
    if (StringUtils.isNotBlank(jscode)) {
      if (!jscode.startsWith("{")) {
        jscode = "{\n" + jscode;
      }
      if (!jscode.endsWith("}")) {
        jscode = jscode + "\n}";
      }
      entity.setJscode(jscode);
    }
  }

  private boolean isForeign(AnyDataModelField entity) {
      return (entity.getRefDataModel() != null && entity.getRefDataModel().getId() != null)
          && (entity.getRefFieldDisplay() != null && entity.getRefFieldDisplay().getId() != null);
  }

  /**
   * 处理特殊字段： 
   * <ul>
   *     <li>用户选择字段，生成一个指向users表的外键</li>
   *     <li>部门选择字段，生成一个纸箱dept表的外键</li>
   *     <li>文件上传</li>
   *     <li>其他外键</li>
   * </ul>
   */
  private void specialField(AnyDataModelField entity) {
    if (StringUtils.equalsIgnoreCase(entity.getInputType(), AnyDataModelConstants.XTYPE_USER)) {
      entity.setIsForeign(true);
      entity.setRefTableName("staff");
      entity.setRefDisplayCol("name");
      entity.setRefCol("id");
    } else if (StringUtils.equalsIgnoreCase(entity.getInputType(), AnyDataModelConstants.XTYPE_DEPT)) { // 如果显示方式为部门选择器，则创建一个指向DEPT表的外键
      entity.setIsForeign(true);
      entity.setRefTableName("dept");
      entity.setRefDisplayCol("name");
      entity.setRefCol("id");
    } else if (StringUtils.equalsIgnoreCase(entity.getInputType(), AnyDataModelConstants.XTYPE_FILEUPLOAD)) { // 如果采用文件上传，则isFile为true
      entity.setIsFile(true);
    } else {
      entity.setRefTableName(null);
      entity.setRefDisplayCol(null);
      entity.setRefCol(null);
    }
  }

  /**
   * 当修改字段的时候，是否需要将物理表中的字段drop
   */
  private boolean needsDrop(AnyDataModelField oldField, AnyDataModelField newField) {
    Preconditions.checkArgument(oldField != null);
    // 字段特征不同
    boolean isDefferentFeatures = !Objects.equals(oldField.getNullable(), newField.getNullable())
        || !Objects.equals(oldField.getDataLength(), newField.getDataLength()) 
        || !Objects.equals(oldField.getDataScale(), newField.getDataScale())
        || !StringUtils.equalsIgnoreCase(oldField.getDataType(), newField.getDataType());
    if (isDefferentFeatures) {
      return true;
    }
    // 引用对象不同
    boolean isDefferentRefObjects = !Objects.equals(oldField.getRefDataModel(), newField.getRefDataModel())
        || !Objects.equals(oldField.getRefField(), newField.getRefField()) 
        || !StringUtils.equalsIgnoreCase(oldField.getRefCol(), newField.getRefCol());
    if (isDefferentRefObjects) {
      return true;
    }
    // 名称不同
    boolean isDeferentNames = !StringUtils.equalsIgnoreCase(oldField.getFieldName(), newField.getFieldName())
        || !StringUtils.equalsIgnoreCase(oldField.getRefTableName(), newField.getRefTableName());
    if (isDeferentNames) {
      return true;
    }

    return false;
  }

  /**
   * 字段排序，将需要排序的字段，排到目标字段的前面
   * 
   * @param sourceId 需要排序的字段ID
   * @param destinationId 目标字段ID
   */
  @Transactional
  public void before(Long sourceId, Long destinationId) {
    Preconditions.checkNotNull(sourceId, "排序字段ID不能为空。");
    Preconditions.checkNotNull(destinationId, "目标字段Id不能为空。");

    long destDataModelId = jdbcTemplate.queryForObject("select data_model_id from any_data_model_field where id=?", Long.class, destinationId);
    int targetOrder = jdbcTemplate.queryForObject("select orders from any_data_model_field where id=?", Integer.class, destinationId);

    // 排在目标字段（含目标字段）后面的字段，继续后移
    jdbcTemplate.update("update any_data_model_field f set f.orders = f.orders + 1 where f.orders>=? and data_model_id=?",
        new Object[] { targetOrder, destDataModelId });
    jdbcTemplate.update("update any_data_model_field f set f.orders=? where f.id=?", targetOrder, sourceId);
  }

  /**
   * 字段排序，将需要排序的字段，排到目标字段的后面面
   * 
   * @param sourceId 需要排序的字段
   * @param destinationId 目标字段
   */
  @Transactional
  public void after(Long sourceId, Long destinationId) {
    Preconditions.checkNotNull(sourceId, "排序字段ID不能为空。");
    Preconditions.checkNotNull(destinationId, "目标字段Id不能为空。");

    long destDataModelId = jdbcTemplate.queryForObject("select data_model_id from any_data_model_field where id=?", Long.class, destinationId);
    int targetOrder = jdbcTemplate.queryForObject("select orders from any_data_model_field where id=?", Integer.class, destinationId);
    // 排在目标字段（含目标字段）后面的字段，继续后移
    jdbcTemplate.update("update any_data_model_field f set f.orders = f.orders + 1 where f.orders>? and data_model_id=?",
        new Object[] { targetOrder, destDataModelId });
    jdbcTemplate.update("update any_data_model_field f set f.orders=? where f.id=?", targetOrder + 1, sourceId);
  }

  /**
   * 删除一个字段，同时删除字段对应的JS组件，如果已经生成表，则drop表中的字段
   * @param entity 给出要删除的字段(ID is required)
   */
  @Transactional
  public void remove(AnyDataModelField entity) {
    entity = byId(entity.getId());
    entity.setDataModel(jdbcTemplate.get(AnyDataModel.class, entity.getDataModel().getId()));

    // 查询所有引用这个字段的字段
    List<Map<String, Object>> refeeFileds = jdbcTemplate.query(
        "select id, data_model_id, field_name from any_data_model_field f where f.ref_field_id=? or f.ref_field_display_id=?", new ColumnMapRowMapper(),
        entity.getId(), entity.getId());

    if (CollectionUtils.isNotEmpty(refeeFileds)) {
      for (Map<String, Object> field : refeeFileds) {
        AnyDataModel model = jdbcTemplate.get(AnyDataModel.class, MapUtils.getLong(field, "data_model_id"));
        // 将所有引用这个字段的字段对应的物理数据删除
        if (model.getHasTable() && anyDDL.isTableExists(model)) {
          try {
            jdbcTemplate.update("UPDATE " + model.getRealTableName() + " SET " + MapUtils.getString(field, "field_name") + "=null");
          } catch (Exception e) {
            logger.warn(e.getMessage());
          }
        }
        jdbcTemplate.update("delete from any_data_model_field where id=?", MapUtils.getLong(field, "id"));// 删除引用这个字段的字段
      }
    }
    //如果已经生成表
    dropColumn(entity);
    
    jdbcTemplate.update("delete from any_cmp where data_model_field_id=?", entity.getId());// 删除相关的界面组件信息
    jdbcTemplate.update("delete from any_data_model_field where id=?", entity.getId());
  }
  
  private void dropColumn(AnyDataModelField entity) {
    if (hasTable(entity)) {
      if (anyDDL.isTableExists(entity.getDataModel())) {
        // 删除字段对应的数据
        if (!StringUtils.equalsIgnoreCase(entity.getFieldName(), AnyDataModelConstants.SYS_COL_PRIMARY)) {
          try {
            jdbcTemplate.update("UPDATE " + entity.getDataModel().getRealTableName() + " SET " + entity.getFieldName() + "=null");
            anyDDL.dropColumn(entity); // 删除实际字段
          } catch (Exception e) {
            logger.warn(e.getMessage());
          }
        }
      }
    }
  }
  
  private boolean hasTable(AnyDataModelField entity) {
    return entity.getDataModel() != null && entity.getDataModel().getHasTable() != null && entity.getDataModel().getHasTable();
  }

  /**
   * 根据AnyDataModel取得所有的AnyDataModelField.
   * 
   * @param dataModel 给定AnyDataModel
   * @return
   */
  public List<AnyDataModelField> fieldsByDataModel(AnyDataModel dataModel) {
    Preconditions.checkNotNull(dataModel);
    Preconditions.checkNotNull(dataModel.getId());

    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append("WHERE data_model_id=? ", dataModel.getId()).orderBy("orders");
    List<AnyDataModelField> fields = jdbcTemplate.query(sqlReady.getSql(), Mappers.byClass(AnyDataModelField.class), sqlReady.getArgs());
    return fields;
  }

  /**
   * 将指定ID的AnyDataModelField作为其所在AnyDataModel 的TreeColumn
   */
  @Transactional
  public void asTreeCol(Long fieldId) {
    AnyDataModelField field = this.byId(fieldId);
    if (field.getAsTreeCol() != null && field.getAsTreeCol()) {
      return;
    }

    jdbcTemplate.update("update any_data_model_field f set f.as_tree_col=false where f.data_model_id=?", field.getDataModel().getId());
    jdbcTemplate.update("update any_data_model_field set as_tree_col=true where id=?", field.getId());
  }

  /**
   * 将指定ID的AnyDataModelField作为其所在AnyDataModel 的分类列表的数据源
   */
  @Transactional
  public void asTreeDs(Long fieldId) {
    AnyDataModelField field = byId(fieldId);
    if (field.getAsTreeDs() != null && field.getAsTreeDs()) {
      return;
    }
    jdbcTemplate.update("update any_data_model_field f set f.as_tree_ds=false where f.data_model_id=?", field.getDataModel().getId());
    jdbcTemplate.update("update any_data_model_field set as_tree_ds=true where id=?", field.getId());
  }

  /**
   * 判断一个字段名是否在指定的AnyDataModel序列中
   * 
   * @param model 给出AnyDataModel
   * @param fieldName 给出字段名
   * @return 如果fieldName在是AnyDataModel的字段，则返回true，否则返回false
   */
  public Boolean isField(AnyDataModel model, String fieldName) {
    model = jdbcTemplate.get(AnyDataModel.class, model.getId());
    List<String> fields = jdbcTemplate.queryForList("select field_name from any_data_model_field where data_model_id=?", String.class, model.getId());
    for (String field : fields) {
      if (StringUtils.equalsIgnoreCase(fieldName, field)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 构造一个离线的AnyDataModelField，方便Json解析
   * 
   * @param fieldId ID of AnyDataModelField
   * @return Detached AnyDataModelField
   */
  public AnyDataModelField makeDetached(Long fieldId) {
    AnyDataModelField simpleField = this.byId(fieldId, true);
    if (simpleField != null && simpleField.getDataModel() != null && simpleField.getDataModel().getId() != null) {
      AnyDataModel dataModel = this.getDataModel(simpleField.getDataModel().getId());
      simpleField.setDataModel(dataModel);
    }
    return simpleField;
  }

  /**
   * 保存系统字段
   */
  @Transactional
  public void saveSys(AnyDataModelField model) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(model.getId());

    AnyDataModelField entity = get(model.getId());
    entity.setDisplayName(model.getDisplayName());
    entity.setInGrid(model.getInGrid());

    merge(entity);
  }

  /**
   * 取得某个AnyDataModel所有的外键，并将字段名与[真实表名,显示字段名]的对应关系 组成一个Map
   * 
   * @param model 给出AnyDataModel
   */
  public Map<String, String[]> foreigns(AnyDataModel model) {
    // 查询所有外键
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append("where data_model_id=? and is_foreign=true", model.getId())
        .orderBy("orders");
    List<AnyDataModelField> foreigns = jdbcTemplate.query(sqlReady.getSql(), Mappers.byClass(AnyDataModelField.class), sqlReady.getArgs());

    Map<String, String[]> references = new TreeMap<String, String[]>();
    for (AnyDataModelField fk : foreigns) {
      if (fk.getRefDataModel() != null && fk.getRefFieldDisplay() != null) {
        String realTableName = jdbcTemplate.queryForObject("select lower(table_name) from any_data_model where id=?", String.class,
            fk.getRefDataModel().getId());
        realTableName = AnyDataModelConstants.getReadTablename(realTableName);
        String refFieldDisplay = jdbcTemplate.queryForObject("select lower(field_name) from any_data_model_field where id=?", String.class,
            fk.getRefFieldDisplay().getId());
        references.put(fk.getFieldName().toUpperCase(), new String[] { realTableName, refFieldDisplay });
      } else if (fk.getRefTableName() != null && fk.getRefDisplayCol() != null) {
        references.put(fk.getFieldName().toUpperCase(), new String[] { fk.getRefTableName().toUpperCase(), fk.getRefDisplayCol().toUpperCase() });
      }
    }

    return references;
  }

  /**
   * 保存一对多字段
   */
  @Transactional
  public void saveOneToMany(AnyDataModelField model) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(model.getDataModel());
    Preconditions.checkNotNull(model.getReverseDataModel());
    Preconditions.checkNotNull(model.getReverseField());

    model.setAsTreeCol(false);
    model.setAsTreeDs(false);
    model.setInForm(true);
    model.setInGrid(false);
    model.setInQuery(false);
    model.setCreateTime(new Date());
    model.setInputType(AnyDataModelConstants.XTYPE_ONETOMANY);
    model.setDataType(AnyDataModelConstants.DATA_TYPE_ONETOMANY);
    model.setDataTypeName(AnyDataModelConstants.dataTypeName(AnyDataModelConstants.DATA_TYPE_ONETOMANY));

    merge(model);
  }

  /**
   * 保存一对多字段对应的模块的外键
   */
  @Transactional
  public AnyDataModelField saveReverseField(AnyDataModelField model) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(model.getDataModel());
    Preconditions.checkNotNull(model.getRefDataModel());

    Long mainTablePrimaryKey = jdbcTemplate.queryForObject(
        "select id from any_data_model_field f where f.data_model_id=? and f.is_primary=true and f.is_sys=true", Long.class, model.getRefDataModel().getId());
    if (mainTablePrimaryKey == null) {
      throw Exceptions.unchecked("主表主键不存在。");
    }
    AnyDataModelField primaryKey = new AnyDataModelField();
    primaryKey.setId(mainTablePrimaryKey);

    model.setRefField(primaryKey);
    model.setRefFieldDisplay(primaryKey); // 暂时用关联字段作为显示字段

    model.setFieldName(model.getFieldName().toUpperCase()); // 字段名大写

    model.setAsTreeCol(false);
    model.setAsTreeDs(false);
    model.setInForm(false);
    model.setInGrid(false);
    model.setInQuery(false);
    model.setCreateTime(new Date());
    model.setInputType(AnyDataModelConstants.XTYPE_COMBOBOX);
    model.setDataType(AnyDataModelConstants.DATA_TYPE_NUMBER);
    model.setDataTypeName(AnyDataModelConstants.dataTypeName(AnyDataModelConstants.DATA_TYPE_NUMBER));
    model.setIsForeign(true);
    model.setDataLength(32);
    model = merge(model);
    anyDDL.addColumn(model);

    return model;
  }

  /**
   * 根据ID，得到AnyDataModelField, 如果需要，返回其关联数据，如果没有对应的AnyDataModelField, 返回{@code null}
   * @param id Id of AnyDataModelField
   * @param cascade 是否级联查询关联数据
   */
  public AnyDataModelField byId(Long id, boolean cascade) {
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append(" WHERE id=? ", id);
    AnyDataModelField field = jdbcTemplate.queryForObject(sqlReady.getSql(), Mappers.byClass(AnyDataModelField.class), sqlReady.getArgs());
    if (cascade && field != null) {
      this.fullField(field);
    }
    return field;
  }
  
  AnyDataModelField byId(Long id) {
    if (id == null) {
      return null;
    }
    return this.byId(id, false);
  }

  void fullField(AnyDataModelField field) {
    Preconditions.checkArgument(field != null, "Field对象为null, 无法加载全部数据。");
    
    if (field.getRefDataModel() != null) {
      field.setRefDataModel(getDataModel(field.getRefDataModel().getId()));
    }
    if (field.getReverseDataModel() != null) {
      field.setReverseDataModel(getDataModel(field.getReverseDataModel().getId()));
    }
    if (field.getRefField() != null) {
      field.setRefField(byId(field.getRefField().getId()));
    }
    if (field.getRefFieldDisplay() != null) {
      field.setRefFieldDisplay(byId(field.getRefFieldDisplay().getId()));
    }
    if (field.getReverseField() != null) {
      field.setReverseField(byId(field.getReverseField().getId()));
    }
  }
  

  private AnyDataModel getDataModel(Long id) {
    if (id == null) {
      return null;
    }
    SQLReady sql = new SQLRequest(AnyDataModel.class, true).selectById().addArg(id);
    AnyDataModel dataModel = jdbcTemplate.queryForObject(sql.getSql(), Mappers.byClass(AnyDataModel.class), sql.getArgs());
    return dataModel;
  }
}
