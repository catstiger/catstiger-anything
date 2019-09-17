package com.github.catstiger.anything.oper.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.anything.designer.service.AnyDataModelService;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.BeanPropertyRowMapperEx;
import com.github.catstiger.common.util.Exceptions;
import com.github.catstiger.websecure.user.model.User;
import com.google.common.base.Preconditions;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

@Service
public class AnyXlsService {
  @Autowired
  private JdbcTemplateProxy jdbcTemplate;
  @Autowired
  private AnyDataModelService admService;
  
  @Autowired
  private AnySql anySql;
 
  /**
   * 导入Xls文件
   */
  @RequestMapping("/impXls")
  @ResponseBody
  public void impXls(MultipartFile file, Long dataModelId, User user) {
    if (file == null || file.isEmpty()) {
      throw new java.lang.IllegalArgumentException("文件不存在。");
    }

    Preconditions.checkNotNull(dataModelId);
    AnyDataModel model = admService.withFields(dataModelId);

    Workbook book = null;
    try {
      book = Workbook.getWorkbook(file.getInputStream());
      Sheet sheet = book.getSheet(0);
      int cols = sheet.getColumns();
      Map<Integer, AnyDataModelField> fields = new HashMap<Integer, AnyDataModelField>();

      for (int i = 0; i < cols; i++) {
        Cell cell = sheet.getCell(i, 0);
        String caption = cell.getContents();
        SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select()
            .append("WHERE data_model_id=? and (display_name=? or field_name=?)", new Object[] { model.getId(), caption, caption });
        AnyDataModelField field = (AnyDataModelField) jdbcTemplate.queryForObject(sqlReady.getSql(),
            new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sqlReady.getArgs());
        if (field != null && !field.getIsFile() && !field.getHidden()) {
          fields.put(i, field);
        }
      }

      int rows = sheet.getRows();
      for (int i = 1; i < rows; i++) {
        Cell[] row = sheet.getRow(i);
        this.rowHandler(model, row, fields, user);
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw Exceptions.unchecked(e.getMessage());
    } finally {
      if (book != null) {
        book.close();
      }
    }
  }
  
  private void rowHandler(AnyDataModel model, Cell[] row, Map<Integer, AnyDataModelField> fields, User user) {
    Map<String, Object> args = new HashMap<String, Object>();
    for (int j = 0; j < row.length; j++) {
      String value = row[j].getContents();
      AnyDataModelField field = fields.get(j);
      if (field != null) {
        if (!field.getIsForeign()) {
          args.put(field.getFieldName().toLowerCase(), value);
        } else { // 处理外键
          String sql = "SELECT ID FROM " + field.getRefTable() + " WHERE " + field.getRefDispFieldName() + "=?";
          List<Map<String, Object>> list = jdbcTemplate.query(sql, new Object[] { value }, new ColumnMapRowMapper());
          if (!list.isEmpty() && list.get(0) != null) {
            String v = (list.get(0).get("ID") != null) ? list.get(0).get("ID").toString() : null;
            args.put(field.getFieldName().toLowerCase(), v);
          }
        }
      }
    }
    if (user != null) {
      args.put(AnyDataModelConstants.SYS_COL_USER, user.getId());
    }
    anySql.insert(model.getRealTableName().toLowerCase(), args);
  }
}
