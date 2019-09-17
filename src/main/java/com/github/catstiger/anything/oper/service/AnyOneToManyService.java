package com.github.catstiger.anything.oper.service;

public interface AnyOneToManyService {
  /**
   * 使用SQL，保存OneToMany中多的一方的外键
   * @param table “多的一方”的表名
   * @param ownerId “1的一方”的ID
   * @param reverseColumn “多的一方”的指向“1的一方”的字段名
   * @param manyIds “多的一方”的ID
   */
  void makeConstraintJdbc(String table, Long ownerId, String reverseColumn, Long...manyIds);
}
