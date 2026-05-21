package com.ruijie.authzengine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.assembler.DelegationAssembler;
import com.ruijie.authzengine.api.assembler.OpsAssembler;
import com.ruijie.authzengine.application.service.DelegationAppService;
import com.ruijie.authzengine.domain.model.common.DelegationStatus;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.ops.AssignmentDelegate;
import com.ruijie.authzengine.domain.repository.DelegationRepository;
import com.ruijie.authzengine.shared.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DelegationControllerTest {

    private static final long LARGE_DELEGATION_ID = 2042474007605948400L;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        InMemoryDelegationRepository delegationRepository = new InMemoryDelegationRepository();
        mockMvc = buildMockMvc(delegationRepository);
    }

    @Test
    void shouldCreatePageGetAndRevokeDelegation() throws Exception {
        String payload = "{"
            + "\"tenantId\":\"T001\"," 
            + "\"appCode\":\"CRM\"," 
            + "\"grantorSubjectModel\":\"SUB_USER\"," 
            + "\"grantorSubjectId\":\"approver-a\"," 
            + "\"delegateSubjectModel\":\"SUB_USER\"," 
            + "\"delegateSubjectId\":\"approver-b\"," 
            + "\"permissionCode\":\"CONTRACT_APPROVE\"," 
            + "\"startTime\":\"2026-04-02T09:00:00\"," 
            + "\"endTime\":\"2026-04-03T09:00:00\"," 
            + "\"reason\":\"请假期间委托\""
            + "}";

        mockMvc.perform(post("/authz-engine/api/v1/governance/delegations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.delegationId").value("1"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/delegations")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].status").value("ACTIVE"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/delegations/1")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.grantorSubjectId").value("approver-a"));

        mockMvc.perform(post("/authz-engine/api/v1/governance/delegations/1/revoke")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.delegationId").value("1"))
            .andExpect(jsonPath("$.data.status").value("REVOKED"));
    }

    @Test
    void shouldSupportRevokeCompatEndpointInOpsController() throws Exception {
        String payload = "{"
            + "\"tenantId\":\"T001\"," 
            + "\"appCode\":\"CRM\"," 
            + "\"grantorSubjectModel\":\"SUB_USER\"," 
            + "\"grantorSubjectId\":\"approver-a\"," 
            + "\"delegateSubjectModel\":\"SUB_USER\"," 
            + "\"delegateSubjectId\":\"approver-b\"," 
            + "\"permissionCode\":\"CONTRACT_APPROVE\"," 
            + "\"startTime\":\"2026-04-02T09:00:00\"," 
            + "\"endTime\":\"2026-04-03T09:00:00\"," 
            + "\"reason\":\"请假期间委托\""
            + "}";

        mockMvc.perform(post("/authz-engine/api/v1/governance/delegations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/authz-engine/api/v1/governance/delegations/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"delegationId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.delegationId").value("1"))
            .andExpect(jsonPath("$.data.status").value("REVOKED"));
    }

    @Test
    void shouldSerializeLargeDelegationIdAsString() throws Exception {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        InMemoryDelegationRepository delegationRepository = new InMemoryDelegationRepository(LARGE_DELEGATION_ID);
        mockMvc = buildMockMvc(delegationRepository);

        String payload = "{"
            + "\"tenantId\":\"T001\"," 
            + "\"appCode\":\"CRM\"," 
            + "\"grantorSubjectModel\":\"SUB_USER\"," 
            + "\"grantorSubjectId\":\"approver-a\"," 
            + "\"delegateSubjectModel\":\"SUB_USER\"," 
            + "\"delegateSubjectId\":\"approver-b\"," 
            + "\"permissionCode\":\"CONTRACT_APPROVE\"," 
            + "\"startTime\":\"2026-04-02T09:00:00\"," 
            + "\"endTime\":\"2026-04-03T09:00:00\"," 
            + "\"reason\":\"请假期间委托\""
            + "}";

        mockMvc.perform(post("/authz-engine/api/v1/governance/delegations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.delegationId").value(String.valueOf(LARGE_DELEGATION_ID)));
    }

    @Test
    void shouldListGrantablePermissionCodes() throws Exception {
        mockMvc.perform(get("/authz-engine/api/v1/governance/delegations/grantable-permissions")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("grantorSubjectModel", "SUB_USER")
                .param("grantorSubjectId", "approver-a")
                .param("effectiveAt", "2026-04-02T09:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data[0]").value("CONTRACT_APPROVE"));
    }

    private MockMvc buildMockMvc(InMemoryDelegationRepository delegationRepository) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        DelegationController delegationController = new DelegationController(
            new DelegationAppService(delegationRepository),
            new DelegationAssembler()
        );
        OpsController opsController = new OpsController(
            new DelegationAppService(delegationRepository),
            new OpsAssembler()
        );
        return MockMvcBuilders.standaloneSetup(delegationController, opsController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    private static final class InMemoryDelegationRepository implements DelegationRepository {

        private final AtomicLong idGenerator;

        private final List<AssignmentDelegate> values = new ArrayList<>();

        private InMemoryDelegationRepository() {
            this(1L);
        }

        private InMemoryDelegationRepository(long initialId) {
            this.idGenerator = new AtomicLong(initialId);
        }

        @Override
        public boolean hasActiveGrantPermission(
            String tenantId,
            String appCode,
            String grantorSubjectModel,
            String grantorSubjectId,
            String permissionCode,
            LocalDateTime effectiveAt
        ) {
            return "T001".equals(tenantId)
                && "CRM".equals(appCode)
                && "SUB_USER".equals(grantorSubjectModel)
                && "approver-a".equals(grantorSubjectId)
                && "CONTRACT_APPROVE".equals(permissionCode)
                && effectiveAt != null;
        }

        @Override
        public List<String> listGrantablePermissionCodes(
            String tenantId,
            String appCode,
            String grantorSubjectModel,
            String grantorSubjectId,
            LocalDateTime effectiveAt
        ) {
            if (hasActiveGrantPermission(
                tenantId,
                appCode,
                grantorSubjectModel,
                grantorSubjectId,
                "CONTRACT_APPROVE",
                effectiveAt
            )) {
                return Collections.singletonList("CONTRACT_APPROVE");
            }
            return Collections.emptyList();
        }

        @Override
        public AssignmentDelegate save(AssignmentDelegate assignmentDelegate) {
            assignmentDelegate.setDelegationId(idGenerator.getAndIncrement());
            values.add(assignmentDelegate);
            return assignmentDelegate;
        }

        @Override
        public AssignmentDelegate revoke(String tenantId, String appCode, Long delegationId) {
            AssignmentDelegate assignmentDelegate = findDelegation(tenantId, appCode, delegationId);
            if (assignmentDelegate == null) {
                throw new IllegalStateException("delegation not found");
            }
            assignmentDelegate.setStatus(DelegationStatus.REVOKED.name());
            return assignmentDelegate;
        }

        @Override
        public PageResult<AssignmentDelegate> pageDelegations(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            List<AssignmentDelegate> records = values.stream()
                .filter(item -> tenantId.equals(item.getTenantId()))
                .filter(item -> appCode.equals(item.getAppCode()))
                .filter(item -> keyword == null || item.getGrantorSubjectId().contains(keyword)
                    || item.getDelegateSubjectId().contains(keyword)
                    || (item.getReason() != null && item.getReason().contains(keyword)))
                .collect(Collectors.toList());
            return PageResult.<AssignmentDelegate>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(records.size())
                .records(records)
                .build();
        }

        @Override
        public AssignmentDelegate findDelegation(String tenantId, String appCode, Long delegationId) {
            return values.stream()
                .filter(item -> tenantId.equals(item.getTenantId()))
                .filter(item -> appCode.equals(item.getAppCode()))
                .filter(item -> delegationId.equals(item.getDelegationId()))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<AssignmentDelegate> findActiveDelegations(
            String tenantId,
            String appCode,
            String delegateSubjectModel,
            String delegateSubjectId,
            LocalDateTime effectiveAt
        ) {
            return Collections.emptyList();
        }
    }

}
