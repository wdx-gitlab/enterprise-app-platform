package com.ruijie.authzengine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.assembler.AuthzAuditAssembler;
import com.ruijie.authzengine.application.service.AuthzAuditAppService;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditPage;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditQuery;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;
import com.ruijie.authzengine.domain.repository.AuthzAuditRepository;
import com.ruijie.authzengine.shared.exception.GlobalExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthzAuditControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldQueryAuditLogsWithFiltersAndPagination() throws Exception {
        mockMvc.perform(get("/authz-engine/api/v1/governance/audit-logs")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("decision", "NOT_PERMIT")
                .param("pageNo", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].requestId").value("TRACE-DENY-001"))
            .andExpect(jsonPath("$.data.records[0].decision").value("NOT_PERMIT"));
    }

            @Test
            void shouldGetAuditLogDetail() throws Exception {
            mockMvc.perform(get("/authz-engine/api/v1/governance/audit-logs/2")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.auditLogId").value("2"))
                .andExpect(jsonPath("$.data.requestId").value("TRACE-DENY-001"));
            }

    @Test
    void shouldValidateMissingTenantIdForAuditLogs() throws Exception {
        mockMvc.perform(get("/authz-engine/api/v1/governance/audit-logs")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-400"))
            .andExpect(jsonPath("$.message", containsString("tenantId 不能为空")));
    }

    @Test
    void shouldValidateInvalidPagingArgumentsForAuditLogs() throws Exception {
        mockMvc.perform(get("/authz-engine/api/v1/governance/audit-logs")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("pageNo", "0")
                .param("pageSize", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-400"))
            .andExpect(jsonPath("$.message", containsString("pageNo 必须大于等于 1")));
    }

            @Test
            void shouldRejectNonNumericPagingArgumentsForAuditLogs() throws Exception {
            mockMvc.perform(get("/authz-engine/api/v1/governance/audit-logs")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("pageNo", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTHZ-400"))
                .andExpect(jsonPath("$.message", containsString("pageNo 参数格式不合法")));
            }

    @Test
    void shouldNotExposeWriteOperationsForAuditLogs() throws Exception {
        mockMvc.perform(post("/authz-engine/api/v1/governance/audit-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("AUTHZ-405"));

        mockMvc.perform(put("/authz-engine/api/v1/governance/audit-logs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("AUTHZ-405"));

        mockMvc.perform(delete("/authz-engine/api/v1/governance/audit-logs/1"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("AUTHZ-405"));
    }

    private MockMvc buildMockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        AuthzAuditController controller = new AuthzAuditController(
            new AuthzAuditAppService(new InMemoryAuthzAuditRepository()),
            new AuthzAuditAssembler()
        );
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    private static final class InMemoryAuthzAuditRepository implements AuthzAuditRepository {

        private final List<AuthzAuditRecord> values = new ArrayList<>();

        private final AtomicLong idGenerator = new AtomicLong(1L);

        private InMemoryAuthzAuditRepository() {
            values.add(AuthzAuditRecord.builder()
                .auditLogId(idGenerator.getAndIncrement())
                .requestId("TRACE-PERMIT-001")
                .tenantId("T001")
                .appCode("CRM")
                .subjectModel("SUB_USER")
                .subjectId("demo-user")
                .resourceModel("RES_DATA_BO")
                .resId("CONTRACT")
                .actionCode("APPROVE")
                .decision("PERMIT")
                .costMs(8L)
                .build());
            values.add(AuthzAuditRecord.builder()
                .auditLogId(idGenerator.getAndIncrement())
                .requestId("TRACE-DENY-001")
                .tenantId("T001")
                .appCode("CRM")
                .subjectModel("SUB_USER")
                .subjectId("demo-user")
                .resourceModel("RES_DATA_BO")
                .resId("CONTRACT")
                .actionCode("DELETE")
                .decision("NOT_PERMIT")
                .failureReason("NO_PERMISSION_ITEM")
                .costMs(5L)
                .build());
        }

        @Override
        public AuthzAuditPage query(AuthzAuditQuery query) {
            String tenantId = query.getTenantId();
            String appCode = query.getAppCode();
            String subjectId = query.getSubjectId();
            String resId = query.getResId();
            String actionCode = query.getActionCode();
            String decision = query.getDecision();
            List<AuthzAuditRecord> filtered = values.stream()
                .filter(item -> tenantId.equals(item.getTenantId()))
                .filter(item -> appCode.equals(item.getAppCode()))
                .filter(item -> decision == null || decision.equals(item.getDecision()))
                .filter(item -> subjectId == null || subjectId.equals(item.getSubjectId()))
                .filter(item -> resId == null || resId.equals(item.getResId()))
                .filter(item -> actionCode == null || actionCode.equals(item.getActionCode()))
                .collect(Collectors.toList());
            return AuthzAuditPage.builder()
                .records(filtered)
                .pageNo(query.getPageNo())
                .pageSize(query.getPageSize())
                .total(filtered.size())
                .build();
        }

        @Override
        public AuthzAuditRecord findById(String tenantId, String appCode, Long auditLogId) {
            return values.stream()
                .filter(item -> tenantId.equals(item.getTenantId()))
                .filter(item -> appCode.equals(item.getAppCode()))
                .filter(item -> auditLogId.equals(item.getAuditLogId()))
                .findFirst()
                .orElse(null);
        }
    }
}