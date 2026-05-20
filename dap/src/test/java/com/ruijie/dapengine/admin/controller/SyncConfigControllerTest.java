package com.ruijie.dapengine.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.admin.service.SyncConfigService;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.FieldMapping;
import com.ruijie.dapengine.common.model.SyncConfigDTO;
import com.ruijie.dapengine.common.model.SyncConfigRequest;
import com.ruijie.dapengine.common.model.SyncDataSourceConfig;
import com.ruijie.dapengine.common.model.PageResult;
import com.ruijie.dapengine.common.model.SyncLogDTO;
import com.ruijie.dapengine.common.model.SyncResultDTO;
import com.ruijie.dapengine.common.model.TestConnectResultDTO;
import com.ruijie.dapengine.provider.DataProvider;
import com.ruijie.dapengine.repository.SyncLogRepository;
import com.ruijie.dapengine.sync.SyncExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SyncConfigController MockMvc 集成测试。
 * 使用 standaloneSetup + Mockito 模拟 Service，不依赖 Spring 完整上下文。
 */
@RunWith(SpringRunner.class)
public class SyncConfigControllerTest {

    private static final String SUBJECT_CODE = "CUSTOMER";

    private MockMvc mockMvc;
    private SyncConfigService syncConfigService;
    private DataProvider mockHttpProvider;
    private SyncExecutor syncExecutor;
    private SyncLogRepository syncLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        syncConfigService = Mockito.mock(SyncConfigService.class);
        mockHttpProvider = Mockito.mock(DataProvider.class);
        when(mockHttpProvider.type()).thenReturn("HTTP");
        syncExecutor = Mockito.mock(SyncExecutor.class);
        syncLogRepository = Mockito.mock(SyncLogRepository.class);
        SyncConfigController controller = new SyncConfigController(syncConfigService,
                Arrays.asList(mockHttpProvider), syncExecutor, syncLogRepository);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // -------------------------------------------------------------------------
    // EP-1: GET /dap-engine/admin/sync/{subjectCode}
    // -------------------------------------------------------------------------

    @Test
    public void should_getConfig_return_masked_config() throws Exception {
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setUrl("http://api.example.com");
        ds.setMethod("GET");

        SyncConfigDTO dto = new SyncConfigDTO();
        dto.setSubjectCode(SUBJECT_CODE);
        dto.setSubjectName("客户");
        dto.setSyncMode("SCHEDULE");
        dto.setProviderType("HTTP");
        dto.setCronExpr("0 0 * * * ?");
        dto.setDatasourceConfig(ds);
        dto.setFieldMapping(Collections.singletonList(new FieldMapping("id", "credit_code")));
        dto.setSyncAction("DELTA");
        dto.setStatus(1);

        when(syncConfigService.get(SUBJECT_CODE)).thenReturn(dto);

        mockMvc.perform(get("/dap-engine/admin/sync/{subjectCode}", SUBJECT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.subjectCode").value(SUBJECT_CODE))
                .andExpect(jsonPath("$.data.syncMode").value("SCHEDULE"))
                .andExpect(jsonPath("$.data.providerType").value("HTTP"));
    }

    @Test
    public void should_getConfig_return_null_data_when_not_configured() throws Exception {
        when(syncConfigService.get(SUBJECT_CODE)).thenReturn(null);

        mockMvc.perform(get("/dap-engine/admin/sync/{subjectCode}", SUBJECT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // EP-2: POST /dap-engine/admin/sync/{subjectCode}
    // -------------------------------------------------------------------------

    @Test
    public void should_saveConfig_return_code0_on_success() throws Exception {
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setUrl("http://api.example.com");
        ds.setMethod("GET");

        SyncConfigRequest req = new SyncConfigRequest();
        req.setSyncMode("SCHEDULE");
        req.setProviderType("HTTP");
        req.setCronExpr("0 0 * * * ?");
        req.setDatasourceConfig(ds);
        req.setFieldMapping(Collections.singletonList(new FieldMapping("id", "credit_code")));
        req.setSyncAction("DELTA");
        req.setStatus(1);

        doNothing().when(syncConfigService).save(eq(SUBJECT_CODE), any(SyncConfigRequest.class), any());

        mockMvc.perform(post("/dap-engine/admin/sync/{subjectCode}", SUBJECT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    public void should_saveConfig_return_code4001_when_validation_fails() throws Exception {
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setUrl("http://api.example.com");
        ds.setMethod("GET");

        SyncConfigRequest req = new SyncConfigRequest();
        req.setSyncMode("SCHEDULE");
        req.setProviderType("HTTP");
        req.setCronExpr("invalid-cron");
        req.setDatasourceConfig(ds);
        req.setFieldMapping(Collections.singletonList(new FieldMapping("id", "credit_code")));

        doThrow(new DapValidationException("[DAP Engine] Invalid cronExpr: invalid-cron"))
                .when(syncConfigService).save(eq(SUBJECT_CODE), any(SyncConfigRequest.class), any());

        mockMvc.perform(post("/dap-engine/admin/sync/{subjectCode}", SUBJECT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cronExpr")));
    }

    // -------------------------------------------------------------------------
    // EP-3: POST /dap-engine/admin/sync/{subjectCode}/test-connect
    // -------------------------------------------------------------------------

    @Test
    public void should_testConnect_return_success_for_http_provider() throws Exception {
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setUrl("http://api.example.com");
        ds.setMethod("GET");

        SyncConfigRequest req = new SyncConfigRequest();
        req.setProviderType("HTTP");
        req.setDatasourceConfig(ds);

        TestConnectResultDTO successResult = TestConnectResultDTO.success(
                "HTTP",
                Collections.singletonList("id"),
                Collections.emptyList());
        when(mockHttpProvider.testConnect(any(SyncDataSourceConfig.class))).thenReturn(successResult);

        mockMvc.perform(post("/dap-engine/admin/sync/{subjectCode}/test-connect", SUBJECT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.providerType").value("HTTP"));
    }

    @Test
    public void should_testConnect_return_ok_with_success_false_for_unsupported_provider() throws Exception {
        SyncConfigRequest req = new SyncConfigRequest();
        req.setProviderType("UNKNOWN");
        req.setDatasourceConfig(new SyncDataSourceConfig());

        mockMvc.perform(post("/dap-engine/admin/sync/{subjectCode}/test-connect", SUBJECT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(false));
    }

    // -------------------------------------------------------------------------
    // EP-4: POST /dap-engine/admin/sync/{subjectCode}/trigger?action=DELTA|FULL_REFRESH
    // -------------------------------------------------------------------------

    @Test
    public void should_trigger_delta_return_code0() throws Exception {
        SyncResultDTO syncResult = new SyncResultDTO(SUBJECT_CODE, true, 5, 200L, "同步完成", "DELTA");
        when(syncExecutor.executeSync(eq(SUBJECT_CODE), eq(com.ruijie.dapengine.common.enums.TriggerAction.DELTA)))
                .thenReturn(syncResult);

        mockMvc.perform(post("/dap-engine/admin/sync/{subjectCode}/trigger", SUBJECT_CODE)
                        .param("action", "DELTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.recordCount").value(5))
                .andExpect(jsonPath("$.data.action").value("DELTA"));
    }

    @Test
    public void should_trigger_full_refresh_return_code0() throws Exception {
        SyncResultDTO syncResult = new SyncResultDTO(SUBJECT_CODE, true, 10, 500L, "同步完成", "FULL_REFRESH");
        when(syncExecutor.executeSync(eq(SUBJECT_CODE),
                eq(com.ruijie.dapengine.common.enums.TriggerAction.FULL_REFRESH)))
                .thenReturn(syncResult);

        mockMvc.perform(post("/dap-engine/admin/sync/{subjectCode}/trigger", SUBJECT_CODE)
                        .param("action", "FULL_REFRESH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.action").value("FULL_REFRESH"));
    }

    @Test
    public void should_trigger_return_code4001_for_invalid_action() throws Exception {
        mockMvc.perform(post("/dap-engine/admin/sync/{subjectCode}/trigger", SUBJECT_CODE)
                        .param("action", "INVALID_ACTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    // -------------------------------------------------------------------------
    // EP-5: GET /dap-engine/admin/sync/{subjectCode}/logs?page=1&size=20
    // -------------------------------------------------------------------------

    @Test
    public void should_getLogs_return_page_result() throws Exception {
        SyncLogDTO logDto = new SyncLogDTO();
        logDto.setId(1L);
        logDto.setSubjectCode(SUBJECT_CODE);
        logDto.setStatus("SUCCESS");
        logDto.setRecordCount(5);

        PageResult<SyncLogDTO> pageResult = PageResult.of(1L, 1, 20,
                Collections.singletonList(logDto));
        when(syncLogRepository.findBySubjectCode(eq(SUBJECT_CODE), eq(1), eq(20)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/dap-engine/admin/sync/{subjectCode}/logs", SUBJECT_CODE)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("SUCCESS"));
    }
}
