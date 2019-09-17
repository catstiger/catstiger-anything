package com.github.catstiger.anything.code.controller;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.github.catstiger.anything.code.service.CodeService;
import com.github.catstiger.anything.designer.model.AnyDataModel;
import com.github.catstiger.common.sql.JdbcTemplateProxy;
import com.github.catstiger.common.util.ContentTypes;
import com.github.catstiger.common.util.Exceptions;
import com.github.catstiger.common.util.FileChannelUtil;
import com.github.catstiger.common.util.ZipUtil;
import com.github.catstiger.common.web.WebUtil;
import com.github.catstiger.common.web.controller.BaseController;
import com.github.catstiger.multipart.service.FileService;
import com.google.common.base.Preconditions;

/**
 * 导出源代码
 */
@Controller
@RequestMapping("/anything/code")
public class CodeController extends BaseController {
  @Autowired
  private JdbcTemplateProxy jdbcTemplate;
  @Autowired
  private CodeService codeService;
  @Autowired
  private FileService fileService;
  
  /**
   * 根据模板和{@link AnyDataModel}的设置，生成源代码，并打包为zip文件，输出到HttpOutputStream
   * @param dataModelId ID of the {@link AnyDataModel}
   * @param packageName 包名，所有生成的Java都位于这个package
   */
  @RequestMapping("/genCode")
  @ResponseBody
  public void genCode(@RequestParam("dataModelId") Long dataModelId, @RequestParam("packageName") String packageName) {
    try {
      Preconditions.checkNotNull(dataModelId, "必须选择一个数据模型。");
      Preconditions.checkNotNull(packageName, "必须输入包名。");
      AnyDataModel dataModel = jdbcTemplate.get(AnyDataModel.class, dataModelId);
      Preconditions.checkNotNull(dataModel, "数据模型不存在。");

      String path = fileService.getFSRoot();
      path += (dataModel.getDisplayName() + File.separator);

      if (!path.endsWith(File.separator)) {
        path += File.separator;
      }
      // 如果存放文件的目录不存在，则创建
      File root = new File(path);
      if (!root.exists()) {
        root.mkdirs();
      }
      if (!root.exists() || !root.isDirectory()) {
        throw Exceptions.unchecked("文件存放位置错误！");
      }
      // 生成源码
      codeService.generate(dataModel, new File(path), packageName, null);

      String source = path + "src" + File.separator;
      String dest = fileService.getFSRoot() + dataModel.getClassName().toLowerCase() + ".zip";
      String zip = ZipUtil.zip(source, dest, true, StringUtils.EMPTY);
      File zipFile = new File(zip);
      try {
        WebUtil.setFileDownloadHeader(getResponse(), zipFile.getName());
        getResponse().setContentLength(Long.valueOf(zipFile.length()).intValue());
        String contentType = ContentTypes.get("zip");
        getResponse().setContentType(contentType);
        FileChannelUtil fileChannel = new FileChannelUtil();
        fileChannel.read(zipFile, getResponse().getOutputStream());
      } finally {
        zipFile.delete();
        FileUtils.deleteDirectory(new File(path));
      }
    } catch (Exception e) {
      e.printStackTrace();
      renderJson(JSON.toJSONString(forExt(e.getMessage())));
    }
  }

}
