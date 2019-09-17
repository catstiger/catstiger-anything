package com.github.catstiger.anything.designer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.catstiger.anything.designer.model.AnyDataModelType;
import com.github.catstiger.common.service.GenericsService;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.BeanPropertyRowMapperEx;
import com.github.catstiger.common.util.Exceptions;
import com.google.common.base.Preconditions;

@Service
public class AnyDataModelTypeService extends GenericsService<AnyDataModelType> {

  /**
   * 返回AnyDataModelType树形结构
   * 
   * @param parentId 上级AnyDataModelType ID
   */
  public List<Map<String, Object>> tree(Long parentId) {
    List<Map<String, Object>> tree = new ArrayList<Map<String, Object>>();
    List<AnyDataModelType> anyDataModelTypes = new ArrayList<AnyDataModelType>();
    if (parentId == null) {
      SQLReady sqlReady = new SQLRequest(AnyDataModelType.class, true).select().append("WHERE parent_id is null").orderBy("orders", "asc");
      anyDataModelTypes = jdbcTemplate.query(sqlReady.getSql(), new BeanPropertyRowMapperEx<AnyDataModelType>(AnyDataModelType.class), sqlReady.getArgs());
    } else {
      SQLReady sqlReady = new SQLRequest(AnyDataModelType.class, true).select().append("WHERE parent_id =?", parentId).orderBy("orders", "asc");
      anyDataModelTypes = jdbcTemplate.query(sqlReady.getSql(), new BeanPropertyRowMapperEx<AnyDataModelType>(AnyDataModelType.class), sqlReady.getArgs());
    }

    for (AnyDataModelType type : anyDataModelTypes) {
      Map<String, Object> node = new HashMap<String, Object>();
      node.put("id", type.getId());
      node.put("text", type.getName());
      node.put("name", type.getName());
      node.put("path", type.getPath());
      node.put("leaf", false);
      node.put("expanded", true);
      if (StringUtils.isNotBlank(type.getIcon())) {
        node.put("icon", type.getIcon());
      }
      node.put("_parentId", (type.getParent() != null) ? type.getParent().getId() : null);

      List<Map<String, Object>> children = tree(type.getId());
      node.put("children", children);
      tree.add(node);
    }
    return tree;

  }

  @Override
  @Transactional
  public AnyDataModelType merge(AnyDataModelType entity) {
    AnyDataModelType model = super.merge(entity);
    // 构建Path，方便使用Like查询子节点
    return updatePath(model);
  }

  private AnyDataModelType updatePath(AnyDataModelType entity) {
    AnyDataModelType parent = (entity.getParent() != null && entity.getParentId() != null) ? byId(entity.getParent().getId()) : null;
    String path;
    if (parent != null) {
      path = new StringBuilder(200).append(parent.getPath()).append(",").append(entity.getId()).toString();
    } else {
      path = entity.getId().toString();
    }
    entity.setPath(path);
    jdbcTemplate.update("update any_data_model_type set path=? where id=?", path, entity.getId());

    SQLReady sqlReady = new SQLRequest(AnyDataModelType.class, true).select().append("WHERE parent_id=?", entity.getId());
    List<AnyDataModelType> children = jdbcTemplate.query(sqlReady.getSql(), new BeanPropertyRowMapperEx<AnyDataModelType>(AnyDataModelType.class),
        sqlReady.getArgs());
    entity.setChildren(children);

    if (CollectionUtils.isNotEmpty(entity.getChildren())) {
      for (AnyDataModelType child : entity.getChildren()) {
        updatePath(child);
      }
    }
    return entity;
  }

  /**
   * 删除一个分类
   * @param entity Instance of AnyDataModelType, contains id.
   * @throws RuntimeException 如果存在下级分类或者模块
   */
  @Transactional
  public void remove(AnyDataModelType entity) {
    Preconditions.checkNotNull(entity);
    Preconditions.checkNotNull(entity.getId());

    entity = byId(entity.getId());
    Long childrens = jdbcTemplate.queryForObject("select count(*) from any_data_model_type where parent_id=?", Long.class, entity.getId());

    if (childrens > 0) {
      throw Exceptions.unchecked("请首先删除下级分类。");
    }
    Long models = jdbcTemplate.queryForObject("select count(*) from any_data_model where type_id=?", Long.class, entity.getId());
    if (models > 0) {
      throw Exceptions.unchecked("请首先删除该分类下面的模块。");
    }
    jdbcTemplate.update("delete from any_data_model_type where id=?", entity.getId());
  }

  /**
   * 调整排列顺序（UI通过拖动方式）
   * @param sourceId 被拖动的分类的ID
   * @param destId 目标分类ID
   * @param position 位置： after, before, append
   */
  @Transactional
  public void positioning(Long sourceId, Long destId, String position) {
    Preconditions.checkNotNull(sourceId);
    Preconditions.checkNotNull(position);

    if (destId == null) {
      destId = 0L;
    }

    AnyDataModelType source = byId(sourceId);
    AnyDataModelType dest = byId(destId);

    if ("before".equals(position)) {
      Preconditions.checkNotNull(dest);
      AnyDataModelType parent = parentIfExists(dest);
     
      Integer targetOrder = dest.getOrders();
      if (parent != null) {
        jdbcTemplate.update("update any_data_model_type f set f.orders = f.orders + 1 where f.orders>=? and f.parent_id=? ",
            new Object[] { targetOrder, parent.getId() });
        jdbcTemplate.update("update any_data_model_type f set f.orders=?, parent_id=? where id=?", targetOrder, parent.getId(), source.getId());
      } else {
        jdbcTemplate.update("update any_data_model_type f set f.orders = f.orders + 1 where f.orders>=? and f.parent_id is null ",
            new Object[] { targetOrder });
        jdbcTemplate.update("update any_data_model_type f set f.orders=?, parent_id=null where id=?", targetOrder, source.getId());
      }
    } else if ("after".equals(position)) {
      Preconditions.checkNotNull(dest);
      AnyDataModelType parent = parentIfExists(dest);

      Integer targetOrder = dest.getOrders();
      if (parent != null) {
        jdbcTemplate.update("update any_data_model_type f set f.orders = f.orders + 1 where f.orders>? and f.parent_id=?",
            new Object[] { targetOrder, parent.getId() });
        jdbcTemplate.update("update any_data_model_type f set f.orders=?, parent_id=? where id=?", targetOrder + 1, parent.getId(), source.getId());
      } else {
        jdbcTemplate.update("update any_data_model_type f set f.orders = f.orders + 1 where f.orders>? and f.parent.id is null", new Object[] { targetOrder });
        jdbcTemplate.update("update any_data_model_type f set f.orders=?, parent_id=null where id=?", targetOrder + 1, source.getId());
      }

    } else if ("append".equals(position)) {
      if (dest == null) {
        jdbcTemplate.update("update any_data_model_type f set f.orders = f.orders + 1 where f.parent_id is null");
        jdbcTemplate.update("update any_data_model_type set parent_id=null,orders=0 where id=?", source.getId());
      } else {
        jdbcTemplate.update("update any_data_model_type f set f.orders = f.orders + 1 where f.parent_id = ?", dest.getId());
        jdbcTemplate.update("update any_data_model_type f set f.parent_id=?,orders=0 where id=?", dest.getId(), source.getId());
      }
    }
  }
  
  private AnyDataModelType parentIfExists(AnyDataModelType child) {
    if (child.getParent() != null && child.getParent().getId() != null) {
      return byId(child.getParent().getId());
    }
    return null;
  }

  private AnyDataModelType byId(Long id) {
    SQLReady sql = new SQLRequest(AnyDataModelType.class, true).selectById().addArg(id);
    return jdbcTemplate.queryForObject(sql.getSql(), new BeanPropertyRowMapperEx<AnyDataModelType>(AnyDataModelType.class), sql.getArgs());
  }
}
