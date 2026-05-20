package com.ruijie.dapengine.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruijie.dapengine.entity.SyncConfigEntity;
import com.ruijie.dapengine.mapper.SyncConfigMapper;

import java.time.LocalDateTime;

/**
 * dap_sys_sync_config 表数据访问层。
 *
 * <p>每个 Subject 最多保存一条同步配置（{@code uk_sync_subject}），
 * save() 按 subjectId 唯一键实现 INSERT-or-UPDATE 语义（先 find 再 insert/update）。</p>
 */
public class SyncConfigRepository {

    private final SyncConfigMapper syncConfigMapper;
    private final String tenantId;
    private final String appCode;

    public SyncConfigRepository(SyncConfigMapper syncConfigMapper, String tenantId, String appCode) {
        this.syncConfigMapper = syncConfigMapper;
        this.tenantId = tenantId;
        this.appCode = appCode;
    }

    /**
     * 按 subjectCode 查询未逻辑删除的同步配置。
     *
     * @return 配置实体，不存在时返回 {@code null}
     */
    public SyncConfigEntity findBySubjectCode(String subjectCode) {
        LambdaQueryWrapper<SyncConfigEntity> wrapper = new LambdaQueryWrapper<SyncConfigEntity>()
                .eq(SyncConfigEntity::getTenantId, tenantId)
                .eq(SyncConfigEntity::getAppCode, appCode)
                .eq(SyncConfigEntity::getSubjectCode, subjectCode)
                .eq(SyncConfigEntity::getIsDelete, 0)
                .last("LIMIT 1");
        return syncConfigMapper.selectOne(wrapper);
    }

    /**
     * 保存同步配置：若不存在则 INSERT，否则按主键 UPDATE（不覆盖 created_by、created_at）。
     *
     * @param entity 待保存实体（datasourceConfig 中敏感字段已由调用方加密）
     */
    public void save(SyncConfigEntity entity) {
        SyncConfigEntity existing = findBySubjectCode(entity.getSubjectCode());
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            entity.setTenantId(tenantId);
            entity.setAppCode(appCode);
            entity.setIsDelete(0);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            syncConfigMapper.insert(entity);
        } else {
            entity.setId(existing.getId());
            entity.setTenantId(tenantId);
            entity.setAppCode(appCode);
            entity.setCreatedAt(existing.getCreatedAt());
            entity.setCreatedBy(existing.getCreatedBy());
            entity.setUpdatedAt(now);
            syncConfigMapper.updateById(entity);
        }
    }
}
