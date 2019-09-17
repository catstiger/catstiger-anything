package com.github.catstiger.anything.oper.service;

import java.util.List;
import java.util.Map;

import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.common.model.KeyValue;

public interface AnyComboService {
  /**
   * 查询任意表，将主键和任意字段组合为Key-Value，通常用于页面下拉列表
   * @param tableName 任意tableName
   * @param displayField 字段名称
   * @param value 字段值，<b>只能处理String类型</b>
   * @return List of KeyValue
   */
  List<KeyValue<Long, Object>> list(String tableName, String displayField, String value);
  
  /**
   * 根据任意AnyDataModelField对象，查询一组Key-Value组合
   * @param field 指定一个外键
   * @param value 字段值，<b>只能处理String类型</b>
   * @return List of KeyValue
   */
  List<KeyValue<Long, Object>> list(AnyDataModelField field, String value);
  
  /**
   * 根据指定的表和指向自身的外键字段名，构建一个树形数据结构
   * @param tableName 指定表名
   * @param parentField 指定外键字段名
   * @param displayField 指定显示字段名
   * @return 用于表述树形结构的List
   */
  List<Map<String, Object>> tree(String tableName, String parentField, String displayField);
  
  /**
   * 根据给定的AnyDataModel对象，构建一个树形数据结构
   * @param dataModel 该DataModel必须是一个treegrid结构的，并且有一个指向自身的外键
   * @return 用于表述树形结构的List
   */
  List<Map<String, Object>> tree(AnyDataModel dataModel);
  
  /**
   * 通常用于自关联的树形列表查询的时候，没有自关联字段的情况
   */
  List<Map<String, Object>> plainTree(String tableName, String displayField);
}
