package com.ruijie.authzengine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.assembler.GovernanceAssembler;
import com.ruijie.authzengine.application.service.CatalogAppService;
import com.ruijie.authzengine.application.service.MetaAppService;
import com.ruijie.authzengine.application.service.ResourceAppService;
import com.ruijie.authzengine.application.service.SubjectAppService;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.domain.repository.SubjectRepository;
import com.ruijie.authzengine.shared.exception.GlobalExceptionHandler;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectoryControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldUpsertAndListUserDirectory() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("staffNo", "U100");
        payload.put("userId", "zhangsan");
        payload.put("staffName", "张三");
        payload.put("orgCode", "ORG-SALES");

        mockMvc.perform(post("/authz-engine/api/v1/governance/subjects/users/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.accepted").value(true))
            .andExpect(jsonPath("$.data.businessId").value("U100"));
    }

    @Test
    void shouldValidateApiDirectoryRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("apiCode", "API-CHECK");
        payload.put("apiName", "单次鉴权接口");
        payload.put("httpMethod", "POST");
        payload.put("uriPattern", "");

        mockMvc.perform(post("/authz-engine/api/v1/governance/resources/apis/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("AUTHZ-400"))
            .andExpect(jsonPath("$.message", containsString("URI 模式不能为空")));
    }

    @Test
    void shouldUpsertAndListApiDirectory() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("apiCode", "API-CHECK");
        payload.put("httpMethod", "POST");
        payload.put("uriPattern", "/authz-engine/api/v1/authz/check");

        mockMvc.perform(post("/authz-engine/api/v1/governance/resources/apis/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.accepted").value(true))
            .andExpect(jsonPath("$.data.businessId").value("API-CHECK"));
    }

    private MockMvc buildMockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(
                new DirectoryController(buildCatalogAppService(), new GovernanceAssembler())
            )
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    private CatalogAppService buildCatalogAppService() {
        MetaRepository metaRepository = new MetaRepository() {
            @Override
            public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
                return definition;
            }

            @Override
            public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
                return definition;
            }

            @Override
            public List<StandardActionDefinition> listStandardActions(String tenantId) {
                return java.util.Collections.emptyList();
            }

            @Override
            public List<StandardPolicyTemplateDefinition> listStandardPolicyTemplates(String tenantId) {
                return java.util.Collections.emptyList();
            }
        };
        SubjectRepository subjectRepository = new SubjectRepository() {
            private final Map<String, SysUserAccount> users = new LinkedHashMap<>();

            @Override
            public SysUserAccount saveUser(SysUserAccount userAccount) {
                users.put(userAccount.getStaffNo(), userAccount);
                return userAccount;
            }

            @Override
            public List<SysUserAccount> listUsers(String tenantId, String appCode) {
                return users.values().stream().filter(item -> tenantId.equals(item.getTenantId()) && appCode.equals(item.getAppCode())).collect(Collectors.toList());
            }
        };
        ResourceRepository resourceRepository = new ResourceRepository() {
            private final Map<String, SysResApi> apis = new LinkedHashMap<>();

            @Override
            public SysResApi saveApi(SysResApi sysResApi) {
                apis.put(sysResApi.getApiCode(), sysResApi);
                return sysResApi;
            }

            @Override
            public List<SysResApi> listApis(String tenantId, String appCode) {
                return apis.values().stream().filter(item -> tenantId.equals(item.getTenantId()) && appCode.equals(item.getAppCode())).collect(Collectors.toList());
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
}