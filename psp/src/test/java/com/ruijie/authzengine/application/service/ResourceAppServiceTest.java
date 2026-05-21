package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.application.spi.AuthMetaModelAdapter;
import com.ruijie.authzengine.application.spi.DataItem;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.resource.SysResMenu;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter;
import com.ruijie.authzengine.shared.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResourceAppServiceTest {

    private static final MetaRepository NOOP_META_REPO = new MetaRepository() {
        @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
        @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
    };

    @Test
    void shouldRejectDeletingReferencedPage() {
        InMemoryResourceRepository repository = new InMemoryResourceRepository();
        repository.page = SysResPage.builder()
            .tenantId("T001")
            .appCode("CRM")
            .pageCode("PAGE-CONTRACT-LIST")
            .pageName("合同列表")
            .status("ENABLED")
            .build();
        repository.pageReferenced = true;
        ResourceAppService resourceAppService = new ResourceAppService(
            repository, NOOP_META_REPO, AuthMetaResolverRouter.noop());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> resourceAppService.deletePage("T001", "CRM", "PAGE-CONTRACT-LIST"));

        Assertions.assertEquals("AUTHZ-409-DELETE", exception.getCode());
    }

    @Test
    void shouldPageApis() {
        InMemoryResourceRepository repository = new InMemoryResourceRepository();
        repository.api = SysResApi.builder()
            .tenantId("T001")
            .appCode("CRM")
            .apiCode("API-CONTRACT-LIST")
            .apiName("合同列表接口")
            .httpMethod("GET")
            .uriPattern("/api/contracts")
            .status("ENABLED")
            .build();
        ResourceAppService resourceAppService = new ResourceAppService(
            repository, NOOP_META_REPO, AuthMetaResolverRouter.noop());

        PageResult<SysResApi> result = resourceAppService.pageApis("T001", "CRM", "CONTRACT", 1, 20);

        Assertions.assertEquals(1, result.getTotal());
        Assertions.assertEquals("API-CONTRACT-LIST", result.getRecords().get(0).getApiCode());
    }

    @Test
    void shouldRejectDuplicatedApiRoute() {
        InMemoryResourceRepository repository = new InMemoryResourceRepository();
        repository.api = SysResApi.builder()
            .tenantId("T001")
            .appCode("CRM")
            .apiCode("API-CONTRACT-LIST")
            .apiName("合同列表接口")
            .httpMethod("GET")
            .uriPattern("/api/contracts")
            .status("ENABLED")
            .build();
        ResourceAppService resourceAppService = new ResourceAppService(
            repository, NOOP_META_REPO, AuthMetaResolverRouter.noop());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> resourceAppService.createApi(SysResApi.builder()
                .tenantId("T001")
                .appCode("CRM")
                .apiCode("API-CONTRACT-LIST-V2")
                .apiName("合同列表接口二")
                .httpMethod("get")
                .uriPattern("/api/contracts")
                .build()));

        Assertions.assertEquals("AUTHZ-409", exception.getCode());
    }

    @Test
    void shouldRejectDuplicatedApiRouteInShadowMode() {
        InMemoryResourceRepository repository = new InMemoryResourceRepository();
        AtomicInteger createCalls = new AtomicInteger();
        MetaRepository shadowMetaRepo = new MetaRepository() {
            @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition
                saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
            @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition
                saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
            @Override public AuthMetaModelDefinition findAuthMetaModel(String tenantId, String appCode, String modelCode) {
                if ("RES_API".equals(modelCode)) {
                    return AuthMetaModelDefinition.builder()
                        .tenantId(tenantId).appCode(appCode).modelCode(modelCode)
                        .adapterType("JAVA_BEAN").resolver("testApiAdapter").build();
                }
                return null;
            }
        };
        AuthMetaModelAdapter testAdapter = new AuthMetaModelAdapter() {
            @Override public com.ruijie.authzengine.application.spi.SubjectHookResult
                fetchInstanceAttributes(com.ruijie.authzengine.application.spi.ModelCode modelCode, String subjectId, Map<String, Object> ctx) { return null; }
            @Override public PageResult<DataItem> pageItems(
                com.ruijie.authzengine.application.spi.ModelCode modelCode, Map<String, String> queryParams, int pageNo, int pageSize) {
                java.util.Map<String, Object> attrs = new java.util.HashMap<>();
                attrs.put("httpMethod", "GET");
                attrs.put("uriPattern", "/api/shadow/contracts");
                return PageResult.<DataItem>builder()
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .total(1L)
                    .records(Collections.singletonList(DataItem.builder()
                        .code("API-SHADOW-EXIST")
                        .name("已存在 Shadow 接口")
                        .status("ENABLED")
                        .attributes(attrs)
                        .build()))
                    .build();
            }
            @Override public DataItem createItem(com.ruijie.authzengine.application.spi.ModelCode modelCode, DataItem item) {
                createCalls.incrementAndGet();
                return item;
            }
        };
        AuthMetaResolverRouter router = new AuthMetaResolverRouter(null) {
            @Override public AuthMetaModelAdapter resolve(String adapterType, String resolver) { return testAdapter; }
        };
        ResourceAppService service = new ResourceAppService(repository, shadowMetaRepo, router);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> service.createApi(SysResApi.builder()
                .tenantId("T001")
                .appCode("CRM")
                .apiCode("API-SHADOW-NEW")
                .apiName("新 Shadow 接口")
                .httpMethod("get")
                .uriPattern("/api/shadow/contracts")
                .build()));

        Assertions.assertEquals("AUTHZ-409", exception.getCode());
        Assertions.assertEquals(0, createCalls.get());
    }

    @Test
    void shouldCreatePageWithoutPageApiRelations() {
        InMemoryResourceRepository repository = new InMemoryResourceRepository();
        ResourceAppService resourceAppService = new ResourceAppService(
            repository, NOOP_META_REPO, AuthMetaResolverRouter.noop());

        SysResPage page = resourceAppService.createPage(SysResPage.builder()
            .tenantId("T001")
            .appCode("CRM")
            .pageCode("PAGE-CONTRACT-LIST")
            .pageName("合同列表")
            .pagePath("/contracts/list")
            .build());

        Assertions.assertEquals("PAGE-CONTRACT-LIST", page.getPageCode());
        Assertions.assertEquals("合同列表", page.getPageName());
    }

    @Test
    void shouldRoutePageMenusToAdapterInShadowMode() {
        InMemoryResourceRepository repository = new InMemoryResourceRepository();
        // 配置 Shadow Mode 元模型（resolver 非 noopHook）
        MetaRepository shadowMetaRepo = new MetaRepository() {
            @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition
                saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
            @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition
                saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
            @Override public AuthMetaModelDefinition findAuthMetaModel(String tenantId, String appCode, String modelCode) {
                if ("RES_UI_MENU".equals(modelCode)) {
                    return AuthMetaModelDefinition.builder()
                        .tenantId(tenantId).appCode(appCode).modelCode(modelCode)
                        .adapterType("JAVA_BEAN").resolver("testMenuAdapter").build();
                }
                return null;
            }
        };
        // 适配器返回测试数据
        AuthMetaModelAdapter testAdapter = new AuthMetaModelAdapter() {
            @Override public com.ruijie.authzengine.application.spi.SubjectHookResult
                fetchInstanceAttributes(com.ruijie.authzengine.application.spi.ModelCode modelCode, String subjectId, Map<String, Object> ctx) { return null; }
            @Override public PageResult<DataItem> pageItems(
                com.ruijie.authzengine.application.spi.ModelCode modelCode, Map<String, String> queryParams, int pageNo, int pageSize) {
                return PageResult.<DataItem>builder()
                    .pageNo(pageNo).pageSize(pageSize).total(1L)
                    .records(Collections.singletonList(
                        DataItem.builder().code("MENU-SHADOW").name("Shadow菜单").status("ENABLED").build()))
                    .build();
            }
        };
        AuthMetaResolverRouter router = new AuthMetaResolverRouter(null) {
            @Override public AuthMetaModelAdapter resolve(String adapterType, String resolver) { return testAdapter; }
        };
        ResourceAppService service = new ResourceAppService(repository, shadowMetaRepo, router);

        PageResult<SysResMenu> result = service.pageMenus("T001", "CRM", null, 1, 20);

        Assertions.assertEquals(1L, result.getTotal());
        Assertions.assertEquals("MENU-SHADOW", result.getRecords().get(0).getMenuCode());
        Assertions.assertEquals("Shadow菜单", result.getRecords().get(0).getMenuName());
    }

    @Test
    void shouldRouteGetApiToAdapterInShadowMode() {
        InMemoryResourceRepository repository = new InMemoryResourceRepository();
        MetaRepository shadowMetaRepo = new MetaRepository() {
            @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition
                saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
            @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition
                saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
            @Override public AuthMetaModelDefinition findAuthMetaModel(String tenantId, String appCode, String modelCode) {
                if ("RES_API".equals(modelCode)) {
                    return AuthMetaModelDefinition.builder()
                        .tenantId(tenantId).appCode(appCode).modelCode(modelCode)
                        .adapterType("JAVA_BEAN").resolver("testApiAdapter").build();
                }
                return null;
            }
        };
        AuthMetaModelAdapter testAdapter = new AuthMetaModelAdapter() {
            @Override public com.ruijie.authzengine.application.spi.SubjectHookResult
                fetchInstanceAttributes(com.ruijie.authzengine.application.spi.ModelCode modelCode, String subjectId, Map<String, Object> ctx) { return null; }
            @Override public DataItem getItem(com.ruijie.authzengine.application.spi.ModelCode modelCode, String itemId) {
                java.util.Map<String, Object> attrs = new java.util.HashMap<>();
                attrs.put("uriPattern", "/api/shadow");
                return DataItem.builder()
                    .code("API-SHADOW").name("Shadow接口").status("ENABLED").attributes(attrs).build();
            }
        };
        AuthMetaResolverRouter router = new AuthMetaResolverRouter(null) {
            @Override public AuthMetaModelAdapter resolve(String adapterType, String resolver) { return testAdapter; }
        };
        ResourceAppService service = new ResourceAppService(repository, shadowMetaRepo, router);

        SysResApi api = service.getApi("T001", "CRM", "API-SHADOW");

        Assertions.assertEquals("API-SHADOW", api.getApiCode());
        Assertions.assertEquals("Shadow接口", api.getApiName());
    }

    private static final class InMemoryResourceRepository implements ResourceRepository {

        private SysResPage page;

        private boolean pageReferenced;

        private SysResApi api;

        private final Map<String, List<String>> pageApiRelations = new LinkedHashMap<>();

        @Override
        public SysResPage findPage(String tenantId, String appCode, String pageCode) {
            return page != null && pageCode.equals(page.getPageCode()) ? page : null;
        }

        @Override
        public boolean hasPageReference(String tenantId, String appCode, String pageCode) {
            return pageReferenced;
        }

        @Override
        public PageResult<SysResApi> pageApis(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return PageResult.<SysResApi>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(api == null ? 0 : 1)
                .records(api == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(api))
                .build();
        }

        @Override
        public List<SysResApi> listApis(String tenantId, String appCode) {
            return api == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(api);
        }

        @Override
        public SysResApi saveApi(SysResApi sysResApi) {
            this.api = sysResApi;
            return sysResApi;
        }

        @Override
        public SysResPage savePage(SysResPage sysResPage) {
            this.page = sysResPage;
            return sysResPage;
        }
    }
}