package com.ruijie.dapengine.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruijie.dapengine.common.model.SubjectDTO;
import com.ruijie.dapengine.entity.SubjectEntity;
import com.ruijie.dapengine.mapper.SubjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * dap_sys_subject 表数据访问层。
 *
 * <p>通过 MyBatis-Plus {@link SubjectMapper} + {@code LambdaQueryWrapper} /
 * {@code LambdaUpdateWrapper} 实现 CRUD，不包含业务逻辑。
 * 所有 SQL 均携带 {@code tenant_id} 和 {@code app_code} 隔离条件。</p>
 */
public class SubjectRepository {

    private final SubjectMapper subjectMapper;
    private final String tenantId;
    private final String appCode;

    public SubjectRepository(SubjectMapper subjectMapper, String tenantId, String appCode) {
        this.subjectMapper = subjectMapper;
        this.tenantId = tenantId;
        this.appCode = appCode;
    }

    /**
     * 查询所有未逻辑删除的 Subject，不含字段列表，按主键升序。
     */
    public List<SubjectDTO> listActive() {
        LambdaQueryWrapper<SubjectEntity> wrapper = new LambdaQueryWrapper<SubjectEntity>()
                .eq(SubjectEntity::getTenantId, tenantId)
                .eq(SubjectEntity::getAppCode, appCode)
                .eq(SubjectEntity::getIsDelete, 0)
                .orderByAsc(SubjectEntity::getId);
        return subjectMapper.selectList(wrapper)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 按 code 查询 Subject（含已逻辑删除记录），用于判断新建 vs 更新场景。
     */
    public SubjectDTO findByCode(String code) {
        LambdaQueryWrapper<SubjectEntity> wrapper = new LambdaQueryWrapper<SubjectEntity>()
                .eq(SubjectEntity::getTenantId, tenantId)
                .eq(SubjectEntity::getAppCode, appCode)
                .eq(SubjectEntity::getCode, code)
                .last("LIMIT 1");
        SubjectEntity entity = subjectMapper.selectOne(wrapper);
        return entity == null ? null : toDTO(entity);
    }

    /**
     * 插入新 Subject 记录，返回生成的主键 id。
     *
     * @param createdBy 操作人 ID
     */
    public long insert(String code, String name, String description,
                       boolean isTree, int status, String createdBy) {
        LocalDateTime now = LocalDateTime.now();
        SubjectEntity entity = new SubjectEntity();
        entity.setTenantId(tenantId);
        entity.setAppCode(appCode);
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(description);
        entity.setIsTree(isTree ? 1 : 0);
        entity.setStatus(status);
        entity.setIsDelete(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);
        subjectMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 更新 Subject 基本信息（name/description/isTree/status）。
     *
     * @param updatedBy 操作人 ID
     */
    public void update(String code, String name, String description,
                       boolean isTree, int status, String updatedBy) {
        subjectMapper.update(null, new LambdaUpdateWrapper<SubjectEntity>()
                .eq(SubjectEntity::getTenantId, tenantId)
                .eq(SubjectEntity::getAppCode, appCode)
                .eq(SubjectEntity::getCode, code)
                .eq(SubjectEntity::getIsDelete, 0)
                .set(SubjectEntity::getName, name)
                .set(SubjectEntity::getDescription, description)
                .set(SubjectEntity::getIsTree, isTree ? 1 : 0)
                .set(SubjectEntity::getStatus, status)
                .set(SubjectEntity::getUpdatedAt, LocalDateTime.now())
                .set(SubjectEntity::getUpdatedBy, updatedBy));
    }

    /**
     * 逻辑删除 Subject（is_delete=1），仅删除未已删除的记录。
     *
     * @return 受影响行数，0 表示记录不存在或已删除
     */
    public int logicDelete(String code) {
        return subjectMapper.update(null, new LambdaUpdateWrapper<SubjectEntity>()
                .eq(SubjectEntity::getTenantId, tenantId)
                .eq(SubjectEntity::getAppCode, appCode)
                .eq(SubjectEntity::getCode, code)
                .eq(SubjectEntity::getIsDelete, 0)
                .set(SubjectEntity::getIsDelete, 1)
                .set(SubjectEntity::getUpdatedAt, LocalDateTime.now()));
    }

    /**
     * 更新 Subject 的 updated_at（字段配置发生变更时调用）。
     *
     * @param subjectId Subject 主键 ID
     */
    public void touchUpdatedAt(long subjectId) {
        subjectMapper.update(null, new LambdaUpdateWrapper<SubjectEntity>()
                .eq(SubjectEntity::getId, subjectId)
                .set(SubjectEntity::getUpdatedAt, LocalDateTime.now()));
    }

    // -------------------------------------------------------------------------
    // 私有辅助方法
    // -------------------------------------------------------------------------

    /**
     * 将 {@link SubjectEntity} 转换为 {@link SubjectDTO}。
     * schemaStatus 和 fields 由上层 Service 填充，此处不设置。
     */
    private SubjectDTO toDTO(SubjectEntity entity) {
        SubjectDTO dto = new SubjectDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setTree(entity.getIsTree() != null && entity.getIsTree() == 1);
        dto.setBuiltIn(entity.getIsBuiltIn() != null && entity.getIsBuiltIn() == 1);
        dto.setStatus(entity.getStatus() != null ? entity.getStatus() : 0);
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().toString().replace("T", " "));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().toString().replace("T", " "));
        }
        dto.setIsDelete(entity.getIsDelete());
        return dto;
    }
}
