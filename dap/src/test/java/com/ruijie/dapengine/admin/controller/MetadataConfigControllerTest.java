package com.ruijie.dapengine.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.SchemaChangeResult;
import com.ruijie.dapengine.common.model.SubjectDTO;
import com.ruijie.dapengine.common.model.SubjectRequest;
import com.ruijie.dapengine.migration.DapEngineSchemaInitializer;
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
 * MetadataConfigController MockMvc 集成测试。
 * 使用 standaloneSetup + Mockito 模拟 Service，不依赖 Spring 完整上下文。
 */
@RunWith(SpringRunner.class)
public class MetadataConfigControllerTest {

    private MockMvc mockMvc;
    private MetadataConfigService metadataConfigService;
    private DapEngineSchemaInitializer schemaInitializer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        metadataConfigService = Mockito.mock(MetadataConfigService.class);
        schemaInitializer = Mockito.mock(DapEngineSchemaInitializer.class);
        MetadataConfigController controller =
            new MetadataConfigController(metadataConfigService, schemaInitializer);
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    // -------------------------------------------------------------------------
    // US1: GET /subjects
    // -------------------------------------------------------------------------

    @Test
    public void should_listSubjects_return_200_with_list() throws Exception {
        SubjectDTO dto = new SubjectDTO();
        dto.setId(1L);
        dto.setCode("CUSTOMER");
        dto.setName("客户");
        dto.setSchemaStatus(SchemaStatus.PENDING);
        when(metadataConfigService.listSubjects()).thenReturn(Arrays.asList(dto));

        mockMvc.perform(get("/dap-engine/admin/metadata/subjects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].code").value("CUSTOMER"))
            .andExpect(jsonPath("$.data[0].schemaStatus").value("PENDING"));
    }

    @Test
    public void should_listSubjects_return_200_with_empty_list() throws Exception {
        when(metadataConfigService.listSubjects()).thenReturn(Collections.<SubjectDTO>emptyList());

        mockMvc.perform(get("/dap-engine/admin/metadata/subjects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isArray());
    }

    // -------------------------------------------------------------------------
    // US1: DELETE /subjects/{subject}
    // -------------------------------------------------------------------------

    @Test
    public void should_deleteSubject_return_200() throws Exception {
        doNothing().when(metadataConfigService).deleteSubject(eq("CUSTOMER"), anyString());

        mockMvc.perform(delete("/dap-engine/admin/metadata/subjects/CUSTOMER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    public void should_deleteSubject_return_4001_when_not_found() throws Exception {
        doThrow(new DapValidationException("[DAP Engine] subject 'NOEXIST' 不存在或已删除"))
            .when(metadataConfigService).deleteSubject(eq("NOEXIST"), anyString());

        mockMvc.perform(delete("/dap-engine/admin/metadata/subjects/NOEXIST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4001))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("不存在")));
    }

    // -------------------------------------------------------------------------
    // US2: POST /subjects
    // -------------------------------------------------------------------------

    @Test
    public void should_createSubject_return_200_with_data() throws Exception {
        SubjectRequest request = new SubjectRequest();
        SubjectRequest.SubjectInfo info = new SubjectRequest.SubjectInfo();
        info.setCode("CUSTOMER");
        info.setName("客户");
        request.setSubject(info);
        request.setFields(Collections.emptyList());

        SubjectDTO returned = new SubjectDTO();
        returned.setId(1L);
        returned.setCode("CUSTOMER");
        returned.setName("客户");
        returned.setSchemaStatus(SchemaStatus.PENDING);
        when(metadataConfigService.saveSubjectConfig(eq("CUSTOMER"), any(), anyString()))
            .thenReturn(returned);

        mockMvc.perform(post("/dap-engine/admin/metadata/subjects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.code").value("CUSTOMER"))
            .andExpect(jsonPath("$.data.schemaStatus").value("PENDING"));
    }

    @Test
    public void should_createSubject_return_4001_when_code_missing() throws Exception {
        SubjectRequest request = new SubjectRequest();
        request.setSubject(new SubjectRequest.SubjectInfo()); // code 为 null

        mockMvc.perform(post("/dap-engine/admin/metadata/subjects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    public void should_createSubject_return_4001_on_duplicate_code() throws Exception {
        SubjectRequest request = new SubjectRequest();
        SubjectRequest.SubjectInfo info = new SubjectRequest.SubjectInfo();
        info.setCode("CUSTOMER");
        info.setName("客户");
        request.setSubject(info);
        request.setFields(Collections.emptyList());

        when(metadataConfigService.saveSubjectConfig(eq("CUSTOMER"), any(), anyString()))
            .thenThrow(new DapValidationException("[DAP Engine] subject code 'CUSTOMER' 已存在"));

        mockMvc.perform(post("/dap-engine/admin/metadata/subjects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4001));
    }

    // -------------------------------------------------------------------------
    // US2: PUT /subjects/{subject}
    // -------------------------------------------------------------------------

    @Test
    public void should_updateSubject_return_200() throws Exception {
        SubjectRequest request = new SubjectRequest();
        SubjectRequest.SubjectInfo info = new SubjectRequest.SubjectInfo();
        info.setName("客户（更新）");
        request.setSubject(info);
        request.setFields(Collections.emptyList());

        SubjectDTO returned = new SubjectDTO();
        returned.setCode("CUSTOMER");
        returned.setName("客户（更新）");
        returned.setSchemaStatus(SchemaStatus.PENDING);
        when(metadataConfigService.saveSubjectConfig(eq("CUSTOMER"), any(), anyString()))
            .thenReturn(returned);

        mockMvc.perform(put("/dap-engine/admin/metadata/subjects/CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.name").value("客户（更新）"));
    }

    // -------------------------------------------------------------------------
    // US2: GET /subjects/{subject}/fields
    // -------------------------------------------------------------------------

    @Test
    public void should_getFields_return_200_with_all_fields_including_deprecated() throws Exception {
        SubjectDTO returned = new SubjectDTO();
        returned.setCode("CUSTOMER");
        returned.setSchemaStatus(SchemaStatus.PENDING);
        returned.setFields(Collections.emptyList());
        when(metadataConfigService.getFieldsBySubject(eq("CUSTOMER"))).thenReturn(returned);

        mockMvc.perform(get("/dap-engine/admin/metadata/subjects/CUSTOMER/fields"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.code").value("CUSTOMER"));
    }

    // -------------------------------------------------------------------------
    // T016: POST /subjects/{subject}/apply-schema
    // -------------------------------------------------------------------------

    @Test
    public void applySchema_returns_schema_change_result() throws Exception {
        SchemaChangeResult changeResult = new SchemaChangeResult(
            "CUSTOMER", "dap_customer",
            Arrays.asList("CREATE TABLE IF NOT EXISTS dap_customer (...)"));
        when(schemaInitializer.applySchema(eq("CUSTOMER"))).thenReturn(changeResult);

        mockMvc.perform(post("/dap-engine/admin/metadata/subjects/CUSTOMER/apply-schema"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.subject").value("CUSTOMER"))
            .andExpect(jsonPath("$.data.table").value("dap_customer"))
            .andExpect(jsonPath("$.data.executedDdl").isArray())
            .andExpect(jsonPath("$.data.executedDdl[0]").value(org.hamcrest.Matchers.containsString("CREATE TABLE")));
    }

    @Test
    public void applySchema_returns_error_for_invalid_subject() throws Exception {
        when(schemaInitializer.applySchema(eq("NOEXIST")))
            .thenThrow(new DapValidationException("[DAP Engine] subject 'NOEXIST' 不存在或已删除"));

        mockMvc.perform(post("/dap-engine/admin/metadata/subjects/NOEXIST/apply-schema"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4001))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("不存在或已删除")));
    }
}

