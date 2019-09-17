package com.github.catstiger.anything.json;

import java.io.IOException;
import java.lang.reflect.Type;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.catstiger.multipart.model.FileObject;
import com.github.catstiger.multipart.service.FileObjectService;

/**
 * 如果一个字段是一个文件的URL，那么SimpleFileJsonSerializer可以将这个字段转换为一个HTLM超级链接， 前提是这个url必须在FileObject中可以查询到
 * 
 * @author lizhenshan
 *
 */
public class SimpleFileJsonSerializer extends JsonSerializer<String> implements ObjectSerializer {
  @Override
  public void serialize(String url, JsonGenerator jsonGen, SerializerProvider provider) throws IOException, JsonProcessingException {
    if (StringUtils.isBlank(url)) {
      jsonGen.writeString(StringUtils.EMPTY);
      return;
    }

    WebApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
    if (ctx == null) {
      return;
    }
    FileObjectService fileObjectService = ctx.getBean(FileObjectService.class);
    if (fileObjectService != null) {
      FileObject fileObject = fileObjectService.get(url);

      String json = null;
      if (fileObject != null) {
        if (jsonGen instanceof XlsJsonGenerator) {
          json = fileObject.getName();
        } else {
          json = new StringBuilder(100).append("<a href='")
              .append(url)
              .append("' target='_blank'>")
              .append(fileObject.getName())
              .append("</a>").toString();
        }
      } else {
        json = url;
      }
      jsonGen.writeString(json);
    }
  }

  @Override
  public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
    if (object == null) {
      return;
    }
    String url = object.toString();
    WebApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
    if (ctx == null) {
      return;
    }
    FileObjectService fileObjectService = ctx.getBean(FileObjectService.class);
    SerializeWriter out = serializer.out;
    if (fileObjectService != null) {
      FileObject fileObject = fileObjectService.get(url);
      String json = new StringBuilder(100)
          .append("<a href='")
          .append(url)
          .append("' target='_blank'>")
          .append(fileObject.getName())
          .append("</a>").toString();
      out.write(json);
    }
  }

}
