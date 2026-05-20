package com.ruijie.dapengine.sdk;

import com.ruijie.dapengine.common.model.PageResult;
import com.ruijie.dapengine.common.model.SyncLogDTO;
import com.ruijie.dapengine.common.model.SyncResultDTO;

/**
 * 主数据同步门面接口。
 * 业务系统通过 {@link DapEngineService#sync()} 获取该接口实例，手动触发同步。
 */
public interface MasterDataSyncService {

    /**
     * 触发全量刷新同步（FULL_REFRESH）。
     * 将清空并重建正式表数据，谨慎调用。
     *
     * @param subject 主题编码
     * @return 同步结果
     */
    SyncResultDTO triggerFullSync(String subject);

    /**
     * 触发增量同步（DELTA）。
     * 基于上次同步 Checkpoint 拉取新数据。
     *
     * @param subject 主题编码
     * @return 同步结果
     */
    SyncResultDTO triggerDeltaSync(String subject);

    /**
     * 获取最近一次同步结果；若从未同步过则返使用 {@code null}。
     */
    SyncResultDTO getLastSyncResult(String subject);

    /**
     * 获取同步日志分页。
     */
    PageResult<SyncLogDTO> getSyncLogs(String subject, int page, int size);
}
