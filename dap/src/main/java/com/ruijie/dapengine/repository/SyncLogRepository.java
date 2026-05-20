package com.ruijie.dapengine.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruijie.dapengine.common.model.PageResult;
import com.ruijie.dapengine.common.model.SyncLogDTO;
import com.ruijie.dapengine.entity.SyncLogEntity;
import com.ruijie.dapengine.mapper.SyncLogMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * dap_sys_sync_log 表数据访问层，基于 MyBatis-Plus {@link SyncLogMapper} 实现。
 */
public class SyncLogRepository {

    private final SyncLogMapper syncLogMapper;
    private final String tenantId;
    private final String appCode;

    public SyncLogRepository(SyncLogMapper syncLogMapper, String tenantId, String appCode) {
        this.syncLogMapper = syncLogMapper;
        this.tenantId = tenantId;
        this.appCode = appCode;
    }

    /**
     * 插入一条同步日志记录。
     *
     * @param entity 日志实体（id 由数据库 AUTO_INCREMENT 生成）
     */
    public void insert(SyncLogEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(tenantId);
        entity.setAppCode(appCode);
        entity.setIsDelete(0);
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy("");
        }
        if (entity.getUpdatedBy() == null) {
            entity.setUpdatedBy("");
        }
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        syncLogMapper.insert(entity);
    }

    /**
     * 按 subjectCode 查询同步日志（按 created_at 倒序分页）。
     *
     * @param subjectCode 主题编码
     * @param page        页码（1-based）
     * @param size        每页条数
     * @return 分页结果
     */
    public PageResult<SyncLogDTO> findBySubjectCode(String subjectCode, int page, int size) {
        int pageNum = Math.max(1, page);
        LambdaQueryWrapper<SyncLogEntity> wrapper = new LambdaQueryWrapper<SyncLogEntity>()
                .eq(SyncLogEntity::getTenantId, tenantId)
                .eq(SyncLogEntity::getAppCode, appCode)
                .eq(SyncLogEntity::getSubjectCode, subjectCode)
                .eq(SyncLogEntity::getIsDelete, 0)
                .orderByDesc(SyncLogEntity::getCreatedAt);

        com.baomidou.mybatisplus.core.metadata.IPage<SyncLogEntity> mbpPage =
                syncLogMapper.selectPage(new Page<>(pageNum, size), wrapper);

        List<SyncLogDTO> dtos = mbpPage.getRecords().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return PageResult.of(mbpPage.getTotal(), pageNum, size, dtos);
    }

    /**
     * 查询指定主题最近一条同步日志。
     */
    public SyncLogEntity findLatestBySubjectCode(String subjectCode) {
        LambdaQueryWrapper<SyncLogEntity> wrapper = new LambdaQueryWrapper<SyncLogEntity>()
                .eq(SyncLogEntity::getTenantId, tenantId)
                .eq(SyncLogEntity::getAppCode, appCode)
                .eq(SyncLogEntity::getSubjectCode, subjectCode)
                .eq(SyncLogEntity::getIsDelete, 0)
                .orderByDesc(SyncLogEntity::getCreatedAt)
                .last("LIMIT 1");
        return syncLogMapper.selectOne(wrapper);
    }

    private SyncLogDTO toDto(SyncLogEntity e) {
        SyncLogDTO dto = new SyncLogDTO();
        dto.setId(e.getId());
        dto.setSubjectCode(e.getSubjectCode());
        dto.setSubjectName(e.getSubjectName());
        dto.setSyncMode(e.getSyncMode());
        dto.setAction(e.getAction());
        dto.setStatus(e.getStatus());
        dto.setRecordCount(e.getRecordCount() == null ? 0 : e.getRecordCount());
        dto.setSkipCount(e.getSkipCount() == null ? 0 : e.getSkipCount());
        dto.setCostMs(e.getCostMs());
        dto.setErrorMsg(e.getErrorMsg());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}
