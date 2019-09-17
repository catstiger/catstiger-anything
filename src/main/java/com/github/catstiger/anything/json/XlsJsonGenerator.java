package com.github.catstiger.anything.json;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;


public class XlsJsonGenerator extends JsonGenerator {
  private String str;
  
  public XlsJsonGenerator() {
    
  }
  
  @Override
  public void writeString(String content) throws IOException, JsonGenerationException {
    this.str = content;
  }
  

  @Override
  public void writeString(SerializableString arg0) throws IOException, JsonGenerationException {
  }
  
  @Override
  public void writeString(char[] arg0, int arg1, int arg2) throws IOException, JsonGenerationException {
  }
  
  public String getStr() {
    return str;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public JsonGenerator disable(Feature arg0) {
    
    return null;
  }

  @Override
  public JsonGenerator enable(Feature arg0) {
    
    return null;
  }

  @Override
  public void flush() throws IOException {
    
    
  }

  @Override
  public ObjectCodec getCodec() {
    
    return null;
  }

  @Override
  public int getFeatureMask() {
    
    return 0;
  }

  @Override
  public JsonStreamContext getOutputContext() {
    
    return null;
  }

  @Override
  public boolean isClosed() {
    
    return false;
  }

  @Override
  public boolean isEnabled(Feature arg0) {
    
    return false;
  }

  @Override
  public JsonGenerator setCodec(ObjectCodec arg0) {
    
    return null;
  }

  @Override
  public JsonGenerator setFeatureMask(int arg0) {
    
    return null;
  }

  @Override
  public JsonGenerator useDefaultPrettyPrinter() {
    
    return null;
  }

  @Override
  public Version version() {
    
    return null;
  }

  @Override
  public int writeBinary(Base64Variant arg0, InputStream arg1, int arg2) throws IOException, JsonGenerationException {
    
    return 0;
  }

  @Override
  public void writeBinary(Base64Variant arg0, byte[] arg1, int arg2, int arg3) throws IOException,
      JsonGenerationException {
    
    
  }

  @Override
  public void writeBoolean(boolean arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeEndArray() throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeEndObject() throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeFieldName(String arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeFieldName(SerializableString arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeNull() throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeNumber(int arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeNumber(long arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeNumber(BigInteger arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeNumber(double arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeNumber(float arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeNumber(BigDecimal arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeNumber(String arg0) throws IOException, JsonGenerationException, UnsupportedOperationException {
    
    
  }

  @Override
  public void writeObject(Object arg0) throws IOException, JsonProcessingException {
    
    
  }

  @Override
  public void writeRaw(String arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeRaw(char arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeRaw(String arg0, int arg1, int arg2) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeRaw(char[] arg0, int arg1, int arg2) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeRawUTF8String(byte[] arg0, int arg1, int arg2) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeRawValue(String arg0) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeRawValue(String arg0, int arg1, int arg2) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeRawValue(char[] arg0, int arg1, int arg2) throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeStartArray() throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeStartObject() throws IOException, JsonGenerationException {
    
    
  }

  @Override
  public void writeTree(TreeNode arg0) throws IOException, JsonProcessingException {
    
    
  }

  @Override
  public void writeUTF8String(byte[] arg0, int arg1, int arg2) throws IOException, JsonGenerationException {
    
    
  }


}
