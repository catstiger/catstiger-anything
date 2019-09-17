package com.github.catstiger.anything;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.junit.Test;

import com.github.catstiger.common.sql.NamingStrategy;
import com.github.catstiger.common.sql.naming.SnakeCaseNamingStrategy;
import com.github.catstiger.common.sql.sync.DDLExecutor;
import com.github.catstiger.common.sql.sync.executor.WriterDDLExecutor;
import com.github.catstiger.common.sql.sync.mysql.MySqlSyncOperator;
import com.github.catstiger.common.sql.sync.mysql.MySqlSyncOperator.DbConnInfo;

public class DDLGenerator {
  public static final String DB_DRIVER =  "com.mysql.jdbc.Driver";
  public static final String DB_URL = "jdbc:mysql://localhost:3306/mysql";
  public static final String DB_USER = "root";
  public static final String DB_PWD = "root";
  
  @Test
  public void testOpe() throws IOException {
    DbConnInfo connInfo = new DbConnInfo();
    connInfo.setDriver(DB_DRIVER);
    connInfo.setUrl(DB_URL);
    connInfo.setUser(DB_USER);
    connInfo.setPassword(DB_PWD);
    
    NamingStrategy namingStrategy = new SnakeCaseNamingStrategy();
    Writer writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(new File(System.getProperty("user.home") + "/db.sql")));
      DDLExecutor executor = new WriterDDLExecutor(writer);
      MySqlSyncOperator.sync(connInfo, executor, namingStrategy, false, "com.github.catstiger.**.model");
      writer.flush();
    } finally {
      writer.close();
    }
  }
}