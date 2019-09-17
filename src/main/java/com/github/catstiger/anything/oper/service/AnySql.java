package com.github.catstiger.anything.oper.service;

import java.util.List;
import java.util.Map;

import com.github.catstiger.common.sql.Page;
import com.github.catstiger.common.sql.filter.QueryPart;

/**
 * 使用SQL操作数据库的接口
 * @author lizhenshan
 *
 */
public interface AnySql {
  /**
   * 向指定的表中插入一条数据
   * @param tableName 表名
   * @param args 插入的数据，以字段名作为KEY，实现类需要将Value转换为响应的数据类型
   * @return
   */
  Long insert(String tableName, Map<String, Object> args);

  /**
   * 更新数据
   * @param tableName 表名
   * @param id 主键
   * @param args 更新的数据
   */
  void update(String tableName, Long id, Map<String, Object> args);
 
  /**
   * 删除数据
   * @param tableName 表名
   * @param id 主键
   */
  void delete(String tableName, Long id);
  
  /**
   * 执行数据查询操作
   * @param tableName 表名
   * @param references 外键关联信息，Key为外键字段，Value是一个String数组，第一个元素为引用表名，第二个元素为显示字段名，
   *     目前只支持对主键的引用，因此不必传递被引用字段。
   * @param args 查询参数
   * @param sortName 排序字段名
   * @param sortOrder 排序方向，ASC，DESC
   * @return
   */
  List<Map<String, Object>> select(String tableName, Map<String, String[]> references, List<QueryPart> args, 
      String sortName, String sortOrder, String querySnippets);

  /**
   * 执行数据分页查询操作
   * @param page Page对象
   * @param tableName 表名
   * @param references 外键关联信息，Key为外键字段，Value是一个String数组，第一个元素为引用表名，第二个元素为显示字段名，
   *     目前只支持对主键的引用，因此不必传递被引用字段。
   * @param args 查询参数
   * @param sortName 排序字段名
   * @param sortOrder 排序方向，ASC，DESC
   * @return Page instance include query result.
   */
  Page pagedQuery(Page page, String tableName, Map<String, String[]> references, List<QueryPart> args, String sortName, String sortOrder, String querySnippets);

  /**
   * 加载一条数据
   * @param tableName 表名
   * @param id 主键
   * @return
   */
  Map<String, Object> load(String tableName, Long id, String...cols);
}
