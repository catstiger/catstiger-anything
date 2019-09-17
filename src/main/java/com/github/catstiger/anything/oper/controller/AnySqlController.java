package com.github.catstiger.anything.oper.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.anything.designer.service.AnyDataModelFieldService;
import com.github.catstiger.anything.designer.service.AnyDataModelService;
import com.github.catstiger.anything.oper.service.AnySql;
import com.github.catstiger.anything.oper.service.AnyXlsService;
import com.github.catstiger.common.AppProps;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.sql.Page;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.filter.Operator;
import com.github.catstiger.common.sql.filter.QueryPart;
import com.github.catstiger.common.sql.mapper.BeanPropertyRowMapperEx;
import com.github.catstiger.common.util.ContentTypes;
import com.github.catstiger.common.web.WebObjectsHolder;
import com.github.catstiger.common.web.WebUtil;
import com.github.catstiger.common.web.controller.BaseController;
import com.github.catstiger.modules.api.LeaderService;
import com.github.catstiger.modules.api.StaffService;
import com.github.catstiger.modules.api.model.Staff;
import com.github.catstiger.multipart.model.FileObject;
import com.github.catstiger.multipart.service.FileObjectService;
import com.github.catstiger.websecure.user.UserHolder;
import com.github.catstiger.websecure.user.model.User;
import com.google.common.base.Preconditions;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

/**
 * 用于配合Anything系统，在AnyDataModel未生成源代码的情况下提供基本的增删改查操作
 * 
 * @author lizhenshan
 * 
 */
@Controller
@RequestMapping("/anything/sql")
@SuppressWarnings("unchecked")
public class AnySqlController extends BaseController {
  public static final int MAX_EXPORT_ROWS = 6000;
  
  @Autowired
  private AppProps appProps;
  @Autowired
  private AnySql anySql;
  @Autowired
  private AnyDataModelFieldService admfService;
  @Autowired
  private FileObjectService fileObjectService;

  @Autowired
  private JdbcTemplateProxy jdbcTemplate;
  @Autowired(required = false)
  private LeaderService leaderService;
  @Autowired(required = false)
  private StaffService staffService;
  @Autowired
  private AnyDataModelService admService;
  @Autowired
  private AnyXlsService anyXlsService;

  /**
   * 查询指定的AnyDataModel对象
   */
  @RequestMapping("/select")
  @ResponseBody
  public void select(AnyDataModel model) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(model.getId());

    model = admService.withFields(model.getId());
    Map<String, String[]> references = admfService.foreigns(model);

    List<QueryPart> queryParts = QueryPart.parse(getRequest());
    // 数据归属权限
    String privilegeQuery = this.buildPrivilegeQuerySnippets(model);
    try {
      Page page = page();
      page = anySql.pagedQuery(page(), model.getRealTableName().toLowerCase(), references, queryParts, getSortName(), getSortOrder(),
          privilegeQuery);

      // 数据转换
      List<Map<String, Object>> list = (List<Map<String, Object>>) page.getRows();
      convertResult(model, list);

      renderJson(JSON.toJSONString(page));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 创建数据级权限所用的SQL查询片段
   * 
   * @see AnyDataModelConstants#DATA_PRIV_SELF
   * @see AnyDataModelConstants#DATA_PRIV_DEPT
   * @see AnyDataModelConstants#DATA_PRIV_FIELD
   * @param anyDataModel 给出AnyDataModel对象
   * @return SQL
   */
  private String buildPrivilegeQuerySnippets(AnyDataModel anyDataModel) {
    User user = UserHolder.getUser();
    if (user == null) {
      return " AND (1<>1) "; // 没有登录的用户，不能访问
    }
    if (anyDataModel.getPrivType() != null && anyDataModel.getPrivType().length() >= AnyDataModelConstants.DATA_PRIV_OPEN.length()
        && anyDataModel.getPrivType().indexOf(AnyDataModelConstants.DATA_PRIV_OPEN) < 0) {
      StringBuilder snippets = new StringBuilder(" AND (1<>1");
      // 仅自己可见
      if (anyDataModel.getPrivType().indexOf(AnyDataModelConstants.DATA_PRIV_SELF) >= 0) {
        snippets.append(" OR " + AnyDataModelConstants.DEFAULT_TABLE_ALIAS + "." + AnyDataModelConstants.SYS_COL_USER + "=")
            .append(user.getId());
      }
      // 根据某个指向用户表的字段
      if (anyDataModel.getPrivType().indexOf(AnyDataModelConstants.DATA_PRIV_FIELD) >= 0
          && StringUtils.isNotBlank(anyDataModel.getPrivStr())) {
        appendPrivSqlBySpecField(anyDataModel, snippets, user);
      }

      // 领导可见
      if (anyDataModel.getPrivType().indexOf(AnyDataModelConstants.DATA_PRIV_LEADER) >= 0) {
        appendPrivSqlByLeader(snippets, user);
      }

      return snippets.append(")").toString();
    }

    return null;
  }
  
  private void appendPrivSqlByLeader(StringBuilder snippets, User user) {
    if (staffService == null || leaderService == null) {
      logger.warn("必须实现StaffService和LeaderService");
      return;
    }
    Staff staff = staffService.byUsername(user.getUsername());
    List<Staff> underling = leaderService.findUnderling(staff);
    if (CollectionUtils.isNotEmpty(underling)) {
      List<Long> ids = new ArrayList<Long>();
      for (Staff und : underling) {
        ids.add(und.getId());
      }
      snippets.append(" OR " + AnyDataModelConstants.DEFAULT_TABLE_ALIAS + "." + AnyDataModelConstants.SYS_COL_USER + " in (")
          .append(StringUtils.join(ids.iterator(), ",")).append(")");
    }
  }
  
  private void appendPrivSqlBySpecField(AnyDataModel anyDataModel, StringBuilder snippets, User user) {
    Long fieldId = null;
    try {
      fieldId = Long.valueOf(anyDataModel.getPrivStr().trim());
    } catch (Exception e) {
      //Do nothing
    }
    if (fieldId != null) {
      AnyDataModelField field = jdbcTemplate.get(AnyDataModelField.class, Long.valueOf(anyDataModel.getPrivStr().trim()));
      if (!field.getInputType().equals(AnyDataModelConstants.XTYPE_USERCOMBO)) {
        snippets.append(" OR " + AnyDataModelConstants.DEFAULT_TABLE_ALIAS_DOT)
        .append(field.getFieldName())
        .append(" like '%")
        .append(user.getId()).append("%'");
      } else {
        snippets.append(" OR " + AnyDataModelConstants.DEFAULT_TABLE_ALIAS + ".")
        .append(field.getFieldName())
        .append("=").append(user.getId());
      }
    }
  }

  /**
   * 导出Excel数据
   */
  @RequestMapping("/xls")
  @ResponseBody
  public void xls(AnyDataModel model) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(model.getId());

    model = admService.withFields(model.getId());
    Map<String, String[]> references = admfService.foreigns(model);

    List<QueryPart> queryParts = QueryPart.parse(getRequest());
    // 数据归属权限
    String privilegeQuery = this.buildPrivilegeQuerySnippets(model);
    try {
      Page page = page();
      page = anySql.pagedQuery(new Page(0, MAX_EXPORT_ROWS), model.getRealTableName().toLowerCase(), references, queryParts,
          getSortName(), getSortOrder(), privilegeQuery);

      // 数据转换
      List<Map<String, Object>> list = (List<Map<String, Object>>) page.getRows();
      convertResult(model, list);

      String contentType = ContentTypes.get("xls");
      WebObjectsHolder.getResponse().setContentType(contentType);
      WebUtil.setFileDownloadHeader(WebObjectsHolder.getResponse(), model.getDisplayName() + ".xls");

      WritableWorkbook book = Workbook.createWorkbook(WebObjectsHolder.getResponse().getOutputStream());
      WritableSheet sheet = book.createSheet(model.getDisplayName(), 0);

      SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append("WHERE data_model_id=?", model.getId())
          .orderBy("orders", "asc");
      List<AnyDataModelField> fields = jdbcTemplate.query(sqlReady.getSql(),
          new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), sqlReady.getArgs());
      // 标题行
      int coIndex = 0;
      for (AnyDataModelField field : fields) {
        if (!field.getFieldName().equals(AnyDataModelConstants.SYS_COL_PRIMARY)) {
          WritableFont font = new WritableFont(WritableFont.ARIAL, 12, WritableFont.BOLD);
          WritableCellFormat format = new WritableCellFormat(font);
          Label label = new Label(coIndex, 0, field.getDisplayName(), format);
          sheet.addCell(label);
          coIndex++;
        }
      }
      // 数据部分
      int rowIndex = 1;
      for (Map<String, Object> row : list) {
        coIndex = 0;
        for (AnyDataModelField field : fields) {
          if (!field.getFieldName().equals(AnyDataModelConstants.SYS_COL_PRIMARY)) {
            Object value = row.get(field.getFieldName());
            Label label = new Label(coIndex, rowIndex, (value == null ? "" : value.toString()));
            sheet.addCell(label);
            coIndex++;
          }
        }
        rowIndex++;
      }
      book.write();
      book.close();
      WebObjectsHolder.getResponse().getOutputStream().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 导入Xls文件
   */
  @RequestMapping("/impXls")
  @ResponseBody
  public Map<String, Object> impXls(MultipartFile file, @RequestParam("id") Long dataModelId) {
    if (file == null || file.isEmpty()) {
      return forExt("请选择一个execel文件。");
    }

    Preconditions.checkNotNull(dataModelId);
    try {
      anyXlsService.impXls(file, dataModelId, UserHolder.getUser());
      return forExt(true);
    } catch (Exception e) {
      return forExt(e.getMessage());
    }
  }

  /**
   * 执行树状数据结构查询，AnyDataModel必须指定一个AnyDataModelField的asTreeCol为true 树形查询不支持分页
   */
  @RequestMapping("/selectTree")
  @ResponseBody
  public void selectTree(AnyDataModel model) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(model.getId());
    model = admService.withFields(model.getId());
    String tableName = model.getRealTableName().toLowerCase();

    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append("WHERE data_model_id=? and as_tree_col=true",
        model.getId());
    AnyDataModelField treeCol = jdbcTemplate.queryForObject(sqlReady.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), model.getId());

    Map<String, String[]> references = admfService.foreigns(model);
    List<QueryPart> queryParts = QueryPart.parse(getRequest());

    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    // 数据归属权限
    String privilegeQuery = this.buildPrivilegeQuerySnippets(model);

    if (treeCol == null) { // 不是树形数据
      list = anySql.select(tableName, references, queryParts, getSortName(), getSortOrder(), privilegeQuery);
    } else {
      queryParts.add(new QueryPart(Operator.NULL, treeCol.getFieldName().toLowerCase(), null));

      // 分页查询只针对顶级
      list = anySql.select(tableName, references, queryParts, getSortName(), getSortOrder(), privilegeQuery);
      for (Map<String, Object> item : list) {
        queryChildren(model, item, tableName, treeCol, references);
      }
    }

    renderJson(JSON.toJSONString(list));
  }

  /**
   * 支持分页查询的Tree。分页查询只对顶层数据有效，子数据即时加载
   */
  @RequestMapping("/asyncTree")
  @ResponseBody
  public void asyncTree(AnyDataModel model, @RequestParam(AnyDataModelConstants.EXT_ROOT_PARAM) Long parentId) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(model.getId());
    // 连带字段信息查询AnyDataModel
    model = admService.withFields(model.getId());
    String tableName = model.getRealTableName().toLowerCase();
    // 查询作为属性列表的Field
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append("WHERE data_model_id=? and as_tree_col=true",
        model.getId());
    AnyDataModelField treeCol = jdbcTemplate.queryForObject(sqlReady.getSql(),
        new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class), model.getId());

    Map<String, String[]> references = admfService.foreigns(model);
    List<QueryPart> queryParts = QueryPart.parse(getRequest());
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

    // 数据归属权限
    String privilegeQuery = this.buildPrivilegeQuerySnippets(model);

    if (treeCol == null) { // 不是树形数据
      list = anySql.select(tableName, references, queryParts, getSortName(), getSortOrder(), privilegeQuery);
    } else {
      if (parentId == null) { // 上级分支为null
        queryParts.add(new QueryPart(Operator.NULL, treeCol.getFieldName().toLowerCase(), null));
      } else {
        queryParts.add(new QueryPart(Operator.EQ, treeCol.getFieldName().toLowerCase(), parentId));
      }
      list = anySql.select(tableName, references, queryParts, getSortName(), getSortOrder(), privilegeQuery);
      convertResult(model, list);
      // 查询每个记录是否有子节点
      for (Map<String, Object> item : list) {
        if (hasChildren(item, tableName, treeCol)) {
          item.put("leaf", false);
        } else {
          item.put("leaf", true);
          item.put("iconCls", "icon-empty");
        }
      }
    }

    renderJson(JSON.toJSONString(list));
  }

  private void queryChildren(AnyDataModel model, Map<String, Object> parent, String tableName, AnyDataModelField treeCol,
      Map<String, String[]> references) {
    List<QueryPart> queryParts = QueryPart.parse(getRequest());
    queryParts.add(new QueryPart(Operator.EQ, treeCol.getFieldName().toLowerCase(), parent.get("ID")));
    // 数据归属权限
    String privilegeQuery = this.buildPrivilegeQuerySnippets(model);

    List<Map<String, Object>> children = anySql.select(tableName, references, queryParts, getSortName(), getSortOrder(), privilegeQuery);
    if (children.size() > 0) {
      for (Map<String, Object> item : children) {
        queryChildren(model, item, tableName, treeCol, references);
      }
      parent.put("rows", children); // 这里与reader的root属性一致
      parent.put("leaf", false);
      parent.put("expanded", true);
    } else {
      parent.put("iconCls", "icon-empty");
      parent.put("leaf", true);
    }
  }

  private Boolean hasChildren(Map<String, Object> parent, String tableName, AnyDataModelField treeCol) {
    StringBuilder sql = new StringBuilder(100).append("SELECT COUNT(*) FROM ").append(tableName).append(" WHERE ")
        .append(treeCol.getFieldName()).append("=").append(parent.get("ID"));

    return jdbcTemplate.queryForObject(sql.toString(), Long.class) > 0L;
  }

  /**
   * 向指定的AnyDataModel对象所代表的数据表中插入一条数据
   */
  @RequestMapping("/insert")
  @ResponseBody
  public Map<String, Object> insert(@RequestParam("dataModelId") Long dataModelId) {
    Preconditions.checkNotNull(dataModelId);

    AnyDataModel model = admService.withFields(dataModelId);
    Map<String, Object> args = new HashMap<String, Object>();

    for (Enumeration<String> e = getRequest().getParameterNames(); e.hasMoreElements();) {
      String fieldName = e.nextElement();
      if (admfService.isField(model, fieldName)) {
        logger.debug("{} is field", fieldName);
        args.put(fieldName.toLowerCase(), getRequest().getParameter(fieldName));
      }
    }

    User user = UserHolder.getUser();
    if (user != null) {
      args.put(AnyDataModelConstants.SYS_COL_USER, user.getId());
    }

    try {
      Long id = anySql.insert(model.getRealTableName().toLowerCase(), args);
      Map<String, Object> data = anySql.load(model.getRealTableName().toLowerCase(), id);
      return forExt(data, true);
    } catch (Exception e) {
      e.printStackTrace();
      return forExt("操作失败！");
    }
  }

  /**
   * 根据ID，加载数据
   */
  @RequestMapping("/load")
  @ResponseBody
  public void load(AnyDataModel model) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(model.getId());
    Preconditions.checkNotNull(getRequest().getParameter("idToLoad"));

    Long idToLoad = Long.valueOf(getRequest().getParameter("idToLoad"));
    model = admService.withFields(model.getId());
    Map<String, Object> row = anySql.load(model.getRealTableName(), idToLoad);
    // 执行格式转换
    for (AnyDataModelField field : model.getFields()) {
      String key = field.getFieldName().toLowerCase();

      if (row.containsKey(key)) {
        Object value = row.get(key);
        if (value == null) {
          continue;
        }
        if (StringUtils.equalsIgnoreCase(field.getDataType(), AnyDataModelConstants.DATA_TYPE_DATE)) { // FIXME:转换日期类型（目前不支持时间类型）
          row.put(key, new DateTime((Date) value).toString(appProps.getDate().getFormat()));
        }
      }
    }
    renderJson(JSON.toJSONString(forExt(row, true)));
  }

  /**
   * 更新数据
   */
  @RequestMapping("/update")
  @ResponseBody
  public Map<String, Object> update(@RequestParam("dataModelId") Long dataModelId) {
    Preconditions.checkNotNull(dataModelId);
    AnyDataModel model = admService.withFields(dataModelId);

    String idStr = getRequest().getParameter(AnyDataModelConstants.SYS_COL_PRIMARY);
    if (idStr == null) {
      idStr = getRequest().getParameter(AnyDataModelConstants.SYS_COL_PRIMARY.toUpperCase());
    }
    Preconditions.checkNotNull(idStr, "更新数据，ID是必须的！");

    Long id = Long.valueOf(idStr);

    Map<String, Object> args = new HashMap<String, Object>();

    for (Enumeration<String> e = getRequest().getParameterNames(); e.hasMoreElements();) {
      String fieldName = e.nextElement();
      if (admfService.isField(model, fieldName) && !StringUtils.equalsIgnoreCase(AnyDataModelConstants.SYS_COL_PRIMARY, fieldName)) {
        args.put(fieldName.toLowerCase(), getRequest().getParameter(fieldName));
      }
    }

    User user = UserHolder.getUser();
    if (user != null) {
      args.put(AnyDataModelConstants.SYS_COL_USER, user.getId());
    }

    try {
      anySql.update(model.getRealTableName(), id, args);
      Map<String, Object> data = anySql.load(model.getRealTableName().toLowerCase(), id);
      return forExt(data, true);
    } catch (Exception e) {
      e.printStackTrace();
      return forExt("操作失败！");
    }
  }

  /**
   * 执行删除操作，可以删除AnyDataModel代表的任何表中的数据
   */
  @RequestMapping("/delete")
  @ResponseBody
  public Map<String, Object> delete(AnyDataModel model) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(model.getId());
    Preconditions.checkNotNull(getRequest().getParameter("idToLoad"));

    Long idToLoad = Long.valueOf(getRequest().getParameter("idToLoad"));
    model = admService.withFields(model.getId());

    try {
      anySql.delete(model.getRealTableName(), idToLoad);
      return forExt("删除成功！", true);
    } catch (Exception e) {
      e.printStackTrace();
      return forExt("删除失败！");
    }
  }

  /**
   * 将查询结果转换为Ext JS需要的数据格式
   */
  private void convertResult(AnyDataModel model, List<Map<String, Object>> results) {
    for (Map<String, Object> row : results) {
      for (AnyDataModelField field : model.getFields()) {
        String key = field.getFieldName().toUpperCase();
        // 转换URL为FileObject
        if (AnyDataModelConstants.XTYPE_FILEUPLOAD.equalsIgnoreCase(field.getInputType())) {
          if (row.containsKey(key)) {
            Object value = row.get(key);
            if (value != null) {
              FileObject fileObject = fileObjectService.get(value.toString());
              row.put(key, fileObject);
            }
          }
        }
      }
    }
  }
}
