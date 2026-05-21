package com.ruijie.authzengine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.assembler.GovernanceAssembler;
import com.ruijie.authzengine.api.assembler.MetaAssembler;
import com.ruijie.authzengine.application.spi.BoMetaModelAdapter;
import com.ruijie.authzengine.application.spi.BoSchemaColumnInfo;
import com.ruijie.authzengine.application.spi.NativeBoSchemaCollector;
import com.ruijie.authzengine.application.service.CatalogAppService;
import com.ruijie.authzengine.application.service.MetaAppService;
import com.ruijie.authzengine.application.service.ResourceAppService;
import com.ruijie.authzengine.application.service.SubjectAppService;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.domain.repository.SubjectRepository;
import com.ruijie.authzengine.infrastructure.authz.BoResolverRouter;
import com.ruijie.authzengine.shared.exception.GlobalExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
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

class MetaControllerTest {

    private static final String VALID_BO_SCHEMA_JSON = "{"
        + "\"entities\":[{"
        + "\"code\":\"opportunity_main\","
        + "\"name\":\"商机主实体\","
        + "\"isPrimary\":true,"
        + "\"tableName\":\"biz_opportunity\","
        + "\"attributes\":["
        + "{\"code\":\"id\",\"fieldName\":\"id\",\"columnName\":\"id\",\"name\":\"主键\",\"type\":\"LONG\",\"isPk\":true},"
        + "{\"code\":\"owner_id\",\"fieldName\":\"ownerId\",\"columnName\":\"owner_id\",\"name\":\"负责人\",\"type\":\"STRING\",\"isPk\":false,\"filterable\":true}"
        + "]}],"
        + "\"operations\":[{\"code\":\"READ\",\"name\":\"查询\",\"scope\":\"BO\"}]"
        + "}";

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldRegisterMetaModelWithApiResponse() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("modelCode", "RES_API");
        payload.put("category", "RESOURCE");
        payload.put("resolver", "noopHook");

        mockMvc.perform(post("/authz-engine/api/v1/governance/meta-models/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.accepted").value(true))
            .andExpect(jsonPath("$.data.businessId").value("RES_API"));
    }

    @Test
    void shouldValidateMetaModelRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("modelCode", "");
        payload.put("category", "RESOURCE");
        payload.put("resolver", "noopHook");

        mockMvc.perform(post("/authz-engine/api/v1/governance/meta-models/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-400"))
            .andExpect(jsonPath("$.message", containsString("模型编码不能为空")));
    }

    @Test
    void shouldExposeStandardCatalogEndpoints() throws Exception {
        mockMvc.perform(get("/authz-engine/api/v1/governance/actions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records[0].actCode").value("READ"))
            .andExpect(jsonPath("$.data.records[0].riskLevel").value(1));

        mockMvc.perform(get("/authz-engine/api/v1/governance/actions").param("tenantId", "T001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records[1].actCode").value("EXPORT"))
            .andExpect(jsonPath("$.data.records[1].riskLevel").value(2));

        mockMvc.perform(get("/authz-engine/api/v1/governance/policy-templates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records[0].templateCode").value("ENV_WORK_HOUR"))
            .andExpect(jsonPath("$.data.records[0].polType").value("ENV"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/policy-templates").param("tenantId", "T001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records[1].templateCode").value("DATA_DEPT"))
            .andExpect(jsonPath("$.data.records[1].polType").value("DATA"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/actions/catalog").param("tenantId", "T001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data[1].actCode").value("EXPORT"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/policy-templates/catalog").param("tenantId", "T001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data[1].templateCode").value("DATA_DEPT"));
    }

    @Test
    void shouldHandleMetaModelCrudEndpoints() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("modelCode", "RES_DATA_BO");
        payload.put("modelName", "数据资源");
        payload.put("category", "RESOURCE");
        payload.put("resolver", "boHook");

        mockMvc.perform(post("/authz-engine/api/v1/governance/meta-models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.businessId").value("RES_DATA_BO"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/meta-models")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records").exists());

        mockMvc.perform(get("/authz-engine/api/v1/governance/meta-models/UNKNOWN_MODEL")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-404"));

        payload.put("modelName", "数据资源模型");
        mockMvc.perform(put("/authz-engine/api/v1/governance/meta-models/UNKNOWN_MODEL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-404"));

        mockMvc.perform(delete("/authz-engine/api/v1/governance/meta-models/UNKNOWN_MODEL")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-404"));
    }

    @Test
    void shouldAllowMetaModelWithoutHookResolverWhenHookDisabled() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("modelCode", "RES_MENU_ROUTE");
        payload.put("modelName", "菜单资源");
        payload.put("category", "RESOURCE");

        mockMvc.perform(post("/authz-engine/api/v1/governance/meta-models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.businessId").value("RES_MENU_ROUTE"));
    }

    @Test
    void shouldHandleBoMetaModelCrudEndpoints() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("boCode", "OPPORTUNITY");
        payload.put("boName", "商机");
        payload.put("schemaJson", VALID_BO_SCHEMA_JSON);
        payload.put("adapterType", "JAVA_BEAN");
        payload.put("resolver", "opportunityHook");

        mockMvc.perform(post("/authz-engine/api/v1/governance/bo-models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.businessId").value("OPPORTUNITY"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/bo-models")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records").exists());

        mockMvc.perform(get("/authz-engine/api/v1/governance/bo-models/UNKNOWN_BO")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-404"));

        payload.put("boName", "商机对象");
        mockMvc.perform(put("/authz-engine/api/v1/governance/bo-models/UNKNOWN_BO")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-404"));

        mockMvc.perform(delete("/authz-engine/api/v1/governance/bo-models/UNKNOWN_BO")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-404"));
    }

    @Test
    void shouldAllowBoMetaModelWithoutHookResolverWhenHookDisabled() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("boCode", "OPPORTUNITY_NO_HOOK");
        payload.put("boName", "商机无 Hook");
        payload.put("schemaJson", VALID_BO_SCHEMA_JSON);

        mockMvc.perform(post("/authz-engine/api/v1/governance/bo-models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.businessId").value("OPPORTUNITY_NO_HOOK"));
    }

    @Test
    void shouldRejectBoHookWithoutAdapterType() throws Exception {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", "T001");
            payload.put("appCode", "CRM");
            payload.put("boCode", "OPPORTUNITY_HOOK");
            payload.put("boName", "商机 Hook");
            payload.put("schemaJson", VALID_BO_SCHEMA_JSON);
            payload.put("resolver", "opportunityHook");

            mockMvc.perform(post("/authz-engine/api/v1/governance/bo-models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTHZ-400"))
                .andExpect(jsonPath("$.message", containsString("adapterType")));
    }

    @Test
    void shouldRejectInvalidBoSchemaJson() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("boCode", "OPPORTUNITY_SCHEMA");
        payload.put("boName", "商机 Schema");
        payload.put("adapterType", "JAVA_BEAN");
        payload.put("resolver", "opportunityHook");
        payload.put("schemaJson", "{bad json}");

        mockMvc.perform(post("/authz-engine/api/v1/governance/bo-models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-400"))
            .andExpect(jsonPath("$.message", containsString("schemaJson")));
    }

    @Test
    void shouldRejectFieldPolicyTemplateWithoutTargetFieldContractAtController() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("templateCode", "FIELD_MASK_MOBILE");
        payload.put("templateName", "手机号脱敏");
        payload.put("polType", "FIELD");
        payload.put("expressionScript", "return true;");
        payload.put("paramSchema", "{\"action\":\"MASK\",\"properties\":{}}");
        payload.put("status", "ENABLED");

        mockMvc.perform(post("/authz-engine/api/v1/governance/policy-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-400"))
            .andExpect(jsonPath("$.message", containsString("targetField")));
    }

    private MockMvc buildMockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        CatalogAppService catalogAppService = buildCatalogAppService();
        MetaAppService metaAppService = buildMetaAppService();
        return MockMvcBuilders.standaloneSetup(
                new MetaController(
                    catalogAppService,
                    metaAppService,
                    new MetaAssembler(),
                    new GovernanceAssembler(),
                    new MockEnvironment()
                )
            )
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    private CatalogAppService buildCatalogAppService() {
        MetaRepository metaRepository = new MetaRepository() {
            private final Map<String, AuthMetaModelDefinition> metaModels = new LinkedHashMap<>();
            private final Map<String, BoMetaModelDefinition> boMetaModels = new LinkedHashMap<>();
            private final List<StandardActionDefinition> actionDefinitions = new ArrayList<>();
            private final List<StandardPolicyTemplateDefinition> policyTemplateDefinitions = new ArrayList<>();

            {
                actionDefinitions.add(StandardActionDefinition.builder()
                    .tenantId("__GLOBAL__")
                    .actCode("READ")
                    .actName("查看")
                    .actType("STANDARD")
                    .resCategory("ALL")
                    .riskLevel(1)
                    .build());
                actionDefinitions.add(StandardActionDefinition.builder()
                    .tenantId("T001")
                    .actCode("EXPORT")
                    .actName("导出")
                    .actType("BIZ")
                    .resCategory("API")
                    .riskLevel(2)
                    .build());
                policyTemplateDefinitions.add(StandardPolicyTemplateDefinition.builder()
                    .tenantId("__GLOBAL__")
                    .templateCode("ENV_WORK_HOUR")
                    .templateName("工作时间限制")
                    .polType("ENV")
                    .status("ENABLED")
                    .build());
                policyTemplateDefinitions.add(StandardPolicyTemplateDefinition.builder()
                    .tenantId("T001")
                    .templateCode("DATA_DEPT")
                    .templateName("部门范围")
                    .polType("DATA")
                    .status("ENABLED")
                    .build());
            }

            @Override
            public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
                metaModels.put(definition.getModelCode(), definition);
                return definition;
            }


            @Override
            public com.ruijie.authzengine.domain.model.governance.PageResult<AuthMetaModelDefinition> pageAuthMetaModels(
                String tenantId, String appCode, String keyword, int pageNo, int pageSize
            ) {
                List<AuthMetaModelDefinition> records = metaModels.values().stream()
                    .filter(item -> tenantId.equals(item.getTenantId()) && appCode.equals(item.getAppCode()))
                    .collect(Collectors.toList());
                return com.ruijie.authzengine.domain.repository.MetaRepository.pageOf(records, pageNo, pageSize);
            }

            @Override
            public AuthMetaModelDefinition findAuthMetaModel(String tenantId, String appCode, String modelCode) {
                AuthMetaModelDefinition definition = metaModels.get(modelCode);
                if (definition == null) {
                    return null;
                }
                if (!tenantId.equals(definition.getTenantId()) || !appCode.equals(definition.getAppCode())) {
                    return null;
                }
                return definition;
            }

            @Override
            public void deleteAuthMetaModel(String tenantId, String appCode, String modelCode) {
                AuthMetaModelDefinition definition = findAuthMetaModel(tenantId, appCode, modelCode);
                if (definition != null) {
                    metaModels.remove(modelCode);
                }
            }

            @Override
            public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
                if (definition.getId() == null) {
                    definition.setId((long) (boMetaModels.size() + 1));
                }
                boMetaModels.put(definition.getBoCode(), definition);
                return definition;
            }


            @Override
            public com.ruijie.authzengine.domain.model.governance.PageResult<BoMetaModelDefinition> pageBoMetaModels(
                String tenantId, String appCode, String keyword, int pageNo, int pageSize
            ) {
                List<BoMetaModelDefinition> records = boMetaModels.values().stream()
                    .filter(item -> tenantId.equals(item.getTenantId()) && appCode.equals(item.getAppCode()))
                    .collect(Collectors.toList());
                return com.ruijie.authzengine.domain.repository.MetaRepository.pageOf(records, pageNo, pageSize);
            }

            @Override
            public BoMetaModelDefinition findBoMetaModel(String tenantId, String appCode, String boCode) {
                BoMetaModelDefinition definition = boMetaModels.get(boCode);
                if (definition == null) {
                    return null;
                }
                if (!tenantId.equals(definition.getTenantId()) || !appCode.equals(definition.getAppCode())) {
                    return null;
                }
                return definition;
            }

            @Override
            public void deleteBoMetaModel(String tenantId, String appCode, String boCode) {
                BoMetaModelDefinition definition = findBoMetaModel(tenantId, appCode, boCode);
                if (definition != null) {
                    boMetaModels.remove(boCode);
                }
            }

            @Override
            public List<StandardActionDefinition> listStandardActions(String tenantId) {
                return actionDefinitions.stream().filter(item -> "__GLOBAL__".equals(item.getTenantId()) || tenantId.equals(item.getTenantId())).collect(Collectors.toList());
            }

            @Override
            public List<StandardPolicyTemplateDefinition> listStandardPolicyTemplates(String tenantId) {
                return policyTemplateDefinitions.stream().filter(item -> "__GLOBAL__".equals(item.getTenantId()) || tenantId.equals(item.getTenantId())).collect(Collectors.toList());
            }
        };
        SubjectRepository subjectRepository = new SubjectRepository() {
            @Override
            public com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount saveUser(com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount userAccount) {
                return userAccount;
            }

            @Override
            public List<com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount> listUsers(String tenantId, String appCode) {
                return java.util.Collections.emptyList();
            }
        };
        ResourceRepository resourceRepository = new ResourceRepository() {
            @Override
            public com.ruijie.authzengine.domain.model.governance.resource.SysResApi saveApi(com.ruijie.authzengine.domain.model.governance.resource.SysResApi sysResApi) {
                return sysResApi;
            }

            @Override
            public List<com.ruijie.authzengine.domain.model.governance.resource.SysResApi> listApis(String tenantId, String appCode) {
                return java.util.Collections.emptyList();
            }
        };
        return new CatalogAppService(
            new MetaAppService(metaRepository),
            new SubjectAppService(subjectRepository,
                new com.ruijie.authzengine.domain.repository.MetaRepository() {
                    @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
                    @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
                },
                com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter.noop()),
            new ResourceAppService(resourceRepository,
                new com.ruijie.authzengine.domain.repository.MetaRepository() {
                    @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
                    @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
                },
                com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter.noop())
        );
    }

    private MetaAppService buildMetaAppService() {
        MetaRepository metaRepository = new MetaRepository() {
            private final Map<String, AuthMetaModelDefinition> metaModels = new LinkedHashMap<>();
            private final Map<String, BoMetaModelDefinition> boMetaModels = new LinkedHashMap<>();
            private final List<StandardActionDefinition> actionDefinitions = new ArrayList<>();
            private final List<StandardPolicyTemplateDefinition> policyTemplateDefinitions = new ArrayList<>();

            {
                actionDefinitions.add(StandardActionDefinition.builder()
                    .tenantId("__GLOBAL__")
                    .actCode("READ")
                    .actName("查看")
                    .actType("STANDARD")
                    .resCategory("ALL")
                    .riskLevel(1)
                    .build());
                actionDefinitions.add(StandardActionDefinition.builder()
                    .tenantId("T001")
                    .actCode("EXPORT")
                    .actName("导出")
                    .actType("BIZ")
                    .resCategory("API")
                    .riskLevel(2)
                    .build());
                policyTemplateDefinitions.add(StandardPolicyTemplateDefinition.builder()
                    .tenantId("__GLOBAL__")
                    .templateCode("ENV_WORK_HOUR")
                    .templateName("工作时间限制")
                    .polType("ENV")
                    .status("ENABLED")
                    .build());
                policyTemplateDefinitions.add(StandardPolicyTemplateDefinition.builder()
                    .tenantId("T001")
                    .templateCode("DATA_DEPT")
                    .templateName("部门范围")
                    .polType("DATA")
                    .status("ENABLED")
                    .build());
            }

            @Override
            public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
                metaModels.put(definition.getModelCode(), definition);
                return definition;
            }

            @Override
            public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
                boMetaModels.put(definition.getBoCode(), definition);
                return definition;
            }

            @Override
            public List<StandardActionDefinition> listStandardActions(String tenantId) {
                return actionDefinitions.stream().filter(item -> "__GLOBAL__".equals(item.getTenantId()) || tenantId.equals(item.getTenantId())).collect(Collectors.toList());
            }

            @Override
            public List<StandardPolicyTemplateDefinition> listStandardPolicyTemplates(String tenantId) {
                return policyTemplateDefinitions.stream().filter(item -> "__GLOBAL__".equals(item.getTenantId()) || tenantId.equals(item.getTenantId())).collect(Collectors.toList());
            }
        };
        return new MetaAppService(metaRepository);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // T021：schema-preview 端点测试
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * T021-1：Native 模式 - schema-preview 返回采集器提供的列元数据。
     */
    @Test
    void shouldReturnColumnsFromNativeSchemaPreviewEndpoint() throws Exception {
        List<BoSchemaColumnInfo> nativeColumns = Arrays.asList(
            BoSchemaColumnInfo.builder()
                .tableName("biz_contract").columnName("id").columnType("BIGINT")
                .isPrimaryKey(true).nullable(false).comment("主键").build(),
            BoSchemaColumnInfo.builder()
                .tableName("biz_contract").columnName("dept_id").columnType("VARCHAR(64)")
                .isPrimaryKey(false).nullable(true).comment("部门").build()
        );
        NativeBoSchemaCollector nativeCollector = tableName -> nativeColumns;
        MockMvc schemaMockMvc = buildMockMvcWithSchemaPreview(null, nativeCollector);

        schemaMockMvc.perform(get("/authz-engine/api/v1/governance/bo-models/CONTRACT/schema-preview")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("tableName", "biz_contract")
                .param("mode", "NATIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].columnName").value("id"))
            .andExpect(jsonPath("$.data[0].primaryKey").value(true))
            .andExpect(jsonPath("$.data[0].columnType").value("BIGINT"))
            .andExpect(jsonPath("$.data[1].columnName").value("dept_id"))
            .andExpect(jsonPath("$.data[1].primaryKey").value(false));
    }

    /**
     * T021-2：Shadow 模式 - schema-preview 委托适配器采集列元数据。
     */
    @Test
    void shouldReturnColumnsFromShadowSchemaPreviewEndpoint() throws Exception {
        List<BoSchemaColumnInfo> shadowColumns = Collections.singletonList(
            BoSchemaColumnInfo.builder()
                .tableName("biz_salary").columnName("amount").columnType("DECIMAL(18,2)")
                .isPrimaryKey(false).nullable(false).comment("薪资金额").build()
        );
        BoMetaModelAdapter shadowAdapter = new BoMetaModelAdapter() {
            @Override
            public Map<String, Object> fetchInstanceAttributes(String instanceId, String schemaJson, Map<String, Object> requestContext) {
                return null;
            }
            @Override
            public List<BoSchemaColumnInfo> fetchBoSchema(String boCode, String tableName, Map<String, Object> hints) {
                return shadowColumns;
            }
        };
        BoResolverRouter testRouter = new BoResolverRouter(null) {
            @Override
            public BoMetaModelAdapter resolve(String adapterType, String resolver) {
                return "salaryBoProvider".equals(resolver) ? shadowAdapter : null;
            }
        };

        // 构造带有 SALARY BO 注册信息的 MockMvc
        MockMvc schemaMockMvc = buildMockMvcWithSchemaPreviewAndBo(testRouter, null,
            BoMetaModelDefinition.builder()
                .tenantId("T001").appCode("HR").boCode("SALARY")
                .boName("薪资").adapterType("JAVA_BEAN").resolver("salaryBoProvider")
                .schemaJson("{\"entities\":[]}")
                .build());

        schemaMockMvc.perform(get("/authz-engine/api/v1/governance/bo-models/SALARY/schema-preview")
                .param("tenantId", "T001")
                .param("appCode", "HR")
                .param("mode", "SHADOW"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].columnName").value("amount"))
            .andExpect(jsonPath("$.data[0].columnType").value("DECIMAL(18,2)"));
    }

    /**
     * T021-3：MANUAL 模式 - schema-preview 返回空列表。
     */
    @Test
    void shouldReturnEmptyListForManualSchemaPreview() throws Exception {
        MockMvc schemaMockMvc = buildMockMvcWithSchemaPreview(null, null);

        schemaMockMvc.perform(get("/authz-engine/api/v1/governance/bo-models/CONTRACT/schema-preview")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("mode", "MANUAL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    /**
     * T021-4：mode 参数省略时默认 MANUAL - 返回空列表。
     */
    @Test
    void shouldDefaultToManualModeWhenModeParamAbsent() throws Exception {
        MockMvc schemaMockMvc = buildMockMvcWithSchemaPreview(null, null);

        schemaMockMvc.perform(get("/authz-engine/api/v1/governance/bo-models/CONTRACT/schema-preview")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────────────

    /**
     * 构建带 schema-preview 能力的 MockMvc（不预置 BO 注册信息）。
     */
    private MockMvc buildMockMvcWithSchemaPreview(BoResolverRouter router, NativeBoSchemaCollector collector) {
        return buildMockMvcWithSchemaPreviewAndBo(router, collector, null);
    }

    /**
     * 构建带 schema-preview 能力的 MockMvc，可选预置 BO 注册信息。
     */
    private MockMvc buildMockMvcWithSchemaPreviewAndBo(
        BoResolverRouter router,
        NativeBoSchemaCollector collector,
        BoMetaModelDefinition preregisteredBo
    ) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        CatalogAppService catalogAppService = buildCatalogAppService();
        MetaAppService metaAppService = buildMetaAppServiceWithSchema(router, collector, preregisteredBo);
        return MockMvcBuilders.standaloneSetup(
                new MetaController(
                    catalogAppService,
                    metaAppService,
                    new MetaAssembler(),
                    new GovernanceAssembler(),
                    new MockEnvironment()
                )
            )
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    /**
     * 构建带 schema-preview 能力的 MetaAppService。
     */
    private MetaAppService buildMetaAppServiceWithSchema(
        BoResolverRouter router,
        NativeBoSchemaCollector collector,
        BoMetaModelDefinition preregisteredBo
    ) {
        MetaRepository metaRepository = new MetaRepository() {
            private final Map<String, BoMetaModelDefinition> boMetaModels = new LinkedHashMap<>();
            private final List<StandardActionDefinition> actionDefinitions = new ArrayList<>();

            {
                actionDefinitions.add(StandardActionDefinition.builder()
                    .tenantId("__GLOBAL__").actCode("READ").actName("查看")
                    .actType("STANDARD").resCategory("ALL").riskLevel(1).build());
                if (preregisteredBo != null) {
                    boMetaModels.put(
                        preregisteredBo.getTenantId() + ":" + preregisteredBo.getAppCode() + ":" + preregisteredBo.getBoCode(),
                        preregisteredBo
                    );
                }
            }

            @Override
            public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
                return definition;
            }

            @Override
            public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
                boMetaModels.put(definition.getTenantId() + ":" + definition.getAppCode() + ":" + definition.getBoCode(), definition);
                return definition;
            }

            @Override
            public BoMetaModelDefinition findBoMetaModel(String tenantId, String appCode, String boCode) {
                return boMetaModels.get(tenantId + ":" + appCode + ":" + boCode);
            }

            @Override
            public List<StandardActionDefinition> listStandardActions(String tenantId) {
                return actionDefinitions.stream()
                    .filter(item -> "__GLOBAL__".equals(item.getTenantId()) || tenantId.equals(item.getTenantId()))
                    .collect(Collectors.toList());
            }

            @Override
            public List<StandardPolicyTemplateDefinition> listStandardPolicyTemplates(String tenantId) {
                return Collections.emptyList();
            }
        };
        return new MetaAppService(metaRepository,
            router != null ? router : BoResolverRouter.noop(),
            collector);
    }
}