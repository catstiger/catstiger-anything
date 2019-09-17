package com.github.catstiger.anything.designer.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.catstiger.anything.AnyDataModelConstants;
import com.github.catstiger.anything.designer.model.AnyCmp;
import com.github.catstiger.anything.designer.model.AnyCt;
import com.github.catstiger.anything.designer.model.AnyDataModelField;
import com.github.catstiger.common.model.KeyValue;
import com.github.catstiger.common.service.GenericsService;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.BeanPropertyRowMapperEx;
import com.google.common.base.Preconditions;

@Service
public class AnyCtService extends GenericsService<AnyCt> {
  @Autowired
  private AnyCmpService anyCmpService;
  @Autowired
  private AnyDataModelFieldService anyDataModelFieldService;

  /**
   * 保存一组表单容器对象，返回组件ID和实体ID的对应关系
   */
  @Transactional
  public List<KeyValue<String, String>> save(List<AnyCt> anyCts) {
    Preconditions.checkNotNull(anyCts);
    // 记录界面元素ID和后台数据ID的对应关系，方便前台删除等操作
    List<KeyValue<String, String>> cmpIds = new ArrayList<KeyValue<String, String>>();
    for (AnyCt anyCt : anyCts) {
      anyCt = merge(anyCt);

      if (anyCt.getAnyCmps() != null) {
        for (AnyCmp anyCmp : anyCt.getAnyCmps()) {
          anyCmp.setAnyCt(anyCt);
          anyCmp = anyCmpService.merge(anyCmp);
        }
      }

      cmpIds.add(new KeyValue<String, String>(anyCt.getCmpId(), anyCt.getId().toString()));
      for (AnyCmp anyCmp : anyCt.getAnyCmps()) {
        cmpIds.add(new KeyValue<String, String>(anyCmp.getCmpId(), anyCmp.getId().toString()));
      }
    }

    return cmpIds;
  }

  /**
   * 按照AnyDataModel加载表单定义
   */
  public List<AnyCt> loadByDataModel(Long dataModelId, Boolean incHidden) {
    Preconditions.checkNotNull(dataModelId);
    // 表单容器
    SQLReady sqlReady = new SQLRequest(AnyCt.class, true).select().append(" WHERE data_model_id=? ", dataModelId).orderBy("orders", "asc");
    List<AnyCt> cts = jdbcTemplate.query(sqlReady.getSql(), new BeanPropertyRowMapperEx<AnyCt>(AnyCt.class), sqlReady.getArgs());
    // 加载容器中的组件
    for (AnyCt anyCt : cts) {
      sqlReady = new SQLRequest(AnyCmp.class, true).select().append("WHERE any_ct_id=?", anyCt.getId()).orderBy("orders", "asc");
      List<AnyCmp> anyCmps = jdbcTemplate.query(sqlReady.getSql(), new BeanPropertyRowMapperEx<AnyCmp>(AnyCmp.class), sqlReady.getArgs());
      for (AnyCmp anyCmp : anyCmps) {
        AnyDataModelField field = anyCmp.getField();
        if (field != null && field.getId() != null) {
          anyCmp.setSimpleField(anyDataModelFieldService.makeDetached(field.getId()));
        }
      }

      anyCt.setAnyCmps(anyCmps);
    }
    // 加载隐藏字段
    if (incHidden) { // 如果包括隐藏字段，在表单设计的适合，没有包含隐藏字段和系统字段，在实际使用中应该加入
      sqlReady = new SQLRequest(AnyDataModelField.class, true).select().append(" WHERE data_model_id=? ", dataModelId)
          .append(" and (input_type=? or is_sys=true) ", AnyDataModelConstants.XTYPE_HIDDEN).orderBy("orders", "asc");

      List<AnyDataModelField> fields = jdbcTemplate.query(sqlReady.getSql(), new BeanPropertyRowMapperEx<AnyDataModelField>(AnyDataModelField.class),
          sqlReady.getArgs());

      List<AnyCmp> cmps = new ArrayList<AnyCmp>(fields.size());
      for (AnyDataModelField field : fields) {
        AnyDataModelField f = new AnyDataModelField();
        BeanUtils.copyProperties(field, f, new String[] { "dataModel", "refDataModel", "refField", "refFieldDisplay" });

        AnyCmp anyCmp = new AnyCmp();
        anyCmp.setField(f);
        anyCmp.setSimpleField(f);
        anyCmp.setOrders(0);
        cmps.add(anyCmp);
      }
      // 将所有的AnyCmp加入一个Container中
      AnyCt ct = new AnyCt();
      ct.setCols(cmps.size());
      ct.setAnyCmps(cmps);
      cts.add(ct);
    }

    return cts;
  }

}
