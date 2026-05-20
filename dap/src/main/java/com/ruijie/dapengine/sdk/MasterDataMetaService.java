package com.ruijie.dapengine.sdk;

import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.SubjectDTO;

import java.util.List;

/**
 * 主数据元数据门面接口。
 */
public interface MasterDataMetaService {

    /** 获取单个主题定义与字段信息。*/
    SubjectDTO getSubject(String subject);

    /** 获取全部可用主题。*/
    List<SubjectDTO> listSubjects();

    /** 获取指定主题字段列表。*/
    List<FieldConfigDTO> listFields(String subject);

    /** 获取主题当前 schema 状态。*/
    SchemaStatus getSchemaStatus(String subject);

    /** 判断主题是否树形。*/
    boolean isTreeSubject(String subject);
}

