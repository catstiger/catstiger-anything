package com.github.catstiger.anything.code.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.code.CodeConstants;
import com.github.catstiger.anything.designer.model.AnyCmp;
import com.github.catstiger.anything.designer.model.AnyCt;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.anything.designer.service.AnyCtService;
import com.github.catstiger.anything.designer.service.AnyDataModelFieldService;
import com.github.catstiger.anything.designer.service.AnyDataModelService;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.Mappers;
import com.github.catstiger.common.web.ui.FreeMarkerService;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

@Service
public class CodeService {
  private Logger logger = LoggerFactory.getLogger(CodeService.class);
  
  @Autowired
  private FreeMarkerService freeMarkerService;
  @Autowired
  private JdbcTemplateProxy jdbcTemplate;
  @Autowired
  private AnyCtService anyCtService;
  @Autowired
  private AnyDataModelFieldService anyDataModelFieldService;
  @Autowired
  private AnyDataModelService anyDataModelService;

  /**
   * 根据{@link AnyDataModel}, 生成单个源代码文件,保存在指定的目录下
   * @param dataModel Instance of AnyDataModel
   * @param root 存放源代码的根目录
   * @param rootPackage 包名
   * @param which 生成哪个文件，{@link CodeConstants}
   */
  @Transactional
  public void generate(AnyDataModel dataModel, File root, String rootPackage, String[] which) {
    Preconditions.checkNotNull(dataModel);
    Preconditions.checkNotNull(dataModel.getId());

    dataModel = jdbcTemplate.get(AnyDataModel.class, dataModel.getId());

    // 如果有一对多字段，并且多的一方还没有代码，则先生成多的一方
    if (dataModel.getHasOneToMany()) {
      doWithOneToMany(dataModel, root, rootPackage, which);
    }

    dataModel.setPackageName(rootPackage);
    // 加载组件容器和组件
    List<AnyCt> cts = anyCtService.loadByDataModel(dataModel.getId(), true);
    dataModel.setAnyCts(cts);
    for (Iterator<AnyCt> itr = cts.iterator(); itr.hasNext();) {
      AnyCt anyCt = itr.next();
      anyCt.setDataModel(dataModel);
      for (Iterator<AnyCmp> cmpItr = anyCt.getAnyCmps().iterator(); cmpItr.hasNext();) {
        AnyCmp anyCmp = cmpItr.next();
        if (anyCmp.getFieldId() != null) {
          anyCmp.setField(anyDataModelFieldService.get(anyCmp.getFieldId()));
          //加载引用的字段显示属性
          if (anyCmp.getField().getRefFieldDisplay() != null
              && anyCmp.getField().getRefFieldDisplay().getId() != null) {
            anyCmp.getField()
                .setRefFieldDisplay(anyDataModelFieldService.get(anyCmp.getField().getRefFieldDisplay().getId()));
          }
          //加载引用字段对应的DataModel
          if (anyCmp.getField().getRefDataModel() != null && anyCmp.getField().getRefDataModel().getId() != null) {
            anyCmp.getField().setRefDataModel(anyDataModelService.get(anyCmp.getField().getRefDataModel().getId()));
          }
          anyCmp.setSimpleField(anyDataModelFieldService.makeDetached(anyCmp.getFieldId()));
        }
      }
    }
    // 加载字段
    List<Long> fieldIds = jdbcTemplate.queryForList(
        "select id from any_data_model_field where data_model_id=? order by orders asc", Long.class, dataModel.getId());
    List<AnyDataModelField> fields = new ArrayList<>(fieldIds.size());
    for (Long fieldId : fieldIds) {
      fields.add(anyDataModelFieldService.makeDetached(fieldId));
    }
    dataModel.setFields(fields);
    
    writeFiles(dataModel, root, which);
    jdbcTemplate.update("update any_data_model set has_code=true where id=?", dataModel.getId());
  }
  
  private void doWithOneToMany(AnyDataModel dataModel, File root, String rootPackage, String[] which) {
    SQLReady sqlReady = new SQLRequest(AnyDataModelField.class, true).select()
        .append("WHERE data_model_id=?", dataModel.getId()).orderBy("orders", "asc");
    List<AnyDataModelField> fields = jdbcTemplate.query(sqlReady.getSql(), Mappers.byClass(AnyDataModelField.class),
        sqlReady.getArgs());
    dataModel.setFields(fields);

    for (AnyDataModelField field : fields) {
      if (AnyDataModelConstants.XTYPE_ONETOMANY.equals(field.getInputType()) && field.getReverseDataModel() != null) {
        if (!field.getReverseDataModel().getHasCode()) {
          generate(field.getReverseDataModel(), root, rootPackage, which);
        }
      }
    }
  }
  
  private void writeFiles(AnyDataModel dataModel, File root, String[] which) {
    if (ArrayUtils.isEmpty(which)) {
      saveJavaModel(dataModel, root.getAbsolutePath());
      saveJavaService(dataModel, root.getAbsolutePath());
      saveJavaAction(dataModel, root.getAbsolutePath());
      saveJsForm(dataModel, root.getAbsolutePath());
      saveJsView(dataModel, root.getAbsolutePath());
      saveJsModel(dataModel, root.getAbsolutePath());
      saveJsStore(dataModel, root.getAbsolutePath());
      
      writeExt6Files(dataModel, root.getAbsolutePath());
    } else {
      for (String type : which) {
        if (StringUtils.equals(type, CodeConstants.TYPE_JAVA_CONTROLLER)) {
          saveJavaAction(dataModel, root.getAbsolutePath());
          // saveFtlView(dataModel, root.getAbsolutePath());
        } else if (StringUtils.equals(type, CodeConstants.TYPE_JAVA_SERVICE)) {
          saveJavaAction(dataModel, root.getAbsolutePath());
        } else if (StringUtils.equals(type, CodeConstants.TYPE_JAVA_MODEL)) {
          saveJavaAction(dataModel, root.getAbsolutePath());
        } else if (StringUtils.equals(type, CodeConstants.TYPE_JS_MODEL)) {
          saveJsModel(dataModel, root.getAbsolutePath());
        } else if (StringUtils.equals(type, CodeConstants.TYPE_JS_STORE)) {
          saveJsStore(dataModel, root.getAbsolutePath());
        } else if (StringUtils.equals(type, CodeConstants.TYPE_JS_FORM)) {
          saveJsForm(dataModel, root.getAbsolutePath());
        } else if (StringUtils.equals(type, CodeConstants.TYPE_JS_VIEW)) {
          saveJsView(dataModel, root.getAbsolutePath());
        }
      }
    }
  }

  private void saveJavaModel(AnyDataModel dataModel, String fileRoot) {
    File file = getJavaFile(dataModel, fileRoot, CodeConstants.TYPE_JAVA_MODEL);
    String code = freeMarkerService.processTemplate("any_java_model.ftl", dataModel);
    write(file, code);
  }

  private void saveJavaService(AnyDataModel dataModel, String fileRoot) {
    File file = getJavaFile(dataModel, fileRoot, CodeConstants.TYPE_JAVA_SERVICE);
    String code = freeMarkerService.processTemplate("any_java_service.ftl", dataModel);
    write(file, code);
  }

  private void saveJavaAction(AnyDataModel dataModel, String fileRoot) {
    File file = getJavaFile(dataModel, fileRoot, CodeConstants.TYPE_JAVA_CONTROLLER);
    String code = freeMarkerService.processTemplate("any_java_controller.ftl", dataModel);
    write(file, code);
  }

  private void saveJsForm(AnyDataModel dataModel, String fileRoot) {
    File file = getJsFile(dataModel, fileRoot, CodeConstants.TYPE_JS_FORM);
    dataModel.setAllXtypes(AnyDataModelConstants.allXtypes());
    String code = freeMarkerService.processTemplate("any_js_form.ftl", dataModel);
    write(file, code);
  }

  private void saveJsModel(AnyDataModel dataModel, String fileRoot) {
    File file = getJsFile(dataModel, fileRoot, CodeConstants.TYPE_JS_MODEL);
    String code = freeMarkerService.processTemplate("any_js_model.ftl", dataModel);
    write(file, code);
  }

  private void saveJsStore(AnyDataModel dataModel, String fileRoot) {
    File file = getJsFile(dataModel, fileRoot, CodeConstants.TYPE_JS_STORE);
    String code = freeMarkerService.processTemplate("any_js_store.ftl", dataModel);
    write(file, code);
  }

  private void saveJsView(AnyDataModel dataModel, String fileRoot) {
    if (StringUtils.equals(dataModel.getUiStyle(), AnyDataModelConstants.UI_STYLE_FORM)) {
      return;
    }
    File file = getJsFile(dataModel, fileRoot, CodeConstants.TYPE_JS_VIEW);
    String code = null;
    if (StringUtils.equals(dataModel.getUiStyle(), AnyDataModelConstants.UI_STYLE_TREEGRID_FORM)) {
      code = freeMarkerService.processTemplate("any_js_view_tree.ftl", dataModel);
    } else {
      code = freeMarkerService.processTemplate("any_js_view_grid.ftl", dataModel);
    }
    write(file, code);
  }

  private File getJavaFile(AnyDataModel dataModel, String fileRoot, String type) {
    String codeRoot = getCodeRoot(fileRoot); // 源代码目录
    // 加入了包的源代码目录
    codeRoot += ("java" + File.separator);
    String location = getPackagePath(codeRoot, dataModel.getPackageName());
    // 加入了子包和类名
    StringBuilder path = new StringBuilder(200).append(location).append(File.separator)
        .append(dataModel.getClassName().toLowerCase()).append(File.separator).append(CodeConstants.PACKAGES.get(type))
        .append(File.separator);

    new File(path.toString()).mkdirs(); // 创建目录
    path.append(dataModel.getClassName()).append(CodeConstants.SUFFIXES.get(type));

    return new File(path.toString());
  }

  private File getJsFile(AnyDataModel dataModel, String fileRoot, String type) {
    String codeRoot = getCodeRoot(fileRoot); // 源代码目录

    StringBuilder path = new StringBuilder(200).append(codeRoot).append("resources/static/js/ext-4/app/");
    // Type的最后一个单词作为目录
    List<String> types = Splitter.on("_").splitToList(type);
    path.append(types.get(types.size() - 1)).append(File.separator);

    new File(path.toString()).mkdirs();
    path.append(dataModel.getClassName()).append(CodeConstants.SUFFIXES.get(type));

    return new File(path.toString());
  }
  
  private String getExt6File(AnyDataModel dataModel, String fileRoot) {
    String codeRoot = getCodeRoot(fileRoot); // 源代码目录

    StringBuilder path = new StringBuilder(200).append(codeRoot).append("resources/static/js/ext-6/app/view/")
        .append(dataModel.getClassName().toLowerCase()).append(File.separator);
    
    new File(path.toString()).mkdirs();
    return path.toString();
  }
  
  private void writeExt6Files(AnyDataModel dataModel, String fileRoot) {
    String path = getExt6File(dataModel, fileRoot);
    
    String store = freeMarkerService.processTemplate("any_js_ext6_store.ftl", dataModel);
    File storeFile = new File(path + dataModel.getClassName() + "Store.js");
    write(storeFile, store);
    logger.info("生成Ext6 {} \n {}", storeFile, store);
    
    String form = freeMarkerService.processTemplate("any_js_ext6_form.ftl", dataModel);
    write(new File(path + dataModel.getClassName() + "Form.js"), form);
    
    String view = freeMarkerService.processTemplate("any_js_ext6_view_grid.ftl", dataModel);
    write(new File(path + dataModel.getClassName() + ".js"), view);
    
    String ctrl = freeMarkerService.processTemplate("any_js_ext6_controller.ftl", dataModel);
    write(new File(path + dataModel.getClassName() + "Controller.js"), ctrl);
  }

  /**
   * 写源代码文件
   * 
   * @param file 文件位置
   * @param code 源代码
   */
  private void write(File file, String code) {
    Writer writer = null;
    try {
      FileOutputStream writerStream = new FileOutputStream(file);
      writer = new BufferedWriter(new OutputStreamWriter(writerStream, "UTF-8"));
      writer.write(code);
      writer.flush();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

  }

  /**
   * 创建一个符合Maven规范的源代码目录
   * 
   * @param fileRoot 根目录
   */
  private String getCodeRoot(String fileRoot) {
    return new StringBuilder(100).append(fileRoot).append(File.separator).append("src").append(File.separator)
        .append("main").append(File.separator).toString();
  }

  private String getPackagePath(String codeRoot, String rootPackage) {
    if (StringUtils.isBlank(rootPackage)) {
      return codeRoot;
    }
    if (!codeRoot.endsWith(File.separator)) {
      codeRoot += File.separator;
    }

    String[] names = StringUtils.split(rootPackage, ".");
    StringBuilder packagePath = new StringBuilder(200).append(codeRoot);
    for (String name : names) {
      packagePath.append(name).append(File.separator);
    }
    return packagePath.toString();
  }
}
