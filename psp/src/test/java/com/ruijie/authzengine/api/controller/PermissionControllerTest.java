package com.ruijie.authzengine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.assembler.PermissionAssembler;
import com.ruijie.authzengine.application.service.PermissionCodeService;
import com.ruijie.authzengine.application.service.PermissionAppService;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.shared.exception.GlobalExceptionHandler;
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

class PermissionControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private InMemoryPermissionRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = new InMemoryPermissionRepository();
        mockMvc = buildMockMvc(repository);
    }

    @Test
    void shouldCrudPermissionItems() throws Exception {
        mockMvc.perform(post("/authz-engine/api/v1/governance/permissions/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"resourceModel\":\"RES_API\",\"resourceCode\":\"API-CONTRACT-QUERY\",\"actionCode\":\"READ\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("CRM:api:API-CONTRACT-QUERY:READ"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/permissions/items")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].permCode").value("CRM:api:API-CONTRACT-QUERY:READ"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/permissions/items/CRM:api:API-CONTRACT-QUERY:READ")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.actionCode").value("READ"));

        mockMvc.perform(put("/authz-engine/api/v1/governance/permissions/items/CRM:api:API-CONTRACT-QUERY:READ")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"resourceModel\":\"RES_API\",\"resourceCode\":\"API-CONTRACT-QUERY\",\"actionCode\":\"READ\",\"failStrategy\":\"ALLOW\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(delete("/authz-engine/api/v1/governance/permissions/items/CRM:api:API-CONTRACT-QUERY:READ")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("CRM:api:API-CONTRACT-QUERY:READ"));
    }

    @Test
    void shouldRejectDeletingReferencedPermissionItem() throws Exception {
        repository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode("CRM")
            .permCode("PERM-REF")
            .resModelCode("RES_API")
            .resId("API-REF")
            .actCode("READ")
            .build());
        repository.references.put("PERM-REF", true);

        mockMvc.perform(delete("/authz-engine/api/v1/governance/permissions/items/PERM-REF")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("AUTHZ-409-DELETE"))
            .andExpect(jsonPath("$.message", containsString("权限项仍被授权分配或委托引用")));
    }

    @Test
    void shouldPagePermissionItemsWithResourceFilters() throws Exception {
        repository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:bo:contract:READ")
            .resModelCode("RES_DATA_BO")
            .resId("1001")
            .actCode("READ")
            .build());
        repository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:api:contract.query:READ")
            .resModelCode("RES_API")
            .resId("contract.query")
            .actCode("READ")
            .build());

        mockMvc.perform(get("/authz-engine/api/v1/governance/permissions/items")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("resModelCode", "RES_DATA_BO")
                .param("resId", "1001")
                .param("keyword", "contract"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].permCode").value("CRM:bo:contract:READ"));
    }

    private MockMvc buildMockMvc(PermissionRepository permissionRepository) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        PermissionController controller = new PermissionController(
            new PermissionAppService(
                permissionRepository,
                new PermissionCodeService(new StubMetaRepository(), new StubResourceRepository())
            ),
            new PermissionAssembler()
        );
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    private static class StubMetaRepository implements MetaRepository {

        @Override
        public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
            return definition;
        }

        @Override
        public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
            return definition;
        }
    }

    private static class StubResourceRepository implements ResourceRepository {

        @Override
        public SysResApi findApi(String tenantId, String appCode, String apiCode) {
            return SysResApi.builder()
                .id(1L)
                .tenantId(tenantId)
                .appCode(appCode)
                .apiCode(apiCode)
                .apiName(apiCode)
                .build();
        }
    }

    private static final class InMemoryPermissionRepository implements PermissionRepository {

        private final AtomicLong idGenerator = new AtomicLong(1L);

        private final Map<String, AuthPermissionItem> items = new LinkedHashMap<>();

        private final Map<String, Boolean> references = new LinkedHashMap<>();

        @Override
        public AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem) {
            if (permissionItem.getId() == null) {
                permissionItem.setId(idGenerator.getAndIncrement());
            }
            items.put(permissionItem.getPermCode(), permissionItem);
            return permissionItem;
        }

        @Override
        public PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return pagePermissionItems(tenantId, appCode, keyword, null, null, pageNo, pageSize);
        }

        @Override
        public PageResult<AuthPermissionItem> pagePermissionItems(
                String tenantId,
                String appCode,
                String keyword,
                String resModelCode,
                String resId,
                int pageNo,
                int pageSize) {
            List<AuthPermissionItem> records = items.values().stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && appCode.equals(item.getAppCode()))
                .filter(item -> resModelCode == null || resModelCode.equals(item.getResModelCode()))
                .filter(item -> resId == null || resId.equals(item.getResId()))
                .filter(item -> keyword == null || item.getPermCode().contains(keyword))
                .collect(Collectors.toList());
            return PageResult.<AuthPermissionItem>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(records.size())
                .records(records)
                .build();
        }

        @Override
        public AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode) {
            AuthPermissionItem item = items.get(permCode);
            if (item == null) {
                return null;
            }
            if (!tenantId.equals(item.getTenantId()) || !appCode.equals(item.getAppCode())) {
                return null;
            }
            return item;
        }

        @Override
        public void deletePermissionItem(String tenantId, String appCode, String permCode) {
            items.remove(permCode);
        }

        @Override
        public boolean hasPermissionItemReference(String tenantId, String appCode, String permCode) {
            return Boolean.TRUE.equals(references.get(permCode));
        }
    }
}
