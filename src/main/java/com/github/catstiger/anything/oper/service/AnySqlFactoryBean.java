package com.github.catstiger.anything.oper.service;

import javax.annotation.Resource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.github.catstiger.anything.ddl.AnyDDL;
import com.github.catstiger.anything.oper.service.impl.H2AnySql;
import com.github.catstiger.anything.oper.service.impl.MySqlAnySql;
import com.github.catstiger.anything.oper.service.impl.OracleAnySql;
import com.github.catstiger.common.sql.limit.DatabaseDetector;
import com.github.catstiger.common.util.Exceptions;

@Service
public class AnySqlFactoryBean implements FactoryBean<AnySql> {
  @Resource
  private AnyDDL anyDDL;

  @Resource
  private JdbcTemplate jdbcTemplate;

  @Resource
  private AnyIdService anyIdService;

  @Resource
  private DatabaseDetector dbDetector;

  @Override
  public AnySql getObject() throws Exception {

    if (dbDetector.isH2()) {
      H2AnySql h2AnySql = new H2AnySql();
      h2AnySql.setJdbcTemplate(jdbcTemplate);
      h2AnySql.setAnyDDL(anyDDL);
      h2AnySql.setAnyIdService(anyIdService);

      return h2AnySql;
    } else if (dbDetector.isMySql()) {
      MySqlAnySql mySqlAnySql = new MySqlAnySql();
      mySqlAnySql.setJdbcTemplate(jdbcTemplate);
      mySqlAnySql.setAnyDDL(anyDDL);
      mySqlAnySql.setAnyIdService(anyIdService);

      return mySqlAnySql;
    } else if (dbDetector.isOracle()) {
      OracleAnySql oracleAnySql = new OracleAnySql();
      oracleAnySql.setJdbcTemplate(jdbcTemplate);
      oracleAnySql.setAnyDDL(anyDDL);
      oracleAnySql.setAnyIdService(anyIdService);
      return oracleAnySql;
    } else {
      throw Exceptions.unchecked("No implementation for " + dbDetector.getVender() + " found");
    }
  }

  @Override
  public Class<?> getObjectType() {
    return AnySql.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
