package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.repository.AssignmentRepository;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.shared.exception.BusinessException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AssignmentAppServiceTest {

    private static final String FIELD_SCHEMA_JSON = "{"
        + "\"entities\":[{"
        + "\"code\":\"customer_main\","
        + "\"isPrimary\":true,"
        + "\"tableName\":\"biz_customer\","
        + "\"attributes\":["
        + "{\"code\":\"id\",\"fieldName\":\"id\",\"columnName\":\"id\",\"type\":\"LONG\",\"isPk\":true},"
        + "{\"code\":\"mobile\",\"fieldName\":\"mobile\",\"columnName\":\"mobile\",\"type\":\"STRING\",\"fieldControl\":true},"
        + "{\"code\":\"salary\",\"fieldName\":\"salary\",\"columnName\":\"salary\",\"type\":\"DECIMAL\",\"fieldControl\":true},"
        + "{\"code\":\"nickname\",\"fieldName\":\"nickname\",\"columnName\":\"nickname\",\"type\":\"STRING\",\"fieldControl\":false}"
        + "]}]}";

    @Test
    void shouldRequireRecreateWhenSubjectChanges() {
        AssignmentAppService appService = new AssignmentAppService(
            new AssignmentRepository() {
                @Override
                public SysAuthAssignment saveAssignment(SysAuthAssignment assignment) {
                    return assignment;
                }

                @Override
                public PageResult<SysAuthAssignment> pageAssignments(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
                    return PageResult.<SysAuthAssignment>builder().pageNo(pageNo).pageSize(pageSize).total(1).records(java.util.Collections.emptyList()).build();
                }

                @Override
                public SysAuthAssignment findAssignment(String tenantId, String appCode, Long assignmentId) {
                    return SysAuthAssignment.builder()
                        .id(assignmentId)
                        .tenantId(tenantId)
                        .appCode(appCode)
                        .subjectModel("SUB_USER")
                        .subjectId("U100")
                        .permItemId(1L)
                        .build();
                }

                @Override
                public void deleteAssignment(String tenantId, String appCode, Long assignmentId) {
                }

                @Override
                public boolean hasAssignmentReference(String tenantId, String appCode, Long assignmentId) {
                    return false;
                }
            },
            new MetaRepository() {
                @Override
                public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition d) { return d; }
                @Override
                public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition d) { return d; }
            }
        );

        SysAuthAssignment updatePayload = SysAuthAssignment.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subjectModel("SUB_USER")
            .subjectId("U200")
            .permItemId(1L)
            .build();

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> appService.updateAssignment("T001", "CRM", 1L, "PERM-CONTRACT-READ", updatePayload));
        Assertions.assertEquals("AUTHZ-409-RELATION", exception.getCode());
    }

    @Test
    void shouldRejectFieldAssignmentWhenTargetFieldMissing() {
        AssignmentAppService appService = buildFieldAssignmentAppService(FIELD_SCHEMA_JSON, "MASK");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> appService.createAssignment("FIELD_MASK_MOBILE", buildAssignment("{}")));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    @Test
    void shouldRejectFieldAssignmentWhenTargetFieldNotControlled() {
        AssignmentAppService appService = buildFieldAssignmentAppService(FIELD_SCHEMA_JSON, "MASK");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> appService.createAssignment("FIELD_MASK_MOBILE", buildAssignment("{\"targetField\":\"nickname\"}")));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    @Test
    void shouldRejectFieldAssignmentWhenTargetFieldIsPrimaryKey() {
        AssignmentAppService appService = buildFieldAssignmentAppService(FIELD_SCHEMA_JSON, "HIDE");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> appService.createAssignment("FIELD_HIDE_ID", buildAssignment("{\"targetField\":\"id\"}")));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    @Test
    void shouldRejectMaskFieldAssignmentWhenTargetFieldIsNotString() {
        AssignmentAppService appService = buildFieldAssignmentAppService(FIELD_SCHEMA_JSON, "MASK");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> appService.createAssignment("FIELD_MASK_SALARY", buildAssignment("{\"targetField\":\"salary\"}")));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    @Test
    void shouldAllowFieldAssignmentWhenTargetFieldIsValid() {
        AssignmentAppService appService = buildFieldAssignmentAppService(FIELD_SCHEMA_JSON, "MASK");

        SysAuthAssignment saved = appService.createAssignment("FIELD_MASK_MOBILE", buildAssignment("{\"targetField\":\"mobile\"}"));

        Assertions.assertEquals(Long.valueOf(100L), saved.getPolicyTplId());
        Assertions.assertEquals("{\"targetField\":\"mobile\"}", saved.getPolicyParams());
    }

    @Test
    void shouldRejectFieldAssignmentWhenMaskScriptUsesLegacyBrokenScript() {
        AssignmentAppService appService = buildFieldAssignmentAppService(FIELD_SCHEMA_JSON, "MASK");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> appService.createAssignment(
                "FIELD_MASK_MOBILE",
                buildAssignment("{\"targetField\":\"mobile\",\"maskScript\":\""
                    + FieldMaskScriptRules.LEGACY_BROKEN_MIDDLE_MASK_SCRIPT.replace("\"", "\\\"")
                    + "\"}")
            ));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    @Test
    void shouldResolveFieldAssignmentBoByResIdBeforePermCode() {
        AuthPermissionItem permissionItem = AuthPermissionItem.builder()
            .id(1L)
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:bo:cacl:READ")
            .resModelCode("RES_DATA_BO")
            .resId("900")
            .actCode("READ")
            .build();
        AssignmentAppService appService = buildFieldAssignmentAppService(FIELD_SCHEMA_JSON, "MASK", permissionItem, 900L, "salary_calc_log");

        SysAuthAssignment saved = appService.createAssignment("FIELD_MASK_MOBILE", buildAssignment("{\"targetField\":\"mobile\"}"));

        Assertions.assertEquals(Long.valueOf(100L), saved.getPolicyTplId());
    }

    private AssignmentAppService buildFieldAssignmentAppService(String schemaJson, String action) {
        return buildFieldAssignmentAppService(
            schemaJson,
            action,
            AuthPermissionItem.builder()
                .id(1L)
                .tenantId("T001")
                .appCode("CRM")
                .permCode("CRM:bo:CUSTOMER:READ")
                .resModelCode("RES_DATA_BO")
                .resId("900")
                .actCode("READ")
                .build(),
            900L,
            "CUSTOMER"
        );
    }

    private AssignmentAppService buildFieldAssignmentAppService(
        String schemaJson,
        String action,
        AuthPermissionItem permissionItem,
        Long expectedBoId,
        String expectedBoCode
    ) {
        AssignmentRepository assignmentRepository = new AssignmentRepository() {
            @Override
            public SysAuthAssignment saveAssignment(SysAuthAssignment assignment) {
                return assignment;
            }

            @Override
            public PageResult<SysAuthAssignment> pageAssignments(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
                return PageResult.<SysAuthAssignment>builder().pageNo(pageNo).pageSize(pageSize).total(0).records(Collections.emptyList()).build();
            }

            @Override
            public SysAuthAssignment findAssignment(String tenantId, String appCode, Long assignmentId) {
                return null;
            }

            @Override
            public void deleteAssignment(String tenantId, String appCode, Long assignmentId) {
            }

            @Override
            public boolean hasAssignmentReference(String tenantId, String appCode, Long assignmentId) {
                return false;
            }
        };
        MetaRepository metaRepository = new MetaRepository() {
            @Override
            public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition d) { return d; }
            @Override
            public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition d) { return d; }

            @Override
            public StandardPolicyTemplateDefinition findStandardPolicyTemplate(String tenantId, String templateCode) {
                return StandardPolicyTemplateDefinition.builder()
                    .id(100L)
                    .tenantId(tenantId)
                    .templateCode(templateCode)
                    .templateName(templateCode)
                    .polType("FIELD")
                    .paramSchema("{\"properties\":{\"action\":{\"const\":\"" + action + "\"},\"targetField\":{\"type\":\"string\"}}}")
                    .status("ENABLED")
                    .build();
            }

            @Override
            public BoMetaModelDefinition findBoMetaModel(String tenantId, String appCode, String boCode) {
                if (!expectedBoCode.equalsIgnoreCase(boCode)) {
                    return null;
                }
                return BoMetaModelDefinition.builder()
                    .id(expectedBoId)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .boCode(expectedBoCode)
                    .schemaJson(schemaJson)
                    .build();
            }

            @Override
            public BoMetaModelDefinition findBoMetaModelById(String tenantId, String appCode, Long targetBoId) {
                if (!expectedBoId.equals(targetBoId)) {
                    return null;
                }
                return BoMetaModelDefinition.builder()
                    .id(expectedBoId)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .boCode(expectedBoCode)
                    .schemaJson(schemaJson)
                    .build();
            }
        };
        PermissionRepository permissionRepository = new PermissionRepository() {
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
                return Collections.singletonList(permissionItem);
            }
        };
        return new AssignmentAppService(assignmentRepository, metaRepository, permissionRepository);
    }

    private SysAuthAssignment buildAssignment(String policyParams) {
        return SysAuthAssignment.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subjectModel("SUB_USER")
            .subjectId("U100")
            .permItemId(1L)
            .policyParams(policyParams)
            .build();
    }
}
