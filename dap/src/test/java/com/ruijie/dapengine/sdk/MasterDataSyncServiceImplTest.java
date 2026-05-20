package com.ruijie.dapengine.sdk;

import com.ruijie.dapengine.common.enums.TriggerAction;
import com.ruijie.dapengine.common.model.PageResult;
import com.ruijie.dapengine.common.model.SyncLogDTO;
import com.ruijie.dapengine.common.model.SyncResultDTO;
import com.ruijie.dapengine.entity.SyncLogEntity;
import com.ruijie.dapengine.repository.SyncLogRepository;
import com.ruijie.dapengine.sync.SyncExecutor;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * MasterDataSyncServiceImpl 单元测试。
 */
public class MasterDataSyncServiceImplTest {

    private SyncExecutor syncExecutor;
    private SyncLogRepository syncLogRepository;
    private MasterDataSyncServiceImpl service;

    @Before
    public void setUp() {
        syncExecutor = mock(SyncExecutor.class);
        syncLogRepository = mock(SyncLogRepository.class);
        service = new MasterDataSyncServiceImpl(syncExecutor, syncLogRepository);
    }

    @Test
    public void getLastSyncResult_should_convert_latest_log() {
        SyncLogEntity entity = new SyncLogEntity();
        entity.setSubjectCode("CUSTOMER");
        entity.setAction("DELTA");
        entity.setStatus("SUCCESS");
        entity.setRecordCount(3);
        entity.setCostMs(88L);
        entity.setErrorMsg("同步完成");
        when(syncLogRepository.findLatestBySubjectCode("CUSTOMER")).thenReturn(entity);

        SyncResultDTO result = service.getLastSyncResult("CUSTOMER");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAction()).isEqualTo("DELTA");
        assertThat(result.getRecordCount()).isEqualTo(3);
    }

    @Test
    public void getSyncLogs_should_delegate_to_repository() {
        PageResult<SyncLogDTO> page = PageResult.of(1, 1, 10, Collections.singletonList(new SyncLogDTO()));
        when(syncLogRepository.findBySubjectCode("CUSTOMER", 1, 10)).thenReturn(page);

        PageResult<SyncLogDTO> result = service.getSyncLogs("CUSTOMER", 1, 10);

        assertThat(result).isSameAs(page);
    }

    @Test
    public void triggerDeltaSync_should_delegate_to_executor() {
        SyncResultDTO expected = new SyncResultDTO("CUSTOMER", true, 1, 10L, "ok", "DELTA");
        when(syncExecutor.executeSync("CUSTOMER", TriggerAction.DELTA)).thenReturn(expected);

        assertThat(service.triggerDeltaSync("CUSTOMER")).isSameAs(expected);
    }
}
