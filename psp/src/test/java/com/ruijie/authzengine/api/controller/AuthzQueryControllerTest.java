package com.ruijie.authzengine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.application.service.AuthzQueryAppService;
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
import com.ruijie.authzengine.shared.exception.GlobalExceptionHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthzQueryControllerTest {

    private MockMvc mockMvc;

    private MockMvc shadowMockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = createMockMvc(false);
        shadowMockMvc = createMockMvc(true);
    }

    @Test
    void shouldReturnMenuTreeInUserContext() throws Exception {
        mockMvc.perform(get("/authz-engine/api/v1/authz/user-context")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("subjectId", "U100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.accessibleResources.RES_UI_MENU[0]").value("MENU_REPORTS"))
            .andExpect(jsonPath("$.data.menuTree[0].menuCode").value("MENU_ROOT"))
            .andExpect(jsonPath("$.data.menuTree[0].children[0].menuCode").value("MENU_REPORTS"))
            .andExpect(jsonPath("$.data.menuTree[0].children[0].routePath").value("/reports"));
    }

    @Test
    void shouldReturnShadowMenuTreeInUserContext() throws Exception {
        shadowMockMvc.perform(get("/authz-engine/api/v1/authz/user-context")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("subjectId", "U100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.menuTree[0].menuCode").value("MENU_ROOT"))
            .andExpect(jsonPath("$.data.menuTree[0].children[0].menuCode").value("MENU_REPORTS"))
            .andExpect(jsonPath("$.data.menuTree[0].children[0].routePath").value("/reports"));
    }

    @Test
    void shouldReturnShadowExpandedPermissionsInUserContext() throws Exception {
        shadowMockMvc.perform(get("/authz-engine/api/v1/authz/user-context")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("subjectId", "U100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.permCodes", containsInAnyOrder("PERM_MENU_REPORTS", "PERM_COMPONENT_EXPORT")));
    }

    @Test
    void shouldReturnDenyVisibilityWhenDerivationOnlyHasNoBindings() throws Exception {
        MockMvc derivationOnlyMockMvc = createMockMvc(
            false,
            new InMemoryDerivationPermissionRepository(null, false, Collections.emptyList(), Collections.emptyMap()),
            derivationProperties(UserContextDerivationProperties.LoadMode.DERIVATION_ONLY,
                UserContextDerivationProperties.MissingBindingStrategy.DENY)
        );

        derivationOnlyMockMvc.perform(post("/authz-engine/api/v1/authz/resources/visibility")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"subjectId\":\"U100\",\"subjectModel\":\"SUB_USER\",\"componentCodes\":[\"BTN_EXPORT\",\"BTN_AUDIT\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.visibility.BTN_EXPORT.visible").value(false))
            .andExpect(jsonPath("$.data.visibility.BTN_AUDIT.visible").value(false));
    }

    @Test
    void shouldReturnDerivedMenuTreeWhenPageBindingsExist() throws Exception {
        MockMvc derivationMenuMockMvc = createMockMvc(
            false,
            new InMemoryDerivationPermissionRepository(
                "RES_UI_PAGE",
                true,
                Arrays.asList("PAGE_REPORTS", "PAGE_ADMIN"),
                Collections.singletonMap(11L, Collections.singletonList("PAGE_ADMIN"))
            ),
            derivationProperties(UserContextDerivationProperties.LoadMode.COMPAT,
                UserContextDerivationProperties.MissingBindingStrategy.DENY)
        );

        derivationMenuMockMvc.perform(get("/authz-engine/api/v1/authz/user-context")
                .param("tenantId", "T001")
                .param("appCode", "CRM")
                .param("subjectId", "U100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.accessibleResources.RES_UI_MENU[0]").value("MENU_ADMIN"))
            .andExpect(jsonPath("$.data.menuTree[0].menuCode").value("MENU_ROOT"))
            .andExpect(jsonPath("$.data.menuTree[0].children[0].menuCode").value("MENU_ADMIN"));
    }

    private MockMvc createMockMvc(boolean shadowMode) {
        return createMockMvc(
            shadowMode,
            new InMemoryDerivationPermissionRepository(null, false, Collections.emptyList(), Collections.emptyMap()),
            derivationProperties(UserContextDerivationProperties.LoadMode.COMPAT,
                UserContextDerivationProperties.MissingBindingStrategy.DENY)
        );
    }

    private MockMvc createMockMvc(boolean shadowMode,
                                  DerivationPermissionRepository derivationPermissionRepository,
                                  UserContextDerivationProperties derivationProperties) {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        ResourceRepository resourceRepository = shadowMode
            ? new FailingResourceRepository()
            : new InMemoryResourceRepository();
        AuthMetaResolverRouter router = shadowMode ? createShadowRouter() : AuthMetaResolverRouter.noop();
        AuthzQueryAppService authzQueryAppService = new AuthzQueryAppService(
            new InMemorySubjectRepository(shadowMode),
            new InMemoryAssignmentRepository(),
            new InMemoryPermissionRepository(),
            resourceRepository,
            new InMemoryMetaRepository(shadowMode),
            router,
            derivationPermissionRepository,
            derivationProperties
        );
        return MockMvcBuilders.standaloneSetup(new AuthzQueryController(authzQueryAppService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
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
