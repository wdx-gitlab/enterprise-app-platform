package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.application.spi.BoSchemaColumnInfo;
import com.ruijie.authzengine.application.spi.NativeBoSchemaCollector;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.infrastructure.authz.BoResolverRouter;
import com.ruijie.authzengine.shared.exception.BusinessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MetaAppServiceTest {

    private static final String CREATE_SCHEMA_JSON = "{"
        + "\"entities\":[{"
        + "\"code\":\"contract_main\","
        + "\"name\":\"合同主实体\","
        + "\"isPrimary\":true,"
        + "\"tableName\":\"biz_contract\","
        + "\"attributes\":["
        + "{\"code\":\"id\",\"fieldName\":\"id\",\"columnName\":\"id\",\"name\":\"主键\",\"type\":\"LONG\",\"isPk\":true},"
        + "{\"code\":\"dept_id\",\"fieldName\":\"deptId\",\"columnName\":\"dept_id\",\"name\":\"部门\",\"type\":\"STRING\",\"isPk\":false,\"filterable\":true}"
        + "]}],"
        + "\"operations\":["
        + "{\"code\":\"READ\",\"name\":\"查询\",\"scope\":\"BO\"},"
        + "{\"code\":\"EXPORT\",\"name\":\"导出\",\"scope\":\"BO\"}"
        + "]"
        + "}";

    private static final String UPDATE_SCHEMA_JSON = "{"
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
    void shouldSyncPermissionItemsWhenSavingBoMetaModel() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        MetaAppService metaAppService = new MetaAppService(metaRepository, permissionRepository);

        BoMetaModelDefinition saved = metaAppService.createBoMetaModel(BoMetaModelDefinition.builder()
            .tenantId("T001")
            .appCode("CRM")
            .boCode("CONTRACT")
            .boName("合同")
            .schemaJson(CREATE_SCHEMA_JSON)
            .adapterType("JAVA_BEAN")
            .resolver("noopHook")
            .build());

        Assertions.assertEquals(Long.valueOf(1L), saved.getId());
        Assertions.assertEquals(2, permissionRepository.savedItems.size());
        Assertions.assertTrue(permissionRepository.savedItems.containsKey("CRM:bo:CONTRACT:READ"));
        Assertions.assertTrue(permissionRepository.savedItems.containsKey("CRM:bo:CONTRACT:EXPORT"));
        Assertions.assertEquals("1", permissionRepository.savedItems.get("CRM:bo:CONTRACT:READ").getResId());
    }

    @Test
    void shouldRejectOperationOutsideStandardActionCatalog() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        MetaAppService metaAppService = new MetaAppService(metaRepository, new InMemoryPermissionRepository());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> metaAppService.createBoMetaModel(
            BoMetaModelDefinition.builder()
                .tenantId("T001")
                .appCode("CRM")
                .boCode("CONTRACT")
                .boName("合同")
                .schemaJson(CREATE_SCHEMA_JSON.replace("EXPORT", "APPROVE_CONTRACT"))
                .adapterType("JAVA_BEAN")
                .resolver("noopHook")
                .build()
        ));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    @Test
    void shouldBlockRemovingReferencedOperationOnUpdate() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        permissionRepository.referencedPermCodes.add("CRM:bo:CONTRACT:EXPORT");
        MetaAppService metaAppService = new MetaAppService(metaRepository, permissionRepository);

        metaAppService.createBoMetaModel(BoMetaModelDefinition.builder()
            .tenantId("T001")
            .appCode("CRM")
            .boCode("CONTRACT")
            .boName("合同")
            .schemaJson(CREATE_SCHEMA_JSON)
            .adapterType("JAVA_BEAN")
            .resolver("noopHook")
            .build());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> metaAppService.updateBoMetaModel(
            "T001",
            "CRM",
            "CONTRACT",
            BoMetaModelDefinition.builder()
                .boName("合同")
                .schemaJson(UPDATE_SCHEMA_JSON)
                .adapterType("JAVA_BEAN")
                .resolver("noopHook")
                .build()
        ));

        Assertions.assertEquals("AUTHZ-409-DELETE", exception.getCode());
        Assertions.assertTrue(permissionRepository.deletedPermCodes.isEmpty());
    }

    @Test
    void shouldRejectFieldPolicyTemplateWithoutTargetFieldContract() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        MetaAppService metaAppService = new MetaAppService(metaRepository, new InMemoryPermissionRepository());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> metaAppService.createStandardPolicyTemplate(
            StandardPolicyTemplateDefinition.builder()
                .tenantId("T001")
                .templateCode("FIELD_MASK_MOBILE")
                .templateName("手机号脱敏")
                .polType("FIELD")
                .paramSchema("{\"action\":\"MASK\",\"properties\":{}}")
                .status("ENABLED")
                .build()
        ));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    @Test
    void shouldRejectEnvPolicyTemplateReferencingSubjectNamespace() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        MetaAppService metaAppService = new MetaAppService(metaRepository, new InMemoryPermissionRepository());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> metaAppService.createStandardPolicyTemplate(
            StandardPolicyTemplateDefinition.builder()
                .tenantId("T001")
                .templateCode("ENV_VIP_ONLY")
                .templateName("VIP 灰度")
                .polType("ENV")
                .expressionScript("sub.staffName != null && sub.staffName.contains('VIP')")
                .status("ENABLED")
                .build()
        ));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
        Assertions.assertTrue(exception.getMessage().contains("sub"));
    }

    @Test
    void shouldRejectDataPolicyTemplateReferencingEnvNamespace() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        MetaAppService metaAppService = new MetaAppService(metaRepository, new InMemoryPermissionRepository());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> metaAppService.createStandardPolicyTemplate(
            StandardPolicyTemplateDefinition.builder()
                .tenantId("T001")
                .templateCode("DATA_ENV_MIX")
                .templateName("非法数据策略")
                .polType("DATA")
                .expressionScript("env.method == 'GET'")
                .status("ENABLED")
                .build()
        ));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
        Assertions.assertTrue(exception.getMessage().contains("env"));
    }

    @Test
    void shouldRejectFieldPolicyTemplateWithoutActionDefinition() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        MetaAppService metaAppService = new MetaAppService(metaRepository, new InMemoryPermissionRepository());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> metaAppService.createStandardPolicyTemplate(
            StandardPolicyTemplateDefinition.builder()
                .tenantId("T001")
                .templateCode("FIELD_MASK_MOBILE")
                .templateName("手机号脱敏")
                .polType("FIELD")
                .paramSchema("{\"properties\":{\"targetField\":{\"type\":\"string\"}}}")
                .status("ENABLED")
                .build()
        ));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    @Test
    void shouldRejectFieldPolicyTemplateWithUnsupportedAction() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        MetaAppService metaAppService = new MetaAppService(metaRepository, new InMemoryPermissionRepository());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> metaAppService.createStandardPolicyTemplate(
            StandardPolicyTemplateDefinition.builder()
                .tenantId("T001")
                .templateCode("FIELD_UNKNOWN")
                .templateName("未知动作")
                .polType("FIELD")
                .paramSchema("{\"action\":\"BLUR\",\"properties\":{\"targetField\":{\"type\":\"string\"}}}")
                .status("ENABLED")
                .build()
        ));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    @Test
    void shouldRejectFieldMaskTemplateWhenUsingLegacyBrokenScript() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        MetaAppService metaAppService = new MetaAppService(metaRepository, new InMemoryPermissionRepository());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> metaAppService.createStandardPolicyTemplate(
            StandardPolicyTemplateDefinition.builder()
                .tenantId("T001")
                .templateCode("FIELD_MASK_MOBILE")
                .templateName("手机号脱敏")
                .polType("FIELD")
                .expressionScript(FieldMaskScriptRules.LEGACY_BROKEN_MIDDLE_MASK_SCRIPT)
                .paramSchema("{\"properties\":{\"action\":{\"const\":\"MASK\"},\"targetField\":{\"type\":\"string\"}}}")
                .status("ENABLED")
                .build()
        ));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    @Test
    void shouldPersistFieldPolicyTemplateWhenActionAndTargetFieldContractAreValid() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        MetaAppService metaAppService = new MetaAppService(metaRepository, new InMemoryPermissionRepository());

        StandardPolicyTemplateDefinition saved = metaAppService.createStandardPolicyTemplate(
            StandardPolicyTemplateDefinition.builder()
                .tenantId("T001")
                .templateCode("FIELD_MASK_MOBILE")
                .templateName("手机号脱敏")
                .polType("FIELD")
                .paramSchema("{\"properties\":{\"action\":{\"const\":\"MASK\"},\"targetField\":{\"type\":\"string\"}}}")
                .status("ENABLED")
                .build()
        );

        Assertions.assertEquals("FIELD", saved.getPolType());
        Assertions.assertEquals("FIELD_MASK_MOBILE", saved.getTemplateCode());
    }

    private static final class InMemoryMetaRepository implements MetaRepository {

        private final Map<String, BoMetaModelDefinition> boMetaModels = new LinkedHashMap<>();

        private final List<StandardActionDefinition> standardActions = new ArrayList<>();

        private final Map<String, StandardPolicyTemplateDefinition> policyTemplates = new LinkedHashMap<>();

        private long sequence = 1L;

        private InMemoryMetaRepository() {
            standardActions.add(StandardActionDefinition.builder()
                .tenantId("__GLOBAL__")
                .actCode("READ")
                .actName("查看")
                .actType("STANDARD")
                .resCategory("ALL")
                .riskLevel(1)
                .build());
            standardActions.add(StandardActionDefinition.builder()
                .tenantId("T001")
                .actCode("EXPORT")
                .actName("导出")
                .actType("BIZ")
                .resCategory("API")
                .riskLevel(2)
                .build());
        }

        @Override
        public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
            return definition;
        }

        @Override
        public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
            if (definition.getId() == null) {
                definition.setId(sequence++);
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
        public StandardPolicyTemplateDefinition saveStandardPolicyTemplate(StandardPolicyTemplateDefinition definition) {
            policyTemplates.put(definition.getTenantId() + ":" + definition.getTemplateCode(), definition);
            return definition;
        }

        @Override
        public StandardPolicyTemplateDefinition findStandardPolicyTemplate(String tenantId, String templateCode) {
            return policyTemplates.get(tenantId + ":" + templateCode);
        }
    }

    private static final class InMemoryPermissionRepository implements PermissionRepository {

        private final Map<String, AuthPermissionItem> savedItems = new LinkedHashMap<>();

        private final Set<String> referencedPermCodes = new LinkedHashSet<>();

        private final List<String> deletedPermCodes = new ArrayList<>();

        @Override
        public AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem) {
            savedItems.put(permissionItem.getPermCode(), permissionItem);
            return permissionItem;
        }

        @Override
        public PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return PageResult.<AuthPermissionItem>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(savedItems.size())
                .records(new ArrayList<>(savedItems.values()))
                .build();
        }

        @Override
        public AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode) {
            return savedItems.get(permCode);
        }

        @Override
        public void deletePermissionItem(String tenantId, String appCode, String permCode) {
            savedItems.remove(permCode);
            deletedPermCodes.add(permCode);
        }

        @Override
        public boolean hasPermissionItemReference(String tenantId, String appCode, String permCode) {
            return referencedPermCodes.contains(permCode);
        }
    }

    // ──────────────────────────────────────────────
    // T020：previewBoSchema 三种模式单元测试
    // ──────────────────────────────────────────────

    /**
     * T020-1：Shadow 模式 - 通过 JDBC 采集列元数据。
     * 期望：previewBoSchema 返回采集器提供的列信息，字段正确。
     */
    @Test
    void shouldReturnColumnsFromShadowModePreview() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();

        List<BoSchemaColumnInfo> stubColumns = Arrays.asList(
            BoSchemaColumnInfo.builder()
                .tableName("biz_contract").columnName("id").columnType("BIGINT")
                .isPrimaryKey(true).nullable(false).comment("主键").build(),
            BoSchemaColumnInfo.builder()
                .tableName("biz_contract").columnName("dept_id").columnType("VARCHAR(64)")
                .isPrimaryKey(false).nullable(true).comment("部门").build()
        );
        NativeBoSchemaCollector shadowCollector = tableName -> {
            Assertions.assertEquals("biz_contract", tableName);
            return stubColumns;
        };

        MetaAppService service = new MetaAppService(metaRepository, BoResolverRouter.noop(), shadowCollector);
        List<BoSchemaColumnInfo> result = service.previewBoSchema("T001", "CRM", "CONTRACT", "biz_contract", "SHADOW");

        Assertions.assertEquals(2, result.size(), "Shadow 模式应返回 JDBC 采集的 2 列");
        BoSchemaColumnInfo col0 = result.get(0);
        Assertions.assertEquals("id", col0.getColumnName());
        Assertions.assertTrue(col0.isPrimaryKey());
        Assertions.assertEquals("BIGINT", col0.getColumnType());
        BoSchemaColumnInfo col1 = result.get(1);
        Assertions.assertEquals("dept_id", col1.getColumnName());
        Assertions.assertFalse(col1.isPrimaryKey());
    }

    /**
     * T020-2：Shadow 模式 - 未注入采集器，应优雅降级为空列表。
     * 期望：不抛异常，返回空列表。
     */
    @Test
    void shouldReturnEmptyWhenShadowCollectorMissing() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();

        MetaAppService service = new MetaAppService(metaRepository, BoResolverRouter.noop(), null);
        List<BoSchemaColumnInfo> result = service.previewBoSchema("T001", "CRM", "CONTRACT", "biz_contract", "SHADOW");

        Assertions.assertNotNull(result, "返回值不能为 null");
        Assertions.assertTrue(result.isEmpty(), "未注入采集器时应降级为空列表");
    }

    /**
     * T020-3：Shadow 模式 - 表名为空，应返回空列表。
     * 期望：不触发采集，直接返回空列表。
     */
    @Test
    void shouldReturnEmptyWhenShadowTableNameBlank() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        NativeBoSchemaCollector shadowCollector = tableName -> {
            return Assertions.fail("表名为空时不应触发采集");
        };

        MetaAppService service = new MetaAppService(metaRepository, BoResolverRouter.noop(), shadowCollector);
        List<BoSchemaColumnInfo> result = service.previewBoSchema("T001", "CRM", "CONTRACT", " ", "SHADOW");

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(), "表名为空时应返回空列表");
    }

    /**
     * T020-4：Native 模式 - 采集器正常返回列元数据。
     * 期望：返回采集器提供的列信息。
     */
    @Test
    void shouldReturnColumnsFromNativeModePreview() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        List<BoSchemaColumnInfo> nativeColumns = Arrays.asList(
            BoSchemaColumnInfo.builder()
                .tableName("biz_salary").columnName("id").columnType("BIGINT")
                .isPrimaryKey(true).nullable(false).comment("主键").build()
        );
        NativeBoSchemaCollector nativeCollector = tableName -> nativeColumns;

        MetaAppService service = new MetaAppService(metaRepository, BoResolverRouter.noop(), nativeCollector);
        List<BoSchemaColumnInfo> result = service.previewBoSchema("T001", "HR", "SALARY", "biz_salary", "NATIVE");

        Assertions.assertEquals(1, result.size(), "Native 模式应返回采集器提供的列");
        Assertions.assertEquals("id", result.get(0).getColumnName());
    }

    /**
     * T020-5：手工模式 - 不应触发任何采集，直接返回空列表。
     * 期望：返回空列表，不抛异常。
     */
    @Test
    void shouldReturnEmptyForManualMode() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        MetaAppService service = new MetaAppService(metaRepository);
        List<BoSchemaColumnInfo> result = service.previewBoSchema("T001", "CRM", "CONTRACT", "biz_contract", "MANUAL");

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(), "手工模式应直接返回空列表");
    }

    /**
     * T020-6：Shadow 模式 - BO 未注册时仍可直接采集。
     * 期望：预览采集不依赖已登记的 resolver/adapter 配置。
     */
    @Test
    void shouldCollectShadowPreviewWithoutRegisteredBo() {
        InMemoryMetaRepository metaRepository = new InMemoryMetaRepository();
        NativeBoSchemaCollector shadowCollector = tableName -> Collections.singletonList(
            BoSchemaColumnInfo.builder()
                .tableName(tableName).columnName("id").columnType("BIGINT")
                .isPrimaryKey(true).nullable(false).comment("主键").build()
        );
        MetaAppService service = new MetaAppService(metaRepository, BoResolverRouter.noop(), shadowCollector);

        List<BoSchemaColumnInfo> result = service.previewBoSchema("T001", "CRM", "UNKNOWN_BO", "some_table", "SHADOW");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size(), "Shadow 预览不应依赖已登记 BO");
        Assertions.assertEquals("id", result.get(0).getColumnName());
    }
}