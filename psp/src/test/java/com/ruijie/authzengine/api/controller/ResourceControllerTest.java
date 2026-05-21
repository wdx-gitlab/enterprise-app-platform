package com.ruijie.authzengine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.assembler.ResourceAssembler;
import com.ruijie.authzengine.application.service.ResourceAppService;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.resource.SysResComponent;
import com.ruijie.authzengine.domain.model.governance.resource.SysResMenu;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.shared.exception.GlobalExceptionHandler;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

class ResourceControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private InMemoryResourceRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        repository = new InMemoryResourceRepository();
        mockMvc = buildMockMvc(repository);
    }

    @Test
    void shouldCrudResourceDirectories() throws Exception {
        mockMvc.perform(post("/authz-engine/api/v1/governance/resources/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"menuCode\":\"MENU-CONTRACT\",\"menuName\":\"合同管理\",\"routePath\":\"/contracts\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("MENU-CONTRACT"));

        mockMvc.perform(post("/authz-engine/api/v1/governance/resources/pages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"pageCode\":\"PAGE-CONTRACT-LIST\",\"pageName\":\"合同列表\",\"menuCode\":\"MENU-CONTRACT\",\"pagePath\":\"/contracts/list\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("PAGE-CONTRACT-LIST"));

        mockMvc.perform(post("/authz-engine/api/v1/governance/resources/components")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"componentCode\":\"BTN-EXPORT\",\"componentName\":\"导出按钮\",\"pageCode\":\"PAGE-CONTRACT-LIST\",\"componentType\":\"BUTTON\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("BTN-EXPORT"));

        mockMvc.perform(post("/authz-engine/api/v1/governance/resources/apis")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"apiCode\":\"API-CONTRACT-LIST\",\"apiName\":\"合同列表接口\",\"httpMethod\":\"GET\",\"uriPattern\":\"/api/contracts\",\"status\":\"ENABLED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("API-CONTRACT-LIST"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/resources/menus")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].menuCode").value("MENU-CONTRACT"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/resources/apis/API-CONTRACT-LIST")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.httpMethod").value("GET"));

        mockMvc.perform(put("/authz-engine/api/v1/governance/resources/apis/API-CONTRACT-LIST")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"apiCode\":\"API-CONTRACT-LIST\",\"apiName\":\"合同列表接口-更新\",\"httpMethod\":\"POST\",\"uriPattern\":\"/api/contracts/query\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("API-CONTRACT-LIST"));

        mockMvc.perform(delete("/authz-engine/api/v1/governance/resources/components/BTN-EXPORT")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("BTN-EXPORT"));
    }

    @Test
    void shouldRejectDeletingReferencedMenu() throws Exception {
        repository.saveMenu(SysResMenu.builder()
            .tenantId("T001")
            .appCode("CRM")
            .menuCode("MENU-CONTRACT")
            .menuName("合同管理")
            .build());
        repository.menuReferences.add("MENU-CONTRACT");

        mockMvc.perform(delete("/authz-engine/api/v1/governance/resources/menus/MENU-CONTRACT")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("AUTHZ-409-DELETE"))
            .andExpect(jsonPath("$.message", containsString("菜单资源仍被页面或权限项引用")));
    }

    @Test
    void shouldRejectDuplicatedApiRoute() throws Exception {
        repository.saveApi(SysResApi.builder()
            .tenantId("T001")
            .appCode("CRM")
            .apiCode("API-CONTRACT-LIST")
            .apiName("合同列表接口")
            .httpMethod("GET")
            .uriPattern("/api/contracts")
            .status("ENABLED")
            .build());

        mockMvc.perform(post("/authz-engine/api/v1/governance/resources/apis")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"apiCode\":\"API-CONTRACT-LIST-V2\",\"apiName\":\"合同列表接口二\",\"httpMethod\":\"GET\",\"uriPattern\":\"/api/contracts\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("AUTHZ-409"))
            .andExpect(jsonPath("$.message", containsString("同一 HTTP 方法 + URI 只能定义一个 API 资源")));
    }

    private MockMvc buildMockMvc(ResourceRepository resourceRepository) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        ResourceController controller = new ResourceController(
            new ResourceAppService(resourceRepository,
                new com.ruijie.authzengine.domain.repository.MetaRepository() {
                    @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
                    @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
                },
                com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter.noop()),
            new ResourceAssembler()
        );
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    private static final class InMemoryResourceRepository implements ResourceRepository {

        private final AtomicLong idGenerator = new AtomicLong(1L);

        private final Map<String, SysResMenu> menus = new LinkedHashMap<>();

        private final Map<String, SysResPage> pages = new LinkedHashMap<>();

        private final Map<String, SysResComponent> components = new LinkedHashMap<>();

        private final Map<String, SysResApi> apis = new LinkedHashMap<>();

        private final LinkedHashSet<String> menuReferences = new LinkedHashSet<>();

        @Override
        public SysResMenu saveMenu(SysResMenu sysResMenu) {
            if (sysResMenu.getId() == null) {
                sysResMenu.setId(idGenerator.getAndIncrement());
            }
            menus.put(sysResMenu.getMenuCode(), sysResMenu);
            return sysResMenu;
        }

        @Override
        public PageResult<SysResMenu> pageMenus(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return page(filterByTenant(menus.values(), tenantId, appCode));
        }

        @Override
        public SysResMenu findMenu(String tenantId, String appCode, String menuCode) {
            return menus.get(menuCode);
        }

        @Override
        public void deleteMenu(String tenantId, String appCode, String menuCode) {
            menus.remove(menuCode);
        }

        @Override
        public boolean hasMenuReference(String tenantId, String appCode, String menuCode) {
            return menuReferences.contains(menuCode);
        }

        @Override
        public SysResPage savePage(SysResPage sysResPage) {
            if (sysResPage.getId() == null) {
                sysResPage.setId(idGenerator.getAndIncrement());
            }
            pages.put(sysResPage.getPageCode(), sysResPage);
            return sysResPage;
        }

        @Override
        public PageResult<SysResPage> pagePages(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return page(filterByTenant(pages.values(), tenantId, appCode));
        }

        @Override
        public SysResPage findPage(String tenantId, String appCode, String pageCode) {
            return pages.get(pageCode);
        }

        @Override
        public void deletePage(String tenantId, String appCode, String pageCode) {
            pages.remove(pageCode);
        }

        @Override
        public SysResComponent saveComponent(SysResComponent sysResComponent) {
            if (sysResComponent.getId() == null) {
                sysResComponent.setId(idGenerator.getAndIncrement());
            }
            components.put(sysResComponent.getComponentCode(), sysResComponent);
            return sysResComponent;
        }

        @Override
        public PageResult<SysResComponent> pageComponents(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return page(filterByTenant(components.values(), tenantId, appCode));
        }

        @Override
        public SysResComponent findComponent(String tenantId, String appCode, String componentCode) {
            return components.get(componentCode);
        }

        @Override
        public void deleteComponent(String tenantId, String appCode, String componentCode) {
            components.remove(componentCode);
        }

        @Override
        public SysResApi saveApi(SysResApi sysResApi) {
            if (sysResApi.getId() == null) {
                sysResApi.setId(idGenerator.getAndIncrement());
            }
            apis.put(sysResApi.getApiCode(), sysResApi);
            return sysResApi;
        }

        @Override
        public PageResult<SysResApi> pageApis(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return page(filterByTenant(apis.values(), tenantId, appCode));
        }

        @Override
        public SysResApi findApi(String tenantId, String appCode, String apiCode) {
            return apis.get(apiCode);
        }

        @Override
        public void deleteApi(String tenantId, String appCode, String apiCode) {
            apis.remove(apiCode);
        }

        @Override
        public List<SysResApi> listApis(String tenantId, String appCode) {
            return filterByTenant(apis.values(), tenantId, appCode);
        }

        private <T> PageResult<T> page(List<T> records) {
            return PageResult.<T>builder()
                .pageNo(1)
                .pageSize(records.size() == 0 ? 20 : records.size())
                .total(records.size())
                .records(records)
                .build();
        }

        private <T> List<T> filterByTenant(java.util.Collection<T> values, String tenantId, String appCode) {
            return values.stream()
                .filter(item -> {
                    if (item instanceof SysResMenu) {
                        SysResMenu menu = (SysResMenu) item;
                        return tenantId.equals(menu.getTenantId()) && appCode.equals(menu.getAppCode());
                    }
                    if (item instanceof SysResPage) {
                        SysResPage page = (SysResPage) item;
                        return tenantId.equals(page.getTenantId()) && appCode.equals(page.getAppCode());
                    }
                    if (item instanceof SysResComponent) {
                        SysResComponent component = (SysResComponent) item;
                        return tenantId.equals(component.getTenantId()) && appCode.equals(component.getAppCode());
                    }
                    SysResApi api = (SysResApi) item;
                    return tenantId.equals(api.getTenantId()) && appCode.equals(api.getAppCode());
                })
                .collect(Collectors.toList());
        }
    }
}