package com.github.catstiger.anything.oper.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.catstiger.common.sql.id.IdGen;

/**
 * 使得Anything系统兼容Hibernate主键生成策略，一旦生成
 * 源代码之后Hibernate开始管理所有实体类，此时，数据表
 * 中的数据不会出现主键冲突。
 * @author sam
 *
 */
@Service
public class AnyIdService {
  @Autowired
  private IdGen idGen;
  
  /**
   * 生成一个长整型的主键
   */
  public Long genId() {
    return idGen.nextId();
  }
}
