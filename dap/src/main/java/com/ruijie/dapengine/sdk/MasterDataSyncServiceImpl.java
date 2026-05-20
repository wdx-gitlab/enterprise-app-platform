package com.ruijie.dapengine.sdk;

import com.ruijie.dapengine.common.enums.TriggerAction;
import com.ruijie.dapengine.common.model.PageResult;
import com.ruijie.dapengine.common.model.SyncLogDTO;
import com.ruijie.dapengine.common.model.SyncResultDTO;
import com.ruijie.dapengine.entity.SyncLogEntity;
import com.ruijie.dapengine.repository.SyncLogRepository;
import com.ruijie.dapengine.sync.SyncExecutor;

/**
 * {@link MasterDataSyncService} 实现，委托给 {@link SyncExecutor}。
 * <p>SyncExecutor 为可选依赖：非 Web 上下文中 SyncExecutor 不会注册，
 * 此时调用同步方法将抛出 {@link UnsupportedOperationException}。</p>
 */
public class MasterDataSyncServiceImpl implements MasterDataSyncService {

    private final SyncExecutor syncExecutor;   // nullable in non-web context
    private final SyncLogRepository syncLogRepository;

    public MasterDataSyncServiceImpl(SyncExecutor syncExecutor, SyncLogRepository syncLogRepository) {
        this.syncExecutor = syncExecutor;
        this.syncLogRepository = syncLogRepository;
    }

    private void requireSyncExecutor() {
        if (syncExecutor == null) {
            throw new UnsupportedOperationException(
                "[DAP Engine] Sync operations require spring-webmvc on the classpath.");
        }
    }

    @Override
    public SyncResultDTO triggerFullSync(String subject) {
        requireSyncExecutor();
        return syncExecutor.executeSync(subject, TriggerAction.FULL_REFRESH);
    }

    @Override
    public SyncResultDTO triggerDeltaSync(String subject) {
        requireSyncExecutor();
        return syncExecutor.executeSync(subject, TriggerAction.DELTA);
    }

    @Override
    public SyncResultDTO getLastSyncResult(String subject) {
        SyncLogEntity entity = syncLogRepository.findLatestBySubjectCode(subject);
        if (entity == null) {
            return null;
        }
        return new SyncResultDTO(
                entity.getSubjectCode(),
                "SUCCESS".equalsIgnoreCase(entity.getStatus()),
                entity.getRecordCount() == null ? 0 : entity.getRecordCount(),
                entity.getCostMs() == null ? 0L : entity.getCostMs(),
                entity.getErrorMsg(),
                entity.getAction()
        );
    }

    @Override
    public PageResult<SyncLogDTO> getSyncLogs(String subject, int page, int size) {
        return syncLogRepository.findBySubjectCode(subject, page, size);
    }
}
