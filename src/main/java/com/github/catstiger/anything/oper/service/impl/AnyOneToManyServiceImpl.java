package com.github.catstiger.anything.oper.service.impl;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.catstiger.anything.oper.service.AnyOneToManyService;

@Service
public class AnyOneToManyServiceImpl implements AnyOneToManyService {
  @Resource
  private JdbcTemplate jdbcTemplate;

  @Override
  @Transactional
  public void makeConstraintJdbc(String table, Long ownerId, String reverseColumn, Long... manyIds) {
    if (manyIds == null || manyIds.length == 0) {
      return;
    }

    String sql = "UPDATE " + table + " SET " + reverseColumn + "=? WHERE ID=?";
    for (Long manyId : manyIds) {
      jdbcTemplate.update(sql, new Object[] { ownerId, manyId });
    }
  }

}
