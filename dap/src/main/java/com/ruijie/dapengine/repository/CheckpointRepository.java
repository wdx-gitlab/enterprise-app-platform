package com.ruijie.dapengine.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruijie.dapengine.autoconfigure.DapEngineJdbcTemplate;
import com.ruijie.dapengine.entity.CheckpointEntity;
import com.ruijie.dapengine.mapper.CheckpointMapper;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * dap_sys_checkpoint 表数据访问层。
 *
 * <p>UPSERT 语义：使用 MySQL {@code INSERT … ON DUPLICATE KEY UPDATE}（原子操作）。
 * 查询使用 {@link CheckpointMapper}（MyBatis-Plus），写入使用 {@link DapEngineJdbcTemplate} 执行原生 SQL。</p>
 */
public class CheckpointRepository {

    private final CheckpointMapper checkpointMapper;
    private final DapEngineJdbcTemplate dapJdbc;
    private final String tenantId;
    private final String appCode;

    public CheckpointRepository(CheckpointMapper checkpointMapper,
                                 DapEngineJdbcTemplate dapJdbc,
                                 String tenantId,
                                 String appCode) {
        this.checkpointMapper = checkpointMapper;
        this.dapJdbc = dapJdbc;
        this.tenantId = tenantId;
        this.appCode = appCode;
    }

    /**
     * 按 subjectCode 查询位点记录。
     *
     * @return 存在则返回非空 Optional，否则返回空 Optional
     */
    public Optional<CheckpointEntity> findBySubjectCode(String subjectCode) {
        LambdaQueryWrapper<CheckpointEntity> wrapper = new LambdaQueryWrapper<CheckpointEntity>()
                .eq(CheckpointEntity::getTenantId, tenantId)
                .eq(CheckpointEntity::getAppCode, appCode)
                .eq(CheckpointEntity::getSubjectCode, subjectCode)
                .eq(CheckpointEntity::getIsDelete, 0)
                .last("LIMIT 1");
        return Optional.ofNullable(checkpointMapper.selectOne(wrapper));
    }

    /**
     * 保存或更新同步位点（UPSERT），使用 MySQL ON DUPLICATE KEY UPDATE。
     *
     * @param subjectCode  主题编码
     * @param lastVersion  本批次毫秒时间戳
     * @param lastSyncTime 同步完成时间
     * @param recordCount  已同步记录数
     */
    public void upsert(String subjectCode, long lastVersion, LocalDateTime lastSyncTime, long recordCount) {
        String sql = "INSERT INTO dap_sys_checkpoint "
                + "(tenant_id, app_code, subject_code, last_version, last_sync_time, record_count, "
                + " is_delete, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, 0, NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE "
                + "last_version=VALUES(last_version), last_sync_time=VALUES(last_sync_time), "
                + "record_count=VALUES(record_count), updated_at=NOW()";
        dapJdbc.getJdbcTemplate().update(sql, tenantId, appCode, subjectCode,
                lastVersion, lastSyncTime, recordCount);
    }
}
