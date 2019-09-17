package com.github.catstiger.anything.ddl;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.catstiger.anything.ddl.impl.H2AnyDDL;
import com.github.catstiger.anything.ddl.impl.MySqlAnyDDL;
import com.github.catstiger.anything.ddl.impl.OracleAnyDDL;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.sql.limit.DatabaseDetector;
import com.github.catstiger.common.util.Exceptions;

@Service
public class AnyDDLFactoryBean implements FactoryBean<AnyDDL> {
  @Autowired
  private JdbcTemplateProxy jdbcTemplate;

  @Autowired
  private DatabaseDetector dbDetector;

  @Value("${app.any.createFK}")
  private Boolean createForeignKey = false;

  @Override
  public AnyDDL getObject() throws Exception {
    if (dbDetector.isH2()) {
      H2AnyDDL h2AnyDDL = new H2AnyDDL();
      h2AnyDDL.setJdbcTemplate(jdbcTemplate);
      return h2AnyDDL;
    } else if (dbDetector.isMySql()) {
      MySqlAnyDDL mySqlAnyDDL = new MySqlAnyDDL();
      mySqlAnyDDL.setJdbcTemplate(jdbcTemplate);
      return mySqlAnyDDL;
    } else if (dbDetector.isOracle()) {
      OracleAnyDDL oracleAnyDDL = new OracleAnyDDL();
      oracleAnyDDL.setJdbcTemplate(jdbcTemplate);
      return oracleAnyDDL;
    } else {
      throw Exceptions.unchecked("No AnyDDL implementation for " + dbDetector.getVender());
    }
  }

  @Override
  public Class<?> getObjectType() {
    return AnyDDL.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
