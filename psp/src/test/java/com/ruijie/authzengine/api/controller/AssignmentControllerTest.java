package com.ruijie.authzengine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.assembler.AssignmentAssembler;
import com.ruijie.authzengine.application.service.AssignmentAppService;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.repository.AssignmentRepository;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.shared.exception.GlobalExceptionHandler;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

class AssignmentControllerTest {

    private static final String FIELD_SCHEMA_JSON = "{"
        + "\"entities\":[{"
        + "\"code\":\"customer_main\","
        + "\"isPrimary\":true,"
        + "\"tableName\":\"biz_customer\","
        + "\"attributes\":["
        + "{\"code\":\"id\",\"fieldName\":\"id\",\"columnName\":\"id\",\"type\":\"LONG\",\"isPk\":true},"
        + "{\"code\":\"mobile\",\"fieldName\":\"mobile\",\"columnName\":\"mobile\",\"type\":\"STRING\",\"fieldControl\":true},"
        + "{\"code\":\"nickname\",\"fieldName\":\"nickname\",\"columnName\":\"nickname\",\"type\":\"STRING\",\"fieldControl\":false}"
        + "]}]}";

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldCrudAssignmentsAndEnforceRecreateRule() throws Exception {
        mockMvc.perform(post("/authz-engine/api/v1/governance/permissions/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"subjectModel\":\"SUB_USER\",\"subjectId\":\"U100\",\"permItemId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/permissions/assignments")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/authz-engine/api/v1/governance/permissions/assignments/1")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.subjectId").value("U100"));

        mockMvc.perform(put("/authz-engine/api/v1/governance/permissions/assignments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"subjectModel\":\"SUB_USER\",\"subjectId\":\"U100\",\"permItemId\":1,\"policyParams\":\"{\\\"scope\\\":\\\"dept\\\"}\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(put("/authz-engine/api/v1/governance/permissions/assignments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"subjectModel\":\"SUB_USER\",\"subjectId\":\"U200\",\"permItemId\":1}"))
            .andExpect(status().is(409))
            .andExpect(jsonPath("$.code").value("AUTHZ-409-RELATION"))
            .andExpect(jsonPath("$.message", containsString("主体或权限项变更必须删除后重建")));

        mockMvc.perform(delete("/authz-engine/api/v1/governance/permissions/assignments/1")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void shouldRejectFieldAssignmentWhenTargetFieldIsNotControlled() throws Exception {
        mockMvc.perform(post("/authz-engine/api/v1/governance/permissions/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"subjectModel\":\"SUB_USER\",\"subjectId\":\"U100\",\"permItemId\":1,\"policyTemplateCode\":\"FIELD_MASK_MOBILE\",\"policyParams\":\"{\\\"targetField\\\":\\\"nickname\\\"}\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-400"))
            .andExpect(jsonPath("$.message", containsString("fieldControl")));
    }

    @Test
    void shouldRejectFieldAssignmentWhenUpdateTargetFieldIsNotControlled() throws Exception {
        // 先创建普通授权，再验证更新路径同样会触发 FIELD 绑定校验。
        mockMvc.perform(post("/authz-engine/api/v1/governance/permissions/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"subjectModel\":\"SUB_USER\",\"subjectId\":\"U100\",\"permItemId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(put("/authz-engine/api/v1/governance/permissions/assignments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"subjectModel\":\"SUB_USER\",\"subjectId\":\"U100\",\"permItemId\":1,\"policyTemplateCode\":\"FIELD_MASK_MOBILE\",\"policyParams\":\"{\\\"targetField\\\":\\\"nickname\\\"}\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-400"))
            .andExpect(jsonPath("$.message", containsString("fieldControl")));
    }

    private MockMvc buildMockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        AssignmentController controller = new AssignmentController(
            new AssignmentAppService(new InMemoryAssignmentRepository(), buildMetaRepository(), buildPermissionRepository()),
            new AssignmentAssembler()
        );
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    private MetaRepository buildMetaRepository() {
        return new MetaRepository() {
            @Override
            public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition d) {
                return d;
            }

            @Override
            public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition d) {
                return d;
            }

            @Override
            public StandardPolicyTemplateDefinition findStandardPolicyTemplate(String tenantId, String templateCode) {
                return StandardPolicyTemplateDefinition.builder()
                    .id(100L)
                    .tenantId(tenantId)
                    .templateCode(templateCode)
                    .templateName(templateCode)
                    .polType("FIELD")
                    .paramSchema("{\"properties\":{\"action\":{\"const\":\"MASK\"},\"targetField\":{\"type\":\"string\"}}}")
                    .status("ENABLED")
                    .build();
            }

            @Override
            public BoMetaModelDefinition findBoMetaModel(String tenantId, String appCode, String boCode) {
                return BoMetaModelDefinition.builder()
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .boCode(boCode)
                    .schemaJson(FIELD_SCHEMA_JSON)
                    .build();
            }
        };
    }

    private PermissionRepository buildPermissionRepository() {
        return new PermissionRepository() {
            @Override
            public AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem) {
                return permissionItem;
            }

            @Override
            public PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
                return PageResult.<AuthPermissionItem>builder().pageNo(pageNo).pageSize(pageSize).total(0).records(Collections.emptyList()).build();
            }

            @Override
            public AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode) {
                return null;
            }

            @Override
            public void deletePermissionItem(String tenantId, String appCode, String permCode) {
            }

            @Override
            public boolean hasPermissionItemReference(String tenantId, String appCode, String permCode) {
                return false;
            }

            @Override
            public List<AuthPermissionItem> findPermissionItemsByIds(String tenantId, String appCode, List<Long> permItemIds) {
                return Collections.singletonList(AuthPermissionItem.builder()
                    .id(permItemIds.get(0))
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .permCode("CRM:bo:CUSTOMER:READ")
                    .resModelCode("RES_DATA_BO")
                    .resId("1")
                    .actCode("READ")
                    .build());
            }
        };
    }

    private static final class InMemoryAssignmentRepository implements AssignmentRepository {

        private final AtomicLong idGenerator = new AtomicLong(1L);

        private final Map<Long, SysAuthAssignment> assignments = new LinkedHashMap<>();

        @Override
        public SysAuthAssignment saveAssignment(SysAuthAssignment assignment) {
            if (assignment.getId() == null) {
                assignment.setId(idGenerator.getAndIncrement());
            }
            assignments.put(assignment.getId(), assignment);
            return assignment;
        }

        @Override
        public PageResult<SysAuthAssignment> pageAssignments(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            List<SysAuthAssignment> records = assignments.values().stream().collect(Collectors.toList());
            return PageResult.<SysAuthAssignment>builder().pageNo(pageNo).pageSize(pageSize).total(records.size()).records(records).build();
        }

        @Override
        public SysAuthAssignment findAssignment(String tenantId, String appCode, Long assignmentId) {
            SysAuthAssignment assignment = assignments.get(assignmentId);
            if (assignment == null) {
                return null;
            }
            if (!tenantId.equals(assignment.getTenantId()) || !appCode.equals(assignment.getAppCode())) {
                return null;
            }
            return assignment;
        }

        @Override
        public void deleteAssignment(String tenantId, String appCode, Long assignmentId) {
            assignments.remove(assignmentId);
        }

        @Override
        public boolean hasAssignmentReference(String tenantId, String appCode, Long assignmentId) {
            return false;
        }
    }
}
