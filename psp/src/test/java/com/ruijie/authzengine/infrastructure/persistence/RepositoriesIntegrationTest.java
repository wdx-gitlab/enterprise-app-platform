package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.permission.ResourceDerivationPermission;
import com.ruijie.authzengine.domain.model.governance.subject.AuthRole;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SysOrgNode;
import com.ruijie.authzengine.domain.model.governance.subject.SysPosition;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.resource.SysResMenu;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import com.ruijie.authzengine.domain.model.common.ResourceModelCode;
import com.ruijie.authzengine.application.service.DerivationPermissionAppService;
import com.ruijie.authzengine.domain.repository.AssignmentRepository;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserGroup;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.domain.repository.SubjectRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 治理仓储集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RepositoriesIntegrationTest {

    @Autowired
    private MetaRepository metaRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private DerivationPermissionRepository derivationPermissionRepository;

    @Autowired
    private DerivationPermissionAppService derivationPermissionAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void shouldPersistMetaModelsThroughDatabaseRepository() {
        AuthMetaModelDefinition authMetaModel = metaRepository.saveAuthMetaModel(AuthMetaModelDefinition.builder()
            .tenantId("T001")
            .appCode("CRM")
            .modelCode("RES_API_TEST")
            .modelName("接口资源测试")
            .category("RESOURCE")
            .adapterType("JAVA_BEAN")
            .resolver("noopHook")
            .schemaView("{}")
            .build());
        BoMetaModelDefinition boMetaModel = metaRepository.saveBoMetaModel(BoMetaModelDefinition.builder()
            .tenantId("T001")
            .appCode("CRM")
            .boCode("INVOICE")
            .boName("发票")
            .schemaJson("{}")
            .adapterType("JAVA_BEAN")
            .resolver("invoiceHook")
            .build());

        Assertions.assertEquals("RES_API_TEST", authMetaModel.getModelCode());
        Assertions.assertEquals("INVOICE", boMetaModel.getBoCode());
        Assertions.assertFalse(metaRepository.listStandardActions("T001").isEmpty());
        Assertions.assertFalse(metaRepository.listStandardPolicyTemplates("T001").isEmpty());
    }

    @Test
    void shouldPersistDirectoryEntriesThroughDatabaseRepository() {
        jdbcTemplate.update(
            "MERGE INTO dap_sys_org (id, tenant_id, app_code, department_code, department_name, parent_org_id, org_path, status, created_by, updated_by, is_deleted) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95001L, "T001", "CRM", "ORG-FIN", "财务组织", null, "/ORG-FIN", "ENABLED", "test", "test", 0
        );
        SysUserAccount savedUser = subjectRepository.saveUser(SysUserAccount.builder()
            .tenantId("T001")
            .appCode("CRM")
            .staffNo("U200")
            .userId("lisi")
            .staffName("李四")
            .orgCode("ORG-FIN")
            .status("ENABLED")
            .build());
        SysResApi savedApi = resourceRepository.saveApi(SysResApi.builder()
            .tenantId("T001")
            .appCode("CRM")
            .apiCode("API-INVOICE-QUERY")
            .apiName("发票查询接口")
            .httpMethod("GET")
            .uriPattern("/api/invoices")
            .status("ENABLED")
            .build());

        List<SysUserAccount> users = subjectRepository.listUsers("T001", "CRM");
        List<SysResApi> apis = resourceRepository.listApis("T001", "CRM");

        Assertions.assertEquals("U200", savedUser.getStaffNo());
        Assertions.assertEquals("ORG-FIN", users.stream()
            .filter(item -> "U200".equals(item.getStaffNo()))
            .findFirst()
            .map(SysUserAccount::getOrgCode)
            .orElse(null));
        Assertions.assertEquals("API-INVOICE-QUERY", savedApi.getApiCode());
        Assertions.assertTrue(users.stream().anyMatch(item -> "U200".equals(item.getStaffNo())));
        Assertions.assertTrue(apis.stream().anyMatch(item -> "API-INVOICE-QUERY".equals(item.getApiCode())));
    }

    @Test
    void shouldNormalizeLegacyRoleRelationTypeOnSaveAndRead() {
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95100L, "T001", "CRM", "SUB_USER", "U-REL-LEGACY", "SUB_ROLE", "viewer", "HAS_ROLE", "test", "test", 0
        );

        AuthSubjectRelation saved = subjectRepository.saveSubjectRelation(AuthSubjectRelation.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subjectModel("SUB_USER")
            .subjectId("U-REL-LEGACY")
            .relatedSubjectModel("SUB_ROLE")
            .relatedSubjectId("viewer")
            .relationType("ROLE")
            .build());

        Integer relationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_subject_relation WHERE tenant_id = ? AND app_code = ? AND subject_model = ? AND subject_id = ? AND related_subject_model = ? AND related_subject_id = ?",
            Integer.class,
            "T001", "CRM", "SUB_USER", "U-REL-LEGACY", "SUB_ROLE", "viewer"
        );
        String storedRelationType = jdbcTemplate.queryForObject(
            "SELECT relation_type FROM authz_subject_relation WHERE id = ?",
            String.class,
            95100L
        );
        AuthSubjectRelation reloaded = subjectRepository.findSubjectRelation("T001", "CRM", 95100L);

        Assertions.assertEquals(Long.valueOf(95100L), saved.getId());
        Assertions.assertEquals(1, relationCount);
        Assertions.assertEquals("ROLE", storedRelationType);
        Assertions.assertEquals("ROLE", reloaded.getRelationType());
    }

    @Test
    void shouldFindUserRelationsByGovernanceSubjectIdWhenRequestUsesUserId() {
        String appCode = "CRM-REL-ID";
        SysUserAccount savedUser = subjectRepository.saveUser(SysUserAccount.builder()
            .tenantId("T001")
            .appCode(appCode)
            .staffNo("U-REL-ID")
            .userId("user-rel-login")
            .staffName("关系用户")
            .status("ENABLED")
            .build());
        AuthRole savedRole = subjectRepository.saveRole(AuthRole.builder()
            .tenantId("T001")
            .appCode(appCode)
            .roleCode("ROLE-REL-ID")
            .roleName("关系角色")
            .roleScope("APP")
            .status("ENABLED")
            .build());
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95110L,
            "T001",
            appCode,
            "SUB_USER",
            String.valueOf(savedUser.getId()),
            "SUB_ROLE",
            String.valueOf(savedRole.getId()),
            "ROLE",
            "test",
            "test",
            0
        );

        List<AuthSubjectRelation> relations = subjectRepository.findRelationsByUserId("T001", appCode, "user-rel-login");

        Assertions.assertEquals(1, relations.size());
        Assertions.assertEquals(String.valueOf(savedUser.getId()), relations.get(0).getSubjectId());
        Assertions.assertEquals(String.valueOf(savedRole.getId()), relations.get(0).getRelatedSubjectId());
    }

    @Test
    void shouldDetectSubjectReferencesByGovernanceSubjectId() {
        String appCode = "CRM-REF-ID";
        SysOrgNode savedOrg = subjectRepository.saveOrg(SysOrgNode.builder()
            .tenantId("T001")
            .appCode(appCode)
            .departmentCode("ORG-REF-ID")
            .departmentName("引用组织")
            .status("ENABLED")
            .build());
        SysPosition savedPosition = subjectRepository.savePosition(SysPosition.builder()
            .tenantId("T001")
            .appCode(appCode)
            .positionCode("POS-REF-ID")
            .positionName("引用岗位")
            .status("ENABLED")
            .build());
        SysUserGroup savedGroup = subjectRepository.saveUserGroup(SysUserGroup.builder()
            .tenantId("T001")
            .appCode(appCode)
            .groupCode("GROUP-REF-ID")
            .groupName("引用用户组")
            .status("ENABLED")
            .build());
        AuthRole savedRole = subjectRepository.saveRole(AuthRole.builder()
            .tenantId("T001")
            .appCode(appCode)
            .roleCode("ROLE-REF-ID")
            .roleName("引用角色")
            .roleScope("APP")
            .status("ENABLED")
            .build());
        SysUserAccount savedUser = subjectRepository.saveUser(SysUserAccount.builder()
            .tenantId("T001")
            .appCode(appCode)
            .staffNo("USER-REF-ID")
            .userId("user-ref-login")
            .staffName("引用用户")
            .status("ENABLED")
            .build());
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95120L,
            "T001",
            appCode,
            "SUB_USER",
            String.valueOf(savedUser.getId()),
            "SUB_ROLE",
            String.valueOf(savedRole.getId()),
            "ROLE",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95121L,
            "T001",
            appCode,
            "SUB_ORG",
            String.valueOf(savedOrg.getId()),
            "SUB_POSITION",
            String.valueOf(savedPosition.getId()),
            "POSITION",
            "test",
            "test",
            0
        );
        jdbcTemplate.update(
            "INSERT INTO authz_subject_relation (id, tenant_id, app_code, subject_model, subject_id, related_subject_model, related_subject_id, relation_type, created_by, updated_by, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            95122L,
            "T001",
            appCode,
            "SUB_GROUP",
            String.valueOf(savedGroup.getId()),
            "SUB_ORG",
            String.valueOf(savedOrg.getId()),
            "ORG",
            "test",
            "test",
            0
        );

        Assertions.assertTrue(subjectRepository.hasUserReference("T001", appCode, "USER-REF-ID"));
        Assertions.assertTrue(subjectRepository.hasRoleReference("T001", appCode, "ROLE-REF-ID"));
        Assertions.assertTrue(subjectRepository.hasOrgReference("T001", appCode, "ORG-REF-ID"));
        Assertions.assertTrue(subjectRepository.hasPositionReference("T001", appCode, "POS-REF-ID"));
        Assertions.assertTrue(subjectRepository.hasUserGroupReference("T001", appCode, "GROUP-REF-ID"));
    }

    @Test
    void shouldPersistMetaModelFromContractMinimumPayload() throws Exception {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("modelCode", "SUB_POSITION_TEST");
        payload.put("category", "SUBJECT");
        payload.put("resolver", "positionHook");

        mockMvc.perform(post("/authz-engine/api/v1/governance/meta-models/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.businessId").value("SUB_POSITION_TEST"));

        String modelName = jdbcTemplate.queryForObject(
            "SELECT model_name FROM authz_meta_model WHERE tenant_id = ? AND app_code = ? AND model_code = ?",
            String.class,
            "T001", "CRM", "SUB_POSITION_TEST"
        );

        Assertions.assertEquals("SUB_POSITION_TEST", modelName);
    }

    @Test
    void shouldPersistApiFromContractMinimumPayload() throws Exception {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("apiCode", "API-CONTRACT-MINIMAL");
        payload.put("httpMethod", "POST");
        payload.put("uriPattern", "/api/contracts/minimal");

        mockMvc.perform(post("/authz-engine/api/v1/governance/resources/apis")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.businessId").value("API-CONTRACT-MINIMAL"));

        String apiName = jdbcTemplate.queryForObject(
            "SELECT api_name FROM usp_api WHERE tenant_id = ? AND app_code = ? AND api_code = ?",
            String.class,
            "T001", "CRM", "API-CONTRACT-MINIMAL"
        );

        Assertions.assertEquals("API-CONTRACT-MINIMAL", apiName);
    }

    @Test
    void shouldClearExpireTimeWhenUpdatingAssignment() {
        AuthPermissionItem permissionItem = permissionRepository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:bo:ASSIGNMENT:EXPIRE:CLEAR")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("READ")
            .build());
        LocalDateTime expireTime = LocalDateTime.of(2030, 1, 1, 12, 0, 0);

        SysAuthAssignment saved = assignmentRepository.saveAssignment(SysAuthAssignment.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subjectModel("SUB_USER")
            .subjectId("U-EXPIRE-CLEAR")
            .permItemId(permissionItem.getId())
            .expireTime(expireTime)
            .build());

        assignmentRepository.saveAssignment(SysAuthAssignment.builder()
            .id(saved.getId())
            .tenantId("T001")
            .appCode("CRM")
            .subjectModel("SUB_USER")
            .subjectId("U-EXPIRE-CLEAR")
            .permItemId(permissionItem.getId())
            .expireTime(null)
            .build());

        SysAuthAssignment reloaded = assignmentRepository.findAssignment("T001", "CRM", saved.getId());
        Assertions.assertNotNull(reloaded);
        Assertions.assertNull(reloaded.getExpireTime());
    }

    @Test
    void shouldPagePermissionItemsByRequestedWindow() {
        String appCode = "CRM-PAGE-PERM";
        permissionRepository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode(appCode)
            .permCode("CRM-PAGE-PERM:bo:CUSTOMER:READ")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("READ")
            .build());
        permissionRepository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode(appCode)
            .permCode("CRM-PAGE-PERM:bo:CUSTOMER:EXPORT")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("EXPORT")
            .build());

        PageResult<AuthPermissionItem> firstPage = permissionRepository.pagePermissionItems("T001", appCode, null, 1, 1);
        PageResult<AuthPermissionItem> secondPage = permissionRepository.pagePermissionItems("T001", appCode, null, 2, 1);

        Assertions.assertEquals(2, firstPage.getTotal());
        Assertions.assertEquals(1, firstPage.getRecords().size());
        Assertions.assertEquals(1, secondPage.getRecords().size());
        Assertions.assertNotEquals(
            firstPage.getRecords().get(0).getPermCode(),
            secondPage.getRecords().get(0).getPermCode());
    }

    @Test
    void shouldPageAssignmentsByRequestedWindow() {
        String appCode = "CRM-PAGE-ASG";
        AuthPermissionItem permissionItem = permissionRepository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode(appCode)
            .permCode("CRM-PAGE-ASG:bo:CUSTOMER:READ")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("READ")
            .build());
        assignmentRepository.saveAssignment(SysAuthAssignment.builder()
            .tenantId("T001")
            .appCode(appCode)
            .subjectModel("SUB_USER")
            .subjectId("U-PAGE-001")
            .permItemId(permissionItem.getId())
            .build());
        assignmentRepository.saveAssignment(SysAuthAssignment.builder()
            .tenantId("T001")
            .appCode(appCode)
            .subjectModel("SUB_USER")
            .subjectId("U-PAGE-002")
            .permItemId(permissionItem.getId())
            .build());

        PageResult<SysAuthAssignment> firstPage = assignmentRepository.pageAssignments("T001", appCode, null, 1, 1);
        PageResult<SysAuthAssignment> secondPage = assignmentRepository.pageAssignments("T001", appCode, null, 2, 1);

        Assertions.assertEquals(2, firstPage.getTotal());
        Assertions.assertEquals(1, firstPage.getRecords().size());
        Assertions.assertEquals(1, secondPage.getRecords().size());
        Assertions.assertNotEquals(
            firstPage.getRecords().get(0).getSubjectId(),
            secondPage.getRecords().get(0).getSubjectId());
    }

    @Test
    void shouldQueryAndCleanupPageDerivationBindings() {
        resourceRepository.saveMenu(SysResMenu.builder()
            .tenantId("T001")
            .appCode("CRM")
            .menuCode("MENU-CONTRACT")
            .menuName("合同管理")
            .routePath("/contracts")
            .status("ENABLED")
            .build());
        SysResPage page = resourceRepository.savePage(SysResPage.builder()
            .tenantId("T001")
            .appCode("CRM")
            .pageCode("PAGE-CONTRACT-LIST")
            .pageName("合同列表")
            .menuCode("MENU-CONTRACT")
            .pagePath("/contracts/list")
            .status("ENABLED")
            .build());
        AuthPermissionItem permissionItem = permissionRepository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:bo:CONTRACT:READ")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("READ")
            .build());

        ResourceDerivationPermission saved = derivationPermissionAppService.saveBinding(ResourceDerivationPermission.builder()
            .tenantId("T001")
            .appCode("CRM")
            .resType(ResourceModelCode.RES_UI_PAGE.name())
            .resId(page.getId())
            .permItemId(permissionItem.getId())
            .build());

        Assertions.assertNotNull(saved.getId());
        Assertions.assertEquals(
            java.util.Collections.singletonList("PAGE-CONTRACT-LIST"),
            derivationPermissionRepository.findDerivedResourceCodesByPermItemIds(
                "T001",
                "CRM",
                ResourceModelCode.RES_UI_PAGE.name(),
                java.util.Collections.singletonList(permissionItem.getId())
            )
        );

        resourceRepository.deletePage("T001", "CRM", "PAGE-CONTRACT-LIST");

        Assertions.assertFalse(
            derivationPermissionRepository.hasDerivationBindings("T001", "CRM", ResourceModelCode.RES_UI_PAGE.name()));
    }

    @Test
    void shouldCleanupApiDerivationBindingsWhenPermissionItemDeleted() {
        SysResApi api = resourceRepository.saveApi(SysResApi.builder()
            .tenantId("T001")
            .appCode("CRM")
            .apiCode("API-CONTRACT-DERIVED")
            .apiName("合同派生接口")
            .httpMethod("POST")
            .uriPattern("/api/contracts/derived")
            .status("ENABLED")
            .build());
        AuthPermissionItem permissionItem = permissionRepository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:bo:CONTRACT:EXPORT")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("EXPORT")
            .build());

        derivationPermissionAppService.saveBinding(ResourceDerivationPermission.builder()
            .tenantId("T001")
            .appCode("CRM")
            .resType(ResourceModelCode.RES_API.name())
            .resId(api.getId())
            .permItemId(permissionItem.getId())
            .build());

        Assertions.assertTrue(
            derivationPermissionRepository.hasDerivationBindings("T001", "CRM", ResourceModelCode.RES_API.name()));

        permissionRepository.deletePermissionItem("T001", "CRM", "CRM:bo:CONTRACT:EXPORT");

        Assertions.assertTrue(derivationPermissionRepository.listBindingsByResource(
            "T001",
            "CRM",
            ResourceModelCode.RES_API.name(),
            api.getId()
        ).isEmpty());
    }

    @Test
    void shouldReviveDeletedPermissionItemWhenSavingSameBusinessKeyAgain() {
        AuthPermissionItem saved = permissionRepository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode("RESTORE-PI")
            .permCode("RESTORE-PI:bo:RESTORE-BO:READ")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("RESTORE-BO")
            .actCode("READ")
            .build());

        permissionRepository.deletePermissionItem("T001", "RESTORE-PI", "RESTORE-PI:bo:RESTORE-BO:READ");

        AuthPermissionItem restored = permissionRepository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode("RESTORE-PI")
            .permCode("RESTORE-PI:bo:RESTORE-BO:READ")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("RESTORE-BO")
            .actCode("READ")
            .build());

        Assertions.assertEquals(saved.getId(), restored.getId());
        Integer activeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_permission_item WHERE tenant_id = ? AND app_code = ? AND res_model_code = ? AND res_id = ? AND act_code = ? AND is_deleted = 0",
            Integer.class,
            "T001", "RESTORE-PI", ResourceModelCode.RES_DATA_BO.name(), "RESTORE-BO", "READ"
        );
        Integer deletedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_permission_item WHERE tenant_id = ? AND app_code = ? AND res_model_code = ? AND res_id = ? AND act_code = ? AND is_deleted = 1",
            Integer.class,
            "T001", "RESTORE-PI", ResourceModelCode.RES_DATA_BO.name(), "RESTORE-BO", "READ"
        );
        Assertions.assertEquals(Integer.valueOf(1), activeCount);
        Assertions.assertEquals(Integer.valueOf(0), deletedCount);
    }

    @Test
    void shouldCreateNewDerivationBindingWhenSavingSameBusinessKeyAgainAfterPhysicalDelete() {
        SysResApi api = resourceRepository.saveApi(SysResApi.builder()
            .tenantId("T001")
            .appCode("RESTORE-DP")
            .apiCode("API-RESTORE-DERIVED")
            .apiName("派生恢复接口")
            .httpMethod("POST")
            .uriPattern("/api/restore/derived")
            .status("ENABLED")
            .build());
        AuthPermissionItem permissionItem = permissionRepository.savePermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode("RESTORE-DP")
            .permCode("RESTORE-DP:bo:RESTORE-DP-BO:READ")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("RESTORE-DP-BO")
            .actCode("READ")
            .build());

        ResourceDerivationPermission saved = derivationPermissionAppService.saveBinding(ResourceDerivationPermission.builder()
            .tenantId("T001")
            .appCode("RESTORE-DP")
            .resType(ResourceModelCode.RES_API.name())
            .resId(api.getId())
            .permItemId(permissionItem.getId())
            .build());

        derivationPermissionAppService.deleteBinding("T001", "RESTORE-DP", saved.getId());

        ResourceDerivationPermission restored = derivationPermissionAppService.saveBinding(ResourceDerivationPermission.builder()
            .tenantId("T001")
            .appCode("RESTORE-DP")
            .resType(ResourceModelCode.RES_API.name())
            .resId(api.getId())
            .permItemId(permissionItem.getId())
            .build());

        Assertions.assertNotEquals(saved.getId(), restored.getId());
        Integer activeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_res_derivation_perm WHERE tenant_id = ? AND app_code = ? AND res_type = ? AND res_id = ? AND perm_item_id = ? AND is_deleted = 0",
            Integer.class,
            "T001", "RESTORE-DP", ResourceModelCode.RES_API.name(), api.getId(), permissionItem.getId()
        );
        Integer deletedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_res_derivation_perm WHERE tenant_id = ? AND app_code = ? AND res_type = ? AND res_id = ? AND perm_item_id = ? AND is_deleted = 1",
            Integer.class,
            "T001", "RESTORE-DP", ResourceModelCode.RES_API.name(), api.getId(), permissionItem.getId()
        );
        Assertions.assertEquals(Integer.valueOf(1), activeCount);
        Assertions.assertEquals(Integer.valueOf(0), deletedCount);
    }

    @Test
    void shouldReturnGlobalAndTenantCatalogItemsWhenTenantScopeProvided() throws Exception {
        jdbcTemplate.update(
            "MERGE INTO authz_std_act_dict (id, tenant_id, act_code, act_name, act_type, res_category, risk_level, created_by, updated_by, is_deleted) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96001L, "T001", "EXPORT_TEST", "导出测试", "BIZ", "API", 2, "test", "test", 0
        );
        jdbcTemplate.update(
            "MERGE INTO authz_std_pol_template (id, tenant_id, template_code, template_name, pol_type, expression_script, param_schema, status, created_by, updated_by, is_deleted) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            96002L, "T001", "DATA_SCOPE_TEST", "数据范围测试", "DATA", "return true;", "{}", "ENABLED", "test", "test", 0
        );

        mockMvc.perform(get("/authz-engine/api/v1/governance/actions").param("tenantId", "T001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records[?(@.actCode=='READ')]").exists())
            .andExpect(jsonPath("$.data.records[?(@.actCode=='EXPORT_TEST')]").exists());

        mockMvc.perform(get("/authz-engine/api/v1/governance/policy-templates").param("tenantId", "T001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records[?(@.templateCode=='DATA_SCOPE_TEST')]").exists());

        mockMvc.perform(get("/authz-engine/api/v1/governance/policy-templates")
                .param("tenantId", "T001")
                .param("polType", "DATA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records[*].polType", everyItem(is("DATA"))))
            .andExpect(jsonPath("$.data.records[?(@.templateCode=='DATA_SCOPE_TEST')]").exists());
    }

    @Test
    void shouldLogicalDeletePolicyTemplate() {
        metaRepository.saveStandardPolicyTemplate(StandardPolicyTemplateDefinition.builder()
            .tenantId("T001")
            .templateCode("DATA_SCOPE_DELETE_TEST")
            .templateName("逻辑删除测试模板")
            .polType("DATA")
            .expressionScript("#tableName + '.dept_id = ' + param(#param['deptId'])")
            .paramSchema("{}")
            .status("ENABLED")
            .build());

        metaRepository.deleteStandardPolicyTemplate("T001", "DATA_SCOPE_DELETE_TEST");

        Integer totalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_std_pol_template WHERE tenant_id = ? AND template_code = ?",
            Integer.class,
            "T001",
            "DATA_SCOPE_DELETE_TEST"
        );
        Integer activeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_std_pol_template WHERE tenant_id = ? AND template_code = ? AND is_deleted = 0",
            Integer.class,
            "T001",
            "DATA_SCOPE_DELETE_TEST"
        );
        Integer deletedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM authz_std_pol_template WHERE tenant_id = ? AND template_code = ? AND is_deleted = 1",
            Integer.class,
            "T001",
            "DATA_SCOPE_DELETE_TEST"
        );

        Assertions.assertEquals(Integer.valueOf(1), totalCount);
        Assertions.assertEquals(Integer.valueOf(0), activeCount);
        Assertions.assertEquals(Integer.valueOf(1), deletedCount);
        Assertions.assertNull(metaRepository.findStandardPolicyTemplate("T001", "DATA_SCOPE_DELETE_TEST"));
    }

    @Test
    void shouldPersistDelegationThroughEndpoints() throws Exception {
        jdbcTemplate.update(
            "MERGE INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            97001L, "T001", "CRM", "PERM-DELEGATION-TEST", "RES_API", "API-DELEGATION-TEST", "APPROVE", "test", "test", 0
        );
        jdbcTemplate.update(
            "MERGE INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, created_by, updated_by, is_deleted) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            97002L, "T001", "CRM", "approver-a", "SUB_USER", 97001L, "test", "test", 0
        );

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("tenantId", "T001");
        payload.put("appCode", "CRM");
        payload.put("grantorSubjectModel", "SUB_USER");
        payload.put("grantorSubjectId", "approver-a");
        payload.put("delegateSubjectModel", "SUB_USER");
        payload.put("delegateSubjectId", "approver-b");
        payload.put("permissionCode", "PERM-DELEGATION-TEST");
        payload.put("startTime", "2026-04-01T09:00:00");
        payload.put("endTime", "2026-04-10T09:00:00");
        payload.put("reason", "集成测试委托");

        MvcResult createResult = mockMvc.perform(post("/authz-engine/api/v1/governance/delegations")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andReturn();

        Long delegationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .path("data")
            .path("delegationId")
            .asLong();

        mockMvc.perform(get("/authz-engine/api/v1/governance/delegations")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("keyword", "approver-b"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.records[0].delegationId").value(delegationId));

        mockMvc.perform(get("/authz-engine/api/v1/governance/delegations/" + delegationId)
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.permissionCode").value("PERM-DELEGATION-TEST"));

        mockMvc.perform(post("/authz-engine/api/v1/governance/delegations/" + delegationId + "/revoke")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.status").value("REVOKED"));
    }

    @Test
    void shouldListGrantablePermissionCodesThroughEndpoint() throws Exception {
        jdbcTemplate.update(
            "MERGE INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            97011L, "T001", "CRM", "PERM-DELEGATION-ACTIVE", "RES_API", "API-DELEGATION-ACTIVE", "APPROVE", "test", "test", 0
        );
        jdbcTemplate.update(
            "MERGE INTO authz_permission_item (id, tenant_id, app_code, perm_code, res_model_code, res_id, act_code, created_by, updated_by, is_deleted) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            97012L, "T001", "CRM", "PERM-DELEGATION-EXPIRED", "RES_API", "API-DELEGATION-EXPIRED", "APPROVE", "test", "test", 0
        );
        jdbcTemplate.update(
            "MERGE INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, expire_time, created_by, updated_by, is_deleted) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            97013L, "T001", "CRM", "approver-c", "SUB_USER", 97011L, java.sql.Timestamp.valueOf("2026-04-05 00:00:00"), "test", "test", 0
        );
        jdbcTemplate.update(
            "MERGE INTO authz_assignment (id, tenant_id, app_code, subject_id, subject_model, perm_item_id, expire_time, created_by, updated_by, is_deleted) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            97014L, "T001", "CRM", "approver-c", "SUB_USER", 97012L, java.sql.Timestamp.valueOf("2026-04-01 00:00:00"), "test", "test", 0
        );

        mockMvc.perform(get("/authz-engine/api/v1/governance/delegations/grantable-permissions")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("grantorSubjectModel", "SUB_USER")
                .param("grantorSubjectId", "approver-c")
                .param("effectiveAt", "2026-04-02T09:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0]").value("PERM-DELEGATION-ACTIVE"));
    }
}