package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.application.service.AuthzQueryAppService.MenuTreeNodeResult;
import com.ruijie.authzengine.application.service.AuthzQueryAppService.UserContextResult;
import com.ruijie.authzengine.application.spi.AuthMetaModelAdapter;
import com.ruijie.authzengine.application.spi.DataItem;
import com.ruijie.authzengine.application.spi.ModelCode;
import com.ruijie.authzengine.application.spi.SubjectHookResult;
import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.resource.SysResComponent;
import com.ruijie.authzengine.domain.model.governance.resource.SysResMenu;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.repository.AssignmentRepository;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.domain.repository.SubjectRepository;
import com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter;
import com.ruijie.authzengine.infrastructure.config.UserContextDerivationProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

class AuthzQueryAppServiceTest {

    @Test
    void shouldBuildMenuTreeWithAccessibleAncestor() {
        UserContextResult result = createService(false).queryUserContext("T001", "CRM", "U100", "SUB_USER");

        Assertions.assertEquals(Collections.singletonList("PERM_MENU_REPORTS"), result.permCodes);
        Assertions.assertEquals(Collections.singletonList("MENU_REPORTS"), result.accessibleResources.get("RES_UI_MENU"));
        Assertions.assertEquals(1, result.menuTree.size());
        MenuTreeNodeResult root = result.menuTree.get(0);
        Assertions.assertEquals("MENU_ROOT", root.menuCode);
        Assertions.assertEquals(1, root.children.size());
        Assertions.assertEquals("MENU_REPORTS", root.children.get(0).menuCode);
        Assertions.assertEquals(Boolean.FALSE, result.visibility.get("BTN_EXPORT"));
    }

    @Test
    void shouldBuildMenuTreeFromShadowBatchResolveItems() {
        UserContextResult result = createService(true).queryUserContext("T001", "CRM", "U100", "SUB_USER");

        Assertions.assertEquals(1, result.menuTree.size());
        Assertions.assertEquals("MENU_ROOT", result.menuTree.get(0).menuCode);
        Assertions.assertEquals("MENU_REPORTS", result.menuTree.get(0).children.get(0).menuCode);
        Assertions.assertEquals("/reports", result.menuTree.get(0).children.get(0).routePath);
    }

    @Test
    void shouldExpandPermissionSnapshotWithEngineRelationsInShadowMenuMode() {
        List<String> permCodes = createService(true).queryPermissionSnapshot("T001", "CRM", "U100", "SUB_USER");

        Assertions.assertEquals(2, permCodes.size());
        Assertions.assertTrue(permCodes.contains("PERM_MENU_REPORTS"));
        Assertions.assertTrue(permCodes.contains("PERM_COMPONENT_EXPORT"));
    }

    @Test
    void shouldPreferDerivedComponentVisibilityWhenBindingsExist() {
        InMemoryDerivationPermissionRepository derivationRepository = new InMemoryDerivationPermissionRepository(
            "RES_UI_COMPONENT",
            true,
            Arrays.asList("BTN_EXPORT", "BTN_AUDIT"),
            Collections.singletonMap(11L, Collections.singletonList("BTN_EXPORT"))
        );

        Map<String, Boolean> visibility = createService(
            false,
            derivationRepository,
            derivationProperties(UserContextDerivationProperties.LoadMode.COMPAT,
                UserContextDerivationProperties.MissingBindingStrategy.DENY)
        ).queryUiVisibility("T001", "CRM", "U100", "SUB_USER", Arrays.asList("BTN_EXPORT", "BTN_AUDIT"));

        Assertions.assertEquals(Boolean.TRUE, visibility.get("BTN_EXPORT"));
        Assertions.assertEquals(Boolean.FALSE, visibility.get("BTN_AUDIT"));
    }

    @Test
    void shouldFallbackToLegacyComponentVisibilityWhenCompatModeHasNoBindings() {
        Map<String, Boolean> visibility = createService(
            false,
            new InMemoryDerivationPermissionRepository(null, false, Collections.emptyList(), Collections.emptyMap()),
            derivationProperties(UserContextDerivationProperties.LoadMode.COMPAT,
                UserContextDerivationProperties.MissingBindingStrategy.DENY)
        ).queryUiVisibility("T001", "CRM", "U100", "SUB_USER", Arrays.asList("BTN_EXPORT", "BTN_UNKNOWN"));

        Assertions.assertEquals(Boolean.FALSE, visibility.get("BTN_EXPORT"));
        Assertions.assertEquals(Boolean.TRUE, visibility.get("BTN_UNKNOWN"));
    }

    @Test
    void shouldApplyAllowStrategyWhenDerivationOnlyHasNoBindings() {
        Map<String, Boolean> visibility = createService(
            false,
            new InMemoryDerivationPermissionRepository(null, false, Collections.emptyList(), Collections.emptyMap()),
            derivationProperties(UserContextDerivationProperties.LoadMode.DERIVATION_ONLY,
                UserContextDerivationProperties.MissingBindingStrategy.ALLOW)
        ).queryUiVisibility("T001", "CRM", "U100", "SUB_USER", Arrays.asList("BTN_EXPORT", "BTN_AUDIT"));

        Assertions.assertEquals(Boolean.TRUE, visibility.get("BTN_EXPORT"));
        Assertions.assertEquals(Boolean.TRUE, visibility.get("BTN_AUDIT"));
    }

    @Test
    void shouldBuildUserContextVisibilityFromDerivedBindings() {
        InMemoryDerivationPermissionRepository derivationRepository = new InMemoryDerivationPermissionRepository(
            "RES_UI_COMPONENT",
            true,
            Arrays.asList("BTN_EXPORT", "BTN_AUDIT"),
            Collections.singletonMap(11L, Collections.singletonList("BTN_EXPORT"))
        );

        UserContextResult result = createService(
            false,
            derivationRepository,
            derivationProperties(UserContextDerivationProperties.LoadMode.COMPAT,
                UserContextDerivationProperties.MissingBindingStrategy.DENY)
        ).queryUserContext("T001", "CRM", "U100", "SUB_USER");

        Assertions.assertEquals(Boolean.TRUE, result.visibility.get("BTN_EXPORT"));
        Assertions.assertEquals(Boolean.FALSE, result.visibility.get("BTN_AUDIT"));
    }

    @Test
    void shouldBuildMenuTreeFromDerivedPagesWhenBindingsExist() {
        InMemoryDerivationPermissionRepository derivationRepository = new InMemoryDerivationPermissionRepository(
            "RES_UI_PAGE",
            true,
            Arrays.asList("PAGE_REPORTS", "PAGE_ADMIN"),
            Collections.singletonMap(11L, Collections.singletonList("PAGE_ADMIN"))
        );

        UserContextResult result = createService(
            false,
            derivationRepository,
            derivationProperties(UserContextDerivationProperties.LoadMode.COMPAT,
                UserContextDerivationProperties.MissingBindingStrategy.DENY)
        ).queryUserContext("T001", "CRM", "U100", "SUB_USER");

        Assertions.assertEquals(Collections.singletonList("MENU_ADMIN"), result.accessibleResources.get("RES_UI_MENU"));
        Assertions.assertEquals(1, result.menuTree.size());
        Assertions.assertEquals("MENU_ROOT", result.menuTree.get(0).menuCode);
        Assertions.assertEquals("MENU_ADMIN", result.menuTree.get(0).children.get(0).menuCode);
    }

    @Test
    private AuthzQueryAppService createService(boolean shadowMode) {
        return createService(
            shadowMode,
            new InMemoryDerivationPermissionRepository(null, false, Collections.emptyList(), Collections.emptyMap()),
            derivationProperties(UserContextDerivationProperties.LoadMode.COMPAT,
                UserContextDerivationProperties.MissingBindingStrategy.DENY)
        );
    }

    private AuthzQueryAppService createService(boolean shadowMode,
                                               DerivationPermissionRepository derivationPermissionRepository,
                                               UserContextDerivationProperties derivationProperties) {
        ResourceRepository resourceRepository = shadowMode
            ? new FailingResourceRepository()
            : new InMemoryResourceRepository();
        AuthMetaResolverRouter router = shadowMode ? createShadowRouter() : AuthMetaResolverRouter.noop();
        return new AuthzQueryAppService(
            new InMemorySubjectRepository(shadowMode),
            new InMemoryAssignmentRepository(),
            new InMemoryPermissionRepository(),
            resourceRepository,
            new InMemoryMetaRepository(shadowMode),
            router,
            derivationPermissionRepository,
            derivationProperties
        );
    }

    private UserContextDerivationProperties derivationProperties(UserContextDerivationProperties.LoadMode mode,
                                                                 UserContextDerivationProperties.MissingBindingStrategy strategy) {
        UserContextDerivationProperties properties = new UserContextDerivationProperties();
        properties.setMode(mode);
        properties.setMissingBindingStrategy(strategy);
        return properties;
    }

    private AuthMetaResolverRouter createShadowRouter() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("shadowMenuAdapter", new ShadowMenuAdapter());
        return new AuthMetaResolverRouter(applicationContext);
    }

    private static final class InMemorySubjectRepository implements SubjectRepository {

        private final boolean includeGroupRelation;

        private InMemorySubjectRepository(boolean includeGroupRelation) {
            this.includeGroupRelation = includeGroupRelation;
        }

        @Override
        public SysUserAccount saveUser(SysUserAccount userAccount) {
            return userAccount;
        }

        @Override
        public List<AuthSubjectRelation> findRelationsByUserId(String tenantId, String appCode, String subjectId) {
            List<AuthSubjectRelation> relations = new java.util.ArrayList<>();
            relations.add(AuthSubjectRelation.builder()
                .tenantId(tenantId)
                .appCode(appCode)
                .subjectModel("SUB_USER")
                .subjectId(subjectId)
                .relatedSubjectModel("SUB_ROLE")
                .relatedSubjectId("ROLE_ANALYST")
                .relationType("ROLE")
                .build());
            if (includeGroupRelation) {
                relations.add(AuthSubjectRelation.builder()
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .subjectModel("SUB_USER")
                    .subjectId(subjectId)
                    .relatedSubjectModel("SUB_GROUP")
                    .relatedSubjectId("GROUP_SHADOW")
                    .relationType("GROUP")
                    .build());
            }
            return relations;
        }
    }

    private static final class InMemoryAssignmentRepository implements AssignmentRepository {

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

        @Override
        public List<SysAuthAssignment> findAssignmentsBySubjectSet(String tenantId, String appCode, List<SubjectKey> subjectKeys) {
            Set<String> keys = subjectKeys.stream()
                .map(key -> key.getSubjectType() + ":" + key.getSubjectId())
                .collect(Collectors.toSet());
            List<SysAuthAssignment> assignments = new java.util.ArrayList<>();
            if (keys.contains("SUB_ROLE:ROLE_ANALYST")) {
                assignments.add(SysAuthAssignment.builder()
                    .id(1L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .subjectModel("SUB_ROLE")
                    .subjectId("ROLE_ANALYST")
                    .permItemId(11L)
                    .build());
            }
            if (keys.contains("SUB_GROUP:GROUP_SHADOW")) {
                assignments.add(SysAuthAssignment.builder()
                    .id(2L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .subjectModel("SUB_GROUP")
                    .subjectId("GROUP_SHADOW")
                    .permItemId(12L)
                    .build());
            }
            return assignments;
        }
    }

    private static final class InMemoryPermissionRepository implements PermissionRepository {

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
            return permItemIds.stream()
                .map(permItemId -> buildPermissionItem(tenantId, appCode, permItemId))
                .filter(permissionItem -> permissionItem != null)
                .collect(Collectors.toList());
        }

        @Override
        public List<AuthPermissionItem> findPermissionItemsByResModelCode(String tenantId, String appCode, String resModelCode) {
            if ("RES_UI_MENU".equals(resModelCode)) {
                return Collections.singletonList(AuthPermissionItem.builder()
                    .id(11L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .permCode("PERM_MENU_REPORTS")
                    .resModelCode("RES_UI_MENU")
                    .resId("MENU_REPORTS")
                    .build());
            }
            if ("RES_UI_COMPONENT".equals(resModelCode)) {
                return Collections.singletonList(AuthPermissionItem.builder()
                    .id(12L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .permCode("PERM_COMPONENT_EXPORT")
                    .resModelCode("RES_UI_COMPONENT")
                    .resId("BTN_EXPORT")
                    .build());
            }
            return Collections.emptyList();
        }

        private AuthPermissionItem buildPermissionItem(String tenantId, String appCode, Long permItemId) {
            if (Long.valueOf(11L).equals(permItemId)) {
                return AuthPermissionItem.builder()
                    .id(11L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .permCode("PERM_MENU_REPORTS")
                    .resModelCode("RES_UI_MENU")
                    .resId("MENU_REPORTS")
                    .build();
            }
            if (Long.valueOf(12L).equals(permItemId)) {
                return AuthPermissionItem.builder()
                    .id(12L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .permCode("PERM_COMPONENT_EXPORT")
                    .resModelCode("RES_UI_COMPONENT")
                    .resId("BTN_EXPORT")
                    .build();
            }
            return null;
        }
    }

    private static final class InMemoryResourceRepository implements ResourceRepository {

        @Override
        public List<SysResMenu> listMenus(String tenantId, String appCode) {
            return Arrays.asList(
                SysResMenu.builder()
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .menuCode("MENU_ROOT")
                    .menuName("首页")
                    .routePath("/home")
                    .sortNo(1)
                    .build(),
                SysResMenu.builder()
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .menuCode("MENU_REPORTS")
                    .menuName("报表中心")
                    .parentMenuCode("MENU_ROOT")
                    .routePath("/reports")
                    .sortNo(10)
                    .build(),
                SysResMenu.builder()
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .menuCode("MENU_ADMIN")
                    .menuName("系统管理")
                    .parentMenuCode("MENU_ROOT")
                    .routePath("/admin")
                    .sortNo(20)
                    .build()
            );
        }

        @Override
        public List<SysResComponent> listComponents(String tenantId, String appCode) {
            return Arrays.asList(
                SysResComponent.builder()
                    .id(201L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .componentCode("BTN_EXPORT")
                    .componentName("导出")
                    .build(),
                SysResComponent.builder()
                    .id(202L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .componentCode("BTN_AUDIT")
                    .componentName("审计")
                    .build()
            );
        }

        @Override
        public List<SysResPage> listPages(String tenantId, String appCode) {
            return Arrays.asList(
                SysResPage.builder()
                    .id(101L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .pageCode("PAGE_REPORTS")
                    .pageName("报表页")
                    .menuCode("MENU_REPORTS")
                    .pagePath("/reports")
                    .build(),
                SysResPage.builder()
                    .id(102L)
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .pageCode("PAGE_ADMIN")
                    .pageName("管理页")
                    .menuCode("MENU_ADMIN")
                    .pagePath("/admin")
                    .build()
            );
        }
    }

    private static final class FailingResourceRepository implements ResourceRepository {

        @Override
        public List<SysResMenu> listMenus(String tenantId, String appCode) {
            throw new AssertionError("Shadow 模式不应回退到内置菜单仓储");
        }
    }

    private static final class InMemoryMetaRepository implements MetaRepository {

        private final boolean shadowMode;

        private InMemoryMetaRepository(boolean shadowMode) {
            this.shadowMode = shadowMode;
        }

        @Override
        public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
            return definition;
        }

        @Override
        public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
            return definition;
        }

        @Override
        public AuthMetaModelDefinition findAuthMetaModel(String tenantId, String appCode, String modelCode) {
            if (!shadowMode || (!"RES_UI_MENU".equals(modelCode) && !"SUB_USER".equals(modelCode))) {
                return null;
            }
            return AuthMetaModelDefinition.builder()
                .tenantId(tenantId)
                .appCode(appCode)
                .modelCode(modelCode)
                .adapterType("JAVA_BEAN")
                .resolver("shadowMenuAdapter")
                .build();
        }
    }

    private static final class InMemoryDerivationPermissionRepository implements DerivationPermissionRepository {

        private final String boundResType;

        private final boolean hasBindings;

        private final List<String> allResourceCodes;

        private final Map<Long, List<String>> visibleCodesByPermItemId;

        private InMemoryDerivationPermissionRepository(String boundResType,
                                                       boolean hasBindings,
                                                       List<String> allResourceCodes,
                                                       Map<Long, List<String>> visibleCodesByPermItemId) {
            this.boundResType = boundResType;
            this.hasBindings = hasBindings;
            this.allResourceCodes = allResourceCodes;
            this.visibleCodesByPermItemId = visibleCodesByPermItemId;
        }

        @Override
        public boolean hasDerivationBindings(String tenantId, String appCode, String resType) {
            return hasBindings && resType.equals(boundResType);
        }

        @Override
        public List<String> findAllDerivedResourceCodes(String tenantId, String appCode, String resType) {
            if (!resType.equals(boundResType)) {
                return Collections.emptyList();
            }
            return allResourceCodes;
        }

        @Override
        public List<String> findDerivedResourceCodesByPermItemIds(String tenantId, String appCode,
                                                                  String resType, java.util.Collection<Long> permItemIds) {
            if (!resType.equals(boundResType) || permItemIds == null || permItemIds.isEmpty()) {
                return Collections.emptyList();
            }
            return permItemIds.stream()
                .map(visibleCodesByPermItemId::get)
                .filter(codes -> codes != null)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
        }
    }

    private static final class ShadowMenuAdapter implements AuthMetaModelAdapter {

        private final Map<String, DataItem> menuIndex = new HashMap<>();

        private ShadowMenuAdapter() {
            menuIndex.put("MENU_ROOT", DataItem.builder()
                .id("1")
                .code("MENU_ROOT")
                .name("首页")
                .status("ENABLED")
                .attributes(attributes(null, "/home"))
                .build());
            menuIndex.put("MENU_REPORTS", DataItem.builder()
                .id("2")
                .code("MENU_REPORTS")
                .name("报表中心")
                .status("ENABLED")
                .attributes(attributes("MENU_ROOT", "/reports"))
                .build());
        }

        @Override
        public SubjectHookResult fetchInstanceAttributes(ModelCode modelCode, String instanceId, Map<String, Object> requestContext) {
            return SubjectHookResult.builder().build();
        }

        @Override
        public List<DataItem> batchResolveItems(ModelCode modelCode, List<String> itemCodes) {
            return itemCodes.stream()
                .map(menuIndex::get)
                .filter(item -> item != null)
                .collect(Collectors.toList());
        }

        private Map<String, Object> attributes(String parentMenuCode, String routePath) {
            Map<String, Object> attributes = new HashMap<>();
            if (parentMenuCode != null) {
                attributes.put("parentMenuCode", parentMenuCode);
            }
            attributes.put("routePath", routePath);
            return attributes;
        }
    }
}
