package com.ruijie.dapengine.sdk;

import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.SubjectDTO;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * {@link MasterDataMetaService} 默认实现，委托给 {@link MetadataConfigService}。
 */
@RequiredArgsConstructor
public class MasterDataMetaServiceImpl implements MasterDataMetaService {

    private final MetadataConfigService metadataConfigService;

    @Override
    public SubjectDTO getSubject(String subject) {
        return metadataConfigService.getFieldsBySubject(subject);
    }

    @Override
    public List<SubjectDTO> listSubjects() {
        return metadataConfigService.listSubjects();
    }

    @Override
    public List<FieldConfigDTO> listFields(String subject) {
        return metadataConfigService.getFieldsBySubject(subject).getFields();
    }

    @Override
    public SchemaStatus getSchemaStatus(String subject) {
        return metadataConfigService.getFieldsBySubject(subject).getSchemaStatus();
    }

    @Override
    public boolean isTreeSubject(String subject) {
        return metadataConfigService.getFieldsBySubject(subject).isTree();
    }
}

