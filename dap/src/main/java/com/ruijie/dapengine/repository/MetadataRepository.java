package com.ruijie.dapengine.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.util.FieldSchemaHelper;
import com.ruijie.dapengine.entity.MetadataConfigEntity;
import com.ruijie.dapengine.mapper.MetadataConfigMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * dap_sys_metadata_config 表数据访问层。
 *
 * <p>通过 MyBatis-Plus {@link MetadataConfigMapper} + {@code LambdaQueryWrapper} /
 * {@code LambdaUpdateWrapper} 实现 CRUD，不包含业务逻辑。
 * 所有 SQL 均携带 {@code tenant_id} 和 {@code app_code} 隔离条件。</p>
 */
public class MetadataRepository {

    private final MetadataConfigMapper metadataConfigMapper;
    private final String tenantId;
    private final String appCode;

    public MetadataRepository(MetadataConfigMapper metadataConfigMapper, String tenantId, String appCode) {
        this.metadataConfigMapper = metadataConfigMapper;
        this.tenantId = tenantId;
        this.appCode = appCode;
    }

    /**
     * 查询指定 Subject 下的全部字段（含废弃），按 sort_order、id 升序。
     *
     * @param subjectId Subject 主键 ID
     */
    public List<FieldConfigDTO> findBySubjectId(long subjectId) {
        LambdaQueryWrapper<MetadataConfigEntity> wrapper = new LambdaQueryWrapper<MetadataConfigEntity>()
                .eq(MetadataConfigEntity::getTenantId, tenantId)
                .eq(MetadataConfigEntity::getAppCode, appCode)
                .eq(MetadataConfigEntity::getSubjectId, subjectId)
                .orderByAsc(MetadataConfigEntity::getSortOrder)
                .orderByAsc(MetadataConfigEntity::getId);
        return metadataConfigMapper.selectList(wrapper)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 按 subjectId + fieldName 查询字段记录（含废弃），用于 upsert 场景。
     */
    public FieldConfigDTO findBySubjectIdAndFieldName(long subjectId, String fieldName) {
        LambdaQueryWrapper<MetadataConfigEntity> wrapper = new LambdaQueryWrapper<MetadataConfigEntity>()
                .eq(MetadataConfigEntity::getTenantId, tenantId)
                .eq(MetadataConfigEntity::getAppCode, appCode)
                .eq(MetadataConfigEntity::getSubjectId, subjectId)
                .eq(MetadataConfigEntity::getFieldName, fieldName)
                .last("LIMIT 1");
        MetadataConfigEntity entity = metadataConfigMapper.selectOne(wrapper);
        return entity == null ? null : toDTO(entity);
    }

    /**
     * 插入新字段记录。
     *
     * @param isSystem  是否系统字段（仅作业务标记，不持久化到 DB）
     * @param createdBy 操作人 ID
     */
    public void insertField(long subjectId, String subjectCode, String subjectName,
                            String fieldName, String fieldType, Integer maxLength, String fieldLabel,
                            boolean required, String dictCode, int sortOrder,
                            boolean isSystem, String createdBy) {
        LocalDateTime now = LocalDateTime.now();
        MetadataConfigEntity entity = new MetadataConfigEntity();
        entity.setTenantId(tenantId);
        entity.setAppCode(appCode);
        entity.setSubjectId(subjectId);
        entity.setSubjectCode(subjectCode);
        entity.setSubjectName(subjectName);
        entity.setFieldName(fieldName);
        entity.setFieldType(fieldType);
        entity.setMaxLength(normalizeStoredMaxLength(maxLength));
        entity.setFieldLabel(fieldLabel);
        entity.setRequired(required ? 1 : 0);
        entity.setDictCode(dictCode);
        entity.setSortOrder(sortOrder);
        entity.setIsDelete(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);
        metadataConfigMapper.insert(entity);
    }

    /**
     * 更新已存在字段的可修改属性（fieldType/fieldLabel/required/dictCode/sortOrder）。
     *
     * @param fieldId   字段主键 ID
     * @param updatedBy 操作人 ID
     */
    public void updateField(long fieldId, String fieldType, String fieldLabel,
                            Integer maxLength, boolean required, String dictCode, int sortOrder, String updatedBy) {
        metadataConfigMapper.update(null, new LambdaUpdateWrapper<MetadataConfigEntity>()
                .eq(MetadataConfigEntity::getId, fieldId)
                .set(MetadataConfigEntity::getFieldType, fieldType)
                .set(MetadataConfigEntity::getMaxLength, normalizeStoredMaxLength(maxLength))
                .set(MetadataConfigEntity::getFieldLabel, fieldLabel)
                .set(MetadataConfigEntity::getRequired, required ? 1 : 0)
                .set(MetadataConfigEntity::getDictCode, dictCode)
                .set(MetadataConfigEntity::getSortOrder, sortOrder)
                .set(MetadataConfigEntity::getUpdatedAt, LocalDateTime.now())
                .set(MetadataConfigEntity::getUpdatedBy, updatedBy));
    }

    /**
     * 设置字段逻辑删除状态。
     *
     * @param isDelete  0=激活，1=废弃
     * @param updatedBy 操作人 ID
     */
    public void setIsDelete(long fieldId, int isDelete, String updatedBy) {
        metadataConfigMapper.update(null, new LambdaUpdateWrapper<MetadataConfigEntity>()
                .eq(MetadataConfigEntity::getId, fieldId)
                .set(MetadataConfigEntity::getIsDelete, isDelete)
                .set(MetadataConfigEntity::getUpdatedAt, LocalDateTime.now())
                .set(MetadataConfigEntity::getUpdatedBy, updatedBy));
    }

    /**
     * 查询 Subject 下所有有效字段（is_delete=0），用于 schemaStatus 计算。
     */
    public List<FieldConfigDTO> findActiveBySubjectId(long subjectId) {
        LambdaQueryWrapper<MetadataConfigEntity> wrapper = new LambdaQueryWrapper<MetadataConfigEntity>()
                .eq(MetadataConfigEntity::getTenantId, tenantId)
                .eq(MetadataConfigEntity::getAppCode, appCode)
                .eq(MetadataConfigEntity::getSubjectId, subjectId)
                .eq(MetadataConfigEntity::getIsDelete, 0)
                .orderByAsc(MetadataConfigEntity::getSortOrder)
                .orderByAsc(MetadataConfigEntity::getId);
        return metadataConfigMapper.selectList(wrapper)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // 私有辅助方法
    // -------------------------------------------------------------------------

    /**
     * 将 {@link MetadataConfigEntity} 转换为 {@link FieldConfigDTO}。
     * {@code system}（系统字段标志）由 {@code MetadataConfigService} 在内存中设置，此处不赋值。
     */
    private FieldConfigDTO toDTO(MetadataConfigEntity entity) {
        FieldConfigDTO dto = new FieldConfigDTO();
        dto.setId(entity.getId());
        dto.setFieldName(entity.getFieldName());
        dto.setFieldType(entity.getFieldType());
        dto.setMaxLength(FieldSchemaHelper.normalizeMaxLength(entity.getFieldType(), entity.getMaxLength()));
        dto.setFieldLabel(entity.getFieldLabel());
        dto.setRequired(entity.getRequired() != null && entity.getRequired() == 1);
        dto.setDictCode(entity.getDictCode());
        dto.setSortOrder(entity.getSortOrder() != null ? entity.getSortOrder() : 0);
        dto.setIsDelete(entity.getIsDelete() != null ? entity.getIsDelete() : 0);
        return dto;
    }

    private Integer normalizeStoredMaxLength(Integer maxLength) {
        return maxLength == null ? 0 : maxLength;
    }
}
