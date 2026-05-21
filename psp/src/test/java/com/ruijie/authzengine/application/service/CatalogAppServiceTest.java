package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.domain.repository.SubjectRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CatalogAppServiceTest {

    private static final String VALID_BO_SCHEMA_JSON = "{"
        + "\"entities\":[{"
        + "\"code\":\"contract_main\","
        + "\"name\":\"合同主实体\","
        + "\"isPrimary\":true,"
        + "\"tableName\":\"biz_contract\","
        + "\"attributes\":["
        + "{\"code\":\"id\",\"fieldName\":\"id\",\"columnName\":\"id\",\"name\":\"主键\",\"type\":\"LONG\",\"isPk\":true},"
        + "{\"code\":\"dept_id\",\"fieldName\":\"deptId\",\"columnName\":\"dept_id\",\"name\":\"部门\",\"type\":\"STRING\",\"isPk\":false,\"filterable\":true}"
        + "]}],"
        + "\"operations\":[{\"code\":\"READ\",\"name\":\"查询\",\"scope\":\"BO\"}]"
        + "}";

    @Test
    void shouldAssembleMetaAndDirectoryOperations() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        InMemorySubjectRepository subjectRepository = new InMemorySubjectRepository();
        InMemoryResourceRepository resourceRepository = new InMemoryResourceRepository();
        CatalogAppService catalogAppService = new CatalogAppService(
            new MetaAppService(metaRepository),
            new SubjectAppService(subjectRepository,
                new MetaRepository() {
                    @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
                    @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
                },
                com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter.noop()),
            new ResourceAppService(resourceRepository,
                new MetaRepository() {
                    @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
                    @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
                },
                com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter.noop())
        );

        AuthMetaModelDefinition metaModel = catalogAppService.registerMetaModel(AuthMetaModelDefinition.builder()
            .tenantId("T001")
            .appCode("CRM")
            .modelCode("RES_API")
            .modelName("接口资源")
            .category("RESOURCE")
            .adapterType("JAVA_BEAN")
            .resolver("noopHook")
            .build());
        BoMetaModelDefinition boMetaModel = catalogAppService.registerBoMetaModel(BoMetaModelDefinition.builder()
            .tenantId("T001")
            .appCode("CRM")
            .boCode("CONTRACT")
            .boName("合同")
            .schemaJson(VALID_BO_SCHEMA_JSON)
            .adapterType("JAVA_BEAN")
            .resolver("noopHook")
            .build());
        SysUserAccount userAccount = catalogAppService.upsertUser(SysUserAccount.builder()
            .tenantId("T001")
            .appCode("CRM")
            .staffNo("U100")
            .userId("zhangsan")
            .staffName("张三")
            .orgCode("ORG-SALES")
            .status("ENABLED")
            .build());
        SysResApi sysResApi = catalogAppService.upsertApi(SysResApi.builder()
            .tenantId("T001")
            .appCode("CRM")
            .apiCode("API-CONTRACT-LIST")
            .apiName("合同列表接口")
            .httpMethod("GET")
            .uriPattern("/api/contracts")
            .status("ENABLED")
            .build());

        Assertions.assertEquals("RES_API", metaModel.getModelCode());
        Assertions.assertEquals("CONTRACT", boMetaModel.getBoCode());
        Assertions.assertEquals("U100", userAccount.getStaffNo());
        Assertions.assertEquals("API-CONTRACT-LIST", sysResApi.getApiCode());
        Assertions.assertEquals(1, catalogAppService.listStandardActions("T001").size());
        Assertions.assertEquals(1, catalogAppService.listStandardPolicyTemplates("T001").size());
        Assertions.assertEquals(1, catalogAppService.listUsers("T001", "CRM").size());
        Assertions.assertEquals(1, catalogAppService.listApis("T001", "CRM").size());
    }

    private static class InMemoryMetaRepository implements MetaRepository {

        private final Map<String, AuthMetaModelDefinition> metaModels = new LinkedHashMap<>();
        private final Map<String, BoMetaModelDefinition> boMetaModels = new LinkedHashMap<>();
        private final List<StandardActionDefinition> standardActions = new ArrayList<>();
        private final List<StandardPolicyTemplateDefinition> standardPolicyTemplates = new ArrayList<>();

        private InMemoryMetaRepository() {
            standardActions.add(StandardActionDefinition.builder()
                .tenantId("__GLOBAL__")
                .actCode("READ")
                .actName("查看")
                .actType("STANDARD")
                .resCategory("ALL")
                .riskLevel(1)
                .build());
            standardPolicyTemplates.add(StandardPolicyTemplateDefinition.builder()
                .tenantId("__GLOBAL__")
                .templateCode("ENV_WORK_HOUR")
                .templateName("工作时间限制")
                .polType("ENV")
                .status("ENABLED")
                .build());
        }

        @Override
        public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
            metaModels.put(definition.getTenantId() + ":" + definition.getAppCode() + ":" + definition.getModelCode(), definition);
            return definition;
        }

        @Override
        public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
            if (definition.getId() == null) {
                definition.setId((long) (boMetaModels.size() + 1));
            }
            boMetaModels.put(definition.getTenantId() + ":" + definition.getAppCode() + ":" + definition.getBoCode(), definition);
            return definition;
        }

        @Override
        public BoMetaModelDefinition findBoMetaModel(String tenantId, String appCode, String boCode) {
            return boMetaModels.get(tenantId + ":" + appCode + ":" + boCode);
        }

        @Override
        public List<StandardActionDefinition> listStandardActions(String tenantId) {
            return standardActions.stream()
                .filter(item -> "__GLOBAL__".equals(item.getTenantId()) || tenantId.equals(item.getTenantId()))
                .collect(Collectors.toList());
        }

        @Override
        public List<StandardPolicyTemplateDefinition> listStandardPolicyTemplates(String tenantId) {
            return standardPolicyTemplates.stream()
                .filter(item -> "__GLOBAL__".equals(item.getTenantId()) || tenantId.equals(item.getTenantId()))
                .collect(Collectors.toList());
        }
    }

    private static class InMemorySubjectRepository implements SubjectRepository {

        private final Map<String, SysUserAccount> users = new LinkedHashMap<>();

        @Override
        public SysUserAccount saveUser(SysUserAccount userAccount) {
            users.put(userAccount.getTenantId() + ":" + userAccount.getAppCode() + ":" + userAccount.getStaffNo(), userAccount);
            return userAccount;
        }

        @Override
        public List<SysUserAccount> listUsers(String tenantId, String appCode) {
            return users.values().stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && appCode.equals(item.getAppCode()))
                .collect(Collectors.toList());
        }
    }

    private static class InMemoryResourceRepository implements ResourceRepository {

        private final Map<String, SysResApi> apis = new LinkedHashMap<>();

        @Override
        public SysResApi saveApi(SysResApi sysResApi) {
            apis.put(sysResApi.getTenantId() + ":" + sysResApi.getAppCode() + ":" + sysResApi.getApiCode(), sysResApi);
            return sysResApi;
        }

        @Override
        public List<SysResApi> listApis(String tenantId, String appCode) {
            return apis.values().stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && appCode.equals(item.getAppCode()))
                .collect(Collectors.toList());
        }
    }
}