package com.ruijie.dapengine.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.sdk.MasterDataCacheService;
import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.admin.service.SchemaStatusService;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.enums.SyncAction;
import com.ruijie.dapengine.common.enums.TriggerAction;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.FieldMapping;
import com.ruijie.dapengine.common.model.SyncCheckpoint;
import com.ruijie.dapengine.common.model.SyncConfigDTO;
import com.ruijie.dapengine.common.model.SyncDataSourceConfig;
import com.ruijie.dapengine.common.model.SyncResultDTO;
import com.ruijie.dapengine.entity.SyncConfigEntity;
import com.ruijie.dapengine.entity.SyncLogEntity;
import com.ruijie.dapengine.provider.DataProvider;
import com.ruijie.dapengine.repository.CheckpointRepository;
import com.ruijie.dapengine.repository.SubjectRepository;
import com.ruijie.dapengine.repository.SyncConfigRepository;
import com.ruijie.dapengine.repository.SyncLogRepository;
import com.ruijie.dapengine.writer.LocalDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 同步执行器：负责一次完整的数据同步生命周期管理。
 *
 * <p>执行步骤（DELTA 路径）：
 * <ol>
 *   <li>查询 SyncConfig + Subject</li>
 *   <li>验证 schemaStatus == APPLIED</li>
 *   <li>获取主题级串行锁（US6 替换，当前暂用对象锁占位。/li>
 *   <li>查询 CheckpointRepository，构。SyncCheckpoint</li>
 *   <li>。DataProvider.fetch()</li>
 *   <li>。FetchResult.isEmpty() 。record_count=0 日志并返。/li>
 *   <li>。FieldMappingService.applyMapping()</li>
 *   <li>。SyncInterceptor.beforeSync()（若。bean。/li>
 *   <li>。LocalDataWriter.upsert()</li>
 *   <li>。SyncInterceptor.afterSync()</li>
 *   <li>。CheckpointRepository.upsert() 更新位点</li>
 *   <li>。sync_log SUCCESS</li>
 * </ol>
 * 捕获任意异常后写 sync_log FAIL。
 * </p>
 */
public class SyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(SyncExecutor.class);

    private final SubjectRepository subjectRepository;
    private final SyncConfigRepository syncConfigRepository;
    private final CheckpointRepository checkpointRepository;
    private final SyncLogRepository syncLogRepository;
    private final List<DataProvider> dataProviders;
    private final FieldMappingService fieldMappingService;
    private final LocalDataWriter localDataWriter;
    private final List<SyncInterceptor> syncInterceptors;
    private final MetadataConfigService metadataConfigService;
    private final SchemaStatusService schemaStatusService;
    private final ObjectMapper objectMapper;

    /** 主题级串行锁管理器（US6）：同一 Subject 并发触发时串行等待，不同 Subject 完全并行 */
    private final SubjectSyncLockManager lockManager;

    /** 主数据缓存服务（可选）：同步完成后驱逐对。subject 缓存；null 时不驱。*/
    private final MasterDataCacheService masterDataCacheService;

    public SyncExecutor(SubjectRepository subjectRepository,
                        SyncConfigRepository syncConfigRepository,
                        CheckpointRepository checkpointRepository,
                        SyncLogRepository syncLogRepository,
                        List<DataProvider> dataProviders,
                        FieldMappingService fieldMappingService,
                        LocalDataWriter localDataWriter,
                        List<SyncInterceptor> syncInterceptors,
                        MetadataConfigService metadataConfigService,
                        SchemaStatusService schemaStatusService,
                        ObjectMapper objectMapper) {
        this(subjectRepository, syncConfigRepository, checkpointRepository, syncLogRepository,
                dataProviders, fieldMappingService, localDataWriter, syncInterceptors,
                metadataConfigService, schemaStatusService, objectMapper,
                new SubjectSyncLockManager());
    }

    public SyncExecutor(SubjectRepository subjectRepository,
                        SyncConfigRepository syncConfigRepository,
                        CheckpointRepository checkpointRepository,
                        SyncLogRepository syncLogRepository,
                        List<DataProvider> dataProviders,
                        FieldMappingService fieldMappingService,
                        LocalDataWriter localDataWriter,
                        List<SyncInterceptor> syncInterceptors,
                        MetadataConfigService metadataConfigService,
                        SchemaStatusService schemaStatusService,
                        ObjectMapper objectMapper,
                        SubjectSyncLockManager lockManager) {
        this(subjectRepository, syncConfigRepository, checkpointRepository, syncLogRepository,
                dataProviders, fieldMappingService, localDataWriter, syncInterceptors,
                metadataConfigService, schemaStatusService, objectMapper,
                lockManager, null);
    }

    public SyncExecutor(SubjectRepository subjectRepository,
                        SyncConfigRepository syncConfigRepository,
                        CheckpointRepository checkpointRepository,
                        SyncLogRepository syncLogRepository,
                        List<DataProvider> dataProviders,
                        FieldMappingService fieldMappingService,
                        LocalDataWriter localDataWriter,
                        List<SyncInterceptor> syncInterceptors,
                        MetadataConfigService metadataConfigService,
                        SchemaStatusService schemaStatusService,
                        ObjectMapper objectMapper,
                        SubjectSyncLockManager lockManager,
                        MasterDataCacheService masterDataCacheService) {
        this.subjectRepository = subjectRepository;
        this.syncConfigRepository = syncConfigRepository;
        this.checkpointRepository = checkpointRepository;
        this.syncLogRepository = syncLogRepository;
        this.dataProviders = dataProviders;
        this.fieldMappingService = fieldMappingService;
        this.localDataWriter = localDataWriter;
        this.syncInterceptors = syncInterceptors;
        this.metadataConfigService = metadataConfigService;
        this.schemaStatusService = schemaStatusService;
        this.objectMapper = objectMapper;
        this.lockManager = lockManager;
        this.masterDataCacheService = masterDataCacheService;
    }

    /**
     * 执行一次完整的数据同步（DELTA 。FULL_REFRESH）。
     *
     * @param subjectCode   主题编码
     * @param triggerAction 触发动作（DELTA / FULL_REFRESH。
     * @return 同步结果 DTO
     */
    public SyncResultDTO executeSync(String subjectCode, TriggerAction triggerAction) {
        long startMs = System.currentTimeMillis();

        // 步骤 1: 查询配置
        SyncConfigEntity configEntity = syncConfigRepository.findBySubjectCode(subjectCode);
        if (configEntity == null) {
            throw new DapValidationException("[DAP Engine] 未找到同步配置: " + subjectCode);
        }
        com.ruijie.dapengine.common.model.SubjectDTO subject =
                subjectRepository.findByCode(subjectCode);
        if (subject == null || (subject.getIsDelete() != null && subject.getIsDelete() == 1)) {
            throw new DapValidationException("[DAP Engine] Subject 不存在或已删除: " + subjectCode);
        }

        // 步骤 2: 验证 schemaStatus == APPLIED
        List<FieldConfigDTO> activeFields = metadataConfigService.getActiveFieldDTOs(subjectCode);
        SchemaStatus schemaStatus = schemaStatusService.computeStatus(subjectCode, activeFields);
        if (schemaStatus != SchemaStatus.APPLIED) {
            throw new DapValidationException("[DAP Engine] subjectCode=" + subjectCode
                    + " schemaStatus 为 PENDING，请先执行 apply-schema");
        }

        // 步骤 3: 主题级串行锁（US6：SubjectSyncLockManager，最多等。300s。
        if (!lockManager.tryLock(subjectCode, 300)) {
            throw new DapValidationException("[DAP Engine] subject=" + subjectCode
                    + " 同步并发超时，已有同步任务正在执行");
        }
        try {
            return doExecuteSync(subjectCode, triggerAction, configEntity, subject.getName(), startMs);
        } finally {
            lockManager.unlock(subjectCode);
        }
    }

    private SyncResultDTO doExecuteSync(String subjectCode, TriggerAction triggerAction,
                                         SyncConfigEntity configEntity, String subjectName, long startMs) {
        int recordCount = 0;
        String backupTableName = null;
        SyncDataSourceConfig dsConfig = deserializeDsConfig(configEntity.getDatasourceConfig());
        List<FieldMapping> fieldMappings = deserializeFieldMappings(configEntity.getFieldMapping());
        String operator = "dap-sync";

        try {
            // 步骤 4: 查询 Checkpoint
            SyncCheckpoint checkpoint = checkpointRepository
                    .findBySubjectCode(subjectCode)
                    .map(e -> new SyncCheckpoint(subjectCode, e.getLastVersion() != null ? e.getLastVersion() : 0L,
                            e.getLastSyncTime(), e.getRecordCount() != null ? e.getRecordCount() : 0L))
                    .orElse(SyncCheckpoint.empty(subjectCode));

            // 步骤 5: 确定 Provider 和增量/全量 Checkpoint
            DataProvider provider = findProvider(configEntity.getProviderType());
            SyncCheckpoint fetchCheckpoint = (triggerAction == TriggerAction.FULL_REFRESH)
                    ? SyncCheckpoint.empty(subjectCode)
                    : checkpoint;
            SyncAction syncAction = triggerAction == TriggerAction.FULL_REFRESH
                    ? SyncAction.FULL_REFRESH : SyncAction.UPSERT;

            // 步骤 6-9: 流式拉取 + 逐页写库
            // 使用 fetchStreaming 代替 fetch：每页数据立即写库，内存峰值 ≈ 单页大小（pageSize 条）。
            // FULL_REFRESH：建 tmp 表 → 逐页写 tmp → RENAME TABLE（原子交换，正式表全程可读）
            // DELTA：每页直接 upsert，写入失败前的页已提交（下次同步会重新覆盖，最终一致）
            final int[] count = {0};
            final boolean[] tmpStarted = {false};
            try {
                provider.fetchStreaming(dsConfig, fetchCheckpoint, page -> {
                    // 字段映射
                    List<Map<String, Object>> mapped = (fieldMappings != null && !fieldMappings.isEmpty())
                            ? fieldMappingService.applyMapping(page, fieldMappings)
                            : page;
                    // beforeSync 拦截
                    mapped = applyBeforeSync(subjectCode, syncAction, mapped);
                    if (mapped == null || mapped.isEmpty()) return;

                    if (triggerAction == TriggerAction.FULL_REFRESH) {
                        if (!tmpStarted[0]) {
                            localDataWriter.beginFullRefresh(subjectCode);
                            tmpStarted[0] = true;
                        }
                        localDataWriter.appendFullRefreshPage(subjectCode, mapped, operator);
                        count[0] += mapped.size();
                    } else {
                        count[0] += localDataWriter.upsert(subjectCode, mapped, operator);
                    }
                });
            } catch (Exception streamEx) {
                // 流式过程中出错：FULL_REFRESH 需要清理 tmp 表，原正式表保持不变
                if (tmpStarted[0]) {
                    localDataWriter.rollbackFullRefresh(subjectCode);
                }
                throw streamEx;
            }

            // 步骤 6b: 空数据处理（fetchStreaming 不调用 consumer 时 count[0] == 0）
            if (count[0] == 0) {
                writeLog(subjectCode, subjectName, configEntity.getSyncMode(), triggerAction.name(),
                        "SUCCESS", 0, 0, System.currentTimeMillis() - startMs, null);
                return new SyncResultDTO(subjectCode, true, 0,
                        System.currentTimeMillis() - startMs, "无新数据", triggerAction.name());
            }

            // 步骤 9b: FULL_REFRESH 提交（RENAME TABLE 原子交换）
            // commitFullRefresh 失败时必须清理 tmp 表，否则孤立表会阻塞下次 beginFullRefresh
            if (triggerAction == TriggerAction.FULL_REFRESH) {
                try {
                    backupTableName = localDataWriter.commitFullRefresh(subjectCode);
                } catch (Exception commitEx) {
                    localDataWriter.rollbackFullRefresh(subjectCode);
                    throw commitEx;
                }
                recordCount = count[0];
            } else {
                recordCount = count[0];
            }

            // 步骤 10: afterSync 拦截（传空列表表示全量已完成，具体记录在流式过程中已处理）
            applyAfterSync(subjectCode, syncAction, Collections.emptyList());

            // 步骤 11: 更新 Checkpoint
            long nowMs = System.currentTimeMillis();
            checkpointRepository.upsert(subjectCode, nowMs, LocalDateTime.now(), recordCount);

            // 步骤 11b: 驱逐主数据缓存
            if (masterDataCacheService != null) {
                masterDataCacheService.evict(subjectCode);
            }

            // 步骤 12: SUCCESS 日志
            String logMsg = backupTableName != null ? "BACKUP_TABLE:" + backupTableName : null;
            writeLog(subjectCode, subjectName, configEntity.getSyncMode(), triggerAction.name(),
                    "SUCCESS", recordCount, 0, nowMs - startMs, logMsg);

            return new SyncResultDTO(subjectCode, true, recordCount,
                    nowMs - startMs, "同步完成", triggerAction.name());

        } catch (DapValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            long costMs = System.currentTimeMillis() - startMs;
            String errMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            log.error("[DAP Engine] sync failed for subject={}, action={}: {}",
                    subjectCode, triggerAction, errMsg, ex);
            writeLog(subjectCode, subjectName, configEntity.getSyncMode(), triggerAction.name(),
                    "FAIL", recordCount, 0, costMs, errMsg);
            return new SyncResultDTO(subjectCode, false, recordCount, costMs, errMsg, triggerAction.name());
        }
    }

    private DataProvider findProvider(String providerType) {
        for (DataProvider p : dataProviders) {
            if (p.type().equals(providerType)) {
                return p;
            }
        }
        throw new DapValidationException("[DAP Engine] 未找到 DataProvider: " + providerType);
    }

    private List<Map<String, Object>> applyBeforeSync(String subjectCode, SyncAction action,
                                                        List<Map<String, Object>> records) {
        List<Map<String, Object>> current = records;
        for (SyncInterceptor interceptor : syncInterceptors) {
            current = interceptor.beforeSync(subjectCode, action, current);
            if (current == null) {
                current = new ArrayList<>();
            }
        }
        return current;
    }

    private void applyAfterSync(String subjectCode, SyncAction action,
                                 List<Map<String, Object>> records) {
        for (SyncInterceptor interceptor : syncInterceptors) {
            interceptor.afterSync(subjectCode, action, records);
        }
    }

    private void writeLog(String subjectCode, String subjectName, String syncMode, String action,
                           String status, int recordCount, int skipCount, long costMs, String errorMsg) {
        try {
            SyncLogEntity entity = new SyncLogEntity();
            entity.setSubjectCode(subjectCode);
            entity.setSubjectName(subjectName != null ? subjectName : "");
            entity.setSyncMode(syncMode);
            entity.setAction(action);
            entity.setStatus(status);
            entity.setRecordCount(recordCount);
            entity.setSkipCount(skipCount);
            entity.setCostMs(costMs);
            entity.setErrorMsg(errorMsg);
            entity.setCreatedBy("dap-sync");
            entity.setUpdatedBy("dap-sync");
            syncLogRepository.insert(entity);
        } catch (Exception ex) {
            log.warn("[DAP Engine] 写 sync_log 失败, subject={}, status={}: {},",
                    subjectCode, status, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private SyncDataSourceConfig deserializeDsConfig(String json) {
        if (json == null || json.isEmpty()) {
            return new SyncDataSourceConfig();
        }
        try {
            return objectMapper.readValue(json, SyncDataSourceConfig.class);
        } catch (Exception ex) {
            log.warn("[DAP Engine] 反序列化 datasourceConfig 失败: {}", ex.getMessage());
            return new SyncDataSourceConfig();
        }
    }

    @SuppressWarnings("unchecked")
    private List<FieldMapping> deserializeFieldMappings(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FieldMapping.class));
        } catch (Exception ex) {
            log.warn("[DAP Engine] 反序列化 fieldMapping 失败: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }
}
