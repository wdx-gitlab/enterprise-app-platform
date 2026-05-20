package com.ruijie.dapengine.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.admin.service.SchemaStatusService;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.enums.TriggerAction;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.FetchResult;
import com.ruijie.dapengine.common.model.SubjectDTO;
import com.ruijie.dapengine.common.model.SyncCheckpoint;
import com.ruijie.dapengine.common.model.SyncResultDTO;
import com.ruijie.dapengine.entity.SyncConfigEntity;
import com.ruijie.dapengine.provider.DataProvider;
import com.ruijie.dapengine.repository.CheckpointRepository;
import com.ruijie.dapengine.repository.SubjectRepository;
import com.ruijie.dapengine.repository.SyncConfigRepository;
import com.ruijie.dapengine.repository.SyncLogRepository;
import com.ruijie.dapengine.writer.LocalDataWriter;
import com.ruijie.dapengine.sdk.MasterDataCacheService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SyncExecutor 单元测试（JUnit4 + Mockito）。
 * 覆盖：DELTA 成功（写入 N 条、checkpoint 更新、log=SUCCESS）。
 * 覆盖：FetchResult 为空时写 record_count=0 日志、schemaStatus=PENDING 时抛 DapValidationException。
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncExecutorTest {

    private static final String SUBJECT_CODE = "CUSTOMER";
    private static final String SUBJECT_NAME = "客户";
    private static final String PROVIDER_TYPE = "HTTP";

    @Mock private SubjectRepository subjectRepository;
    @Mock private SyncConfigRepository syncConfigRepository;
    @Mock private CheckpointRepository checkpointRepository;
    @Mock private SyncLogRepository syncLogRepository;
    @Mock private DataProvider dataProvider;
    @Mock private FieldMappingService fieldMappingService;
    @Mock private LocalDataWriter localDataWriter;
    @Mock private MetadataConfigService metadataConfigService;
    @Mock private SchemaStatusService schemaStatusService;
    @Mock private MasterDataCacheService masterDataCacheService;

    private SyncExecutor syncExecutor;

    @Before
    public void setUp() {
        when(dataProvider.type()).thenReturn(PROVIDER_TYPE);
        syncExecutor = new SyncExecutor(
                subjectRepository, syncConfigRepository, checkpointRepository,
                syncLogRepository, Collections.singletonList(dataProvider),
                fieldMappingService, localDataWriter, Collections.emptyList(),
                metadataConfigService, schemaStatusService, new ObjectMapper());
    }

    private SyncConfigEntity buildConfig() {
        SyncConfigEntity e = new SyncConfigEntity();
        e.setSubjectCode(SUBJECT_CODE);
        e.setProviderType(PROVIDER_TYPE);
        e.setSyncMode("SCHEDULE");
        e.setDatasourceConfig("{}");
        e.setFieldMapping("[]");
        e.setSyncAction("DELTA");
        e.setStatus(1);
        return e;
    }

    private SubjectDTO buildSubject() {
        SubjectDTO dto = new SubjectDTO();
        dto.setCode(SUBJECT_CODE);
        dto.setName(SUBJECT_NAME);
        dto.setStatus(1);
        dto.setIsDelete(0);
        return dto;
    }

    private List<Map<String, Object>> buildRecords(int count) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("code", "C00" + i);
            row.put("name", "Name" + i);
            list.add(row);
        }
        return list;
    }

    @Test
    public void should_execute_delta_successfully_and_update_checkpoint() {
        List<Map<String, Object>> records = buildRecords(3);

        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(buildConfig());
        when(subjectRepository.findByCode(SUBJECT_CODE)).thenReturn(buildSubject());
        when(metadataConfigService.getActiveFieldDTOs(SUBJECT_CODE))
                .thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq(SUBJECT_CODE), anyList()))
                .thenReturn(SchemaStatus.APPLIED);
        when(checkpointRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(Optional.empty());
        when(dataProvider.fetch(any(), any(SyncCheckpoint.class)))
                .thenReturn(FetchResult.of(records));
        // fieldMapping is empty ([]); fieldMappingService not called when mapping is empty
        when(localDataWriter.upsert(eq(SUBJECT_CODE), eq(records), anyString())).thenReturn(3);

        SyncResultDTO result = syncExecutor.executeSync(SUBJECT_CODE, TriggerAction.DELTA);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRecordCount()).isEqualTo(3);
        assertThat(result.getAction()).isEqualTo("DELTA");

        // checkpoint 应更新
        verify(checkpointRepository).upsert(eq(SUBJECT_CODE), anyLong(), any(), eq(3L));
        // 日志应写入 SUCCESS
        verify(syncLogRepository).insert(argThat(log -> "SUCCESS".equals(log.getStatus())));
    }

    @Test
    public void should_write_zero_count_log_when_fetch_result_is_empty() {
        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(buildConfig());
        when(subjectRepository.findByCode(SUBJECT_CODE)).thenReturn(buildSubject());
        when(metadataConfigService.getActiveFieldDTOs(SUBJECT_CODE))
                .thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq(SUBJECT_CODE), anyList()))
                .thenReturn(SchemaStatus.APPLIED);
        when(checkpointRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(Optional.empty());
        when(dataProvider.fetch(any(), any(SyncCheckpoint.class)))
                .thenReturn(FetchResult.of(Collections.emptyList()));

        SyncResultDTO result = syncExecutor.executeSync(SUBJECT_CODE, TriggerAction.DELTA);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRecordCount()).isEqualTo(0);

        // 无数据时不调用 LocalDataWriter
        verify(localDataWriter, never()).upsert(any(), any(), any());
        // 日志仍写入成功（record_count=0)
        verify(syncLogRepository).insert(argThat(log -> "SUCCESS".equals(log.getStatus())
                && log.getRecordCount() == 0));
    }

    @Test
    public void should_throw_validation_exception_when_schema_status_pending() {
        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(buildConfig());
        when(subjectRepository.findByCode(SUBJECT_CODE)).thenReturn(buildSubject());
        when(metadataConfigService.getActiveFieldDTOs(SUBJECT_CODE))
                .thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq(SUBJECT_CODE), anyList()))
                .thenReturn(SchemaStatus.PENDING);

        assertThatThrownBy(() -> syncExecutor.executeSync(SUBJECT_CODE, TriggerAction.DELTA))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    public void should_write_fail_log_when_provider_throws_exception() {
        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(buildConfig());
        when(subjectRepository.findByCode(SUBJECT_CODE)).thenReturn(buildSubject());
        when(metadataConfigService.getActiveFieldDTOs(SUBJECT_CODE))
                .thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq(SUBJECT_CODE), anyList()))
                .thenReturn(SchemaStatus.APPLIED);
        when(checkpointRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(Optional.empty());
        when(dataProvider.fetch(any(), any(SyncCheckpoint.class)))
                .thenThrow(new RuntimeException("Network error"));

        SyncResultDTO result = syncExecutor.executeSync(SUBJECT_CODE, TriggerAction.DELTA);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Network error");
        verify(syncLogRepository).insert(argThat(log -> "FAIL".equals(log.getStatus())));
    }

    // -------------------------------------------------------------------------
    // T054: FULL_REFRESH 场景
    // -------------------------------------------------------------------------

    @Test
    public void should_execute_full_refresh_successfully_and_log_backup_table() {
        SyncConfigEntity config = buildConfig();
        config.setSyncAction("FULL_REFRESH");

        List<Map<String, Object>> records = buildRecords(5);

        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(config);
        when(subjectRepository.findByCode(SUBJECT_CODE)).thenReturn(buildSubject());
        when(metadataConfigService.getActiveFieldDTOs(SUBJECT_CODE))
                .thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq(SUBJECT_CODE), anyList()))
                .thenReturn(SchemaStatus.APPLIED);
        when(checkpointRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(Optional.empty());
        when(dataProvider.fetch(any(), any(SyncCheckpoint.class)))
                .thenReturn(FetchResult.of(records));
        when(localDataWriter.fullRefresh(eq(SUBJECT_CODE), eq(records), anyString()))
                .thenReturn("dap_customer_bak_20240101120000");

        SyncResultDTO result = syncExecutor.executeSync(SUBJECT_CODE, TriggerAction.FULL_REFRESH);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRecordCount()).isEqualTo(5);
        assertThat(result.getAction()).isEqualTo("FULL_REFRESH");

        // log.errorMsg 应以 "BACKUP_TABLE:" 开头
        verify(syncLogRepository).insert(argThat(log ->
                "SUCCESS".equals(log.getStatus())
                && log.getErrorMsg() != null
                && log.getErrorMsg().startsWith("BACKUP_TABLE:")));

        // checkpoint 应更新（recordCount = 5）
        verify(checkpointRepository).upsert(eq(SUBJECT_CODE), anyLong(), any(), eq(5L));
    }

    @Test
    public void should_write_fail_log_when_full_refresh_throws_exception() {
        SyncConfigEntity config = buildConfig();
        config.setSyncAction("FULL_REFRESH");

        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(config);
        when(subjectRepository.findByCode(SUBJECT_CODE)).thenReturn(buildSubject());
        when(metadataConfigService.getActiveFieldDTOs(SUBJECT_CODE))
                .thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq(SUBJECT_CODE), anyList()))
                .thenReturn(SchemaStatus.APPLIED);
        when(checkpointRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(Optional.empty());
        when(dataProvider.fetch(any(), any(SyncCheckpoint.class)))
                .thenReturn(FetchResult.of(buildRecords(3)));
        when(localDataWriter.fullRefresh(any(), any(), any()))
                .thenThrow(new RuntimeException("Rename failed"));

        SyncResultDTO result = syncExecutor.executeSync(SUBJECT_CODE, TriggerAction.FULL_REFRESH);

        assertThat(result.isSuccess()).isFalse();
        verify(syncLogRepository).insert(argThat(log -> "FAIL".equals(log.getStatus())));
    }

    // -------------------------------------------------------------------------
    // T008/T009: MasterDataCacheService 与缓存驱逐场景
    // -------------------------------------------------------------------------

    @Test
    public void should_evict_cache_after_successful_sync() {
        // 使用 13 参构造器，注入 masterDataCacheService
        SyncExecutor execWithCache = new SyncExecutor(
                subjectRepository, syncConfigRepository, checkpointRepository,
                syncLogRepository, Collections.singletonList(dataProvider),
                fieldMappingService, localDataWriter, Collections.emptyList(),
                metadataConfigService, schemaStatusService, new ObjectMapper(),
                new SubjectSyncLockManager(), masterDataCacheService);

        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(buildConfig());
        when(subjectRepository.findByCode(SUBJECT_CODE)).thenReturn(buildSubject());
        when(metadataConfigService.getActiveFieldDTOs(SUBJECT_CODE)).thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq(SUBJECT_CODE), anyList())).thenReturn(SchemaStatus.APPLIED);
        when(checkpointRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(Optional.empty());
        when(dataProvider.fetch(any(), any(SyncCheckpoint.class))).thenReturn(FetchResult.of(buildRecords(2)));
        when(localDataWriter.upsert(eq(SUBJECT_CODE), any(), anyString())).thenReturn(2);

        execWithCache.executeSync(SUBJECT_CODE, TriggerAction.DELTA);

        verify(masterDataCacheService).evict(SUBJECT_CODE);
    }

    @Test
    public void should_not_throw_when_cache_service_is_null() {
        // 现有 syncExecutor 使用 11 参构造器（cacheService=null），确认不会抛 NPE
        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(buildConfig());
        when(subjectRepository.findByCode(SUBJECT_CODE)).thenReturn(buildSubject());
        when(metadataConfigService.getActiveFieldDTOs(SUBJECT_CODE)).thenReturn(Collections.emptyList());
        when(schemaStatusService.computeStatus(eq(SUBJECT_CODE), anyList())).thenReturn(SchemaStatus.APPLIED);
        when(checkpointRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(Optional.empty());
        when(dataProvider.fetch(any(), any(SyncCheckpoint.class))).thenReturn(FetchResult.of(buildRecords(1)));
        when(localDataWriter.upsert(any(), any(), any())).thenReturn(1);

        assertThatCode(() -> syncExecutor.executeSync(SUBJECT_CODE, TriggerAction.DELTA))
                .doesNotThrowAnyException();
    }
}
