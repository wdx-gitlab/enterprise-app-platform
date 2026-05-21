package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.application.spi.AuthMetaModelAdapter;
import com.ruijie.authzengine.application.spi.DataItem;
import com.ruijie.authzengine.application.spi.ModelCode;
import com.ruijie.authzengine.application.spi.ModelFieldSchema;
import com.ruijie.authzengine.domain.model.common.ResourceModelCode;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.resource.SysResComponent;
import com.ruijie.authzengine.domain.model.governance.resource.SysResMenu;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 治理资源目录应用服务。
 *
 * <p>所有 CRUD 方法内嵌 Shadow Mode 路由判断：
 * <ul>
 *   <li>resolver 有效（非空且非 noopHook）→ 调用宿主 {@link AuthMetaModelAdapter}</li>
 *   <li>否则 → 走引擎内置 Repository（现有逻辑不变）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ResourceAppService {

    private static final Logger log = LoggerFactory.getLogger(ResourceAppService.class);

    private final ResourceRepository resourceRepository;
    private final MetaRepository metaRepository;
    private final AuthMetaResolverRouter authMetaResolverRouter;

    // ────────── 菜单（RES_UI_MENU） ──────────

    /**
     * 分页查询菜单资源。
     */
    public PageResult<SysResMenu> pageMenus(String tenantId, String appCode, String keyword,
                                                       int pageNo, int pageSize) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_MENU");
        if (ctx != null) {
            PageResult<DataItem> result =
                ctx.adapter.pageItems(ModelCode.RES_UI_MENU, buildKeywordParams(keyword), pageNo, pageSize);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_MENU");
            }
            return ShadowDataMapper.toModelPage(result, SysResMenu.class, ctx.schema, tenantId, appCode);
        }
        return resourceRepository.pageMenus(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询菜单资源详情。
     */
    public SysResMenu getMenu(String tenantId, String appCode, String menuCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_MENU");
        if (ctx != null) {
            DataItem result = ctx.adapter.getItem(ModelCode.RES_UI_MENU, menuCode);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_MENU");
            }
            return ShadowDataMapper.toModel(result, SysResMenu.class, ctx.schema, tenantId, appCode);
        }
        SysResMenu sysResMenu = resourceRepository.findMenu(tenantId, appCode, menuCode);
        if (sysResMenu == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "菜单资源不存在");
        }
        return sysResMenu;
    }

    /**
     * 创建菜单资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysResMenu createMenu(SysResMenu sysResMenu) {
        ShadowAdapterContext ctx = tryResolveShadow(sysResMenu.getTenantId(), sysResMenu.getAppCode(), "RES_UI_MENU");
        if (ctx != null) {
            DataItem result = ctx.adapter.createItem(ModelCode.RES_UI_MENU,
                ShadowDataMapper.fromModel(sysResMenu, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_MENU");
            }
            return ShadowDataMapper.toModel(result, SysResMenu.class, ctx.schema,
                sysResMenu.getTenantId(), sysResMenu.getAppCode());
        }
        sysResMenu.setStatus(normalizeStatus(sysResMenu.getStatus()));
        return resourceRepository.saveMenu(sysResMenu);
    }

    /**
     * 更新菜单资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysResMenu updateMenu(String tenantId, String appCode, String menuCode, SysResMenu sysResMenu) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_MENU");
        if (ctx != null) {
            DataItem result = ctx.adapter.updateItem(ModelCode.RES_UI_MENU, menuCode,
                ShadowDataMapper.fromModel(sysResMenu, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_MENU");
            }
            return ShadowDataMapper.toModel(result, SysResMenu.class, ctx.schema, tenantId, appCode);
        }
        SysResMenu existing = getMenu(tenantId, appCode, menuCode);
        sysResMenu.setId(existing.getId());
        sysResMenu.setTenantId(tenantId);
        sysResMenu.setAppCode(appCode);
        sysResMenu.setMenuCode(menuCode);
        sysResMenu.setStatus(normalizeStatus(sysResMenu.getStatus()));
        return resourceRepository.saveMenu(sysResMenu);
    }

    /**
     * 删除菜单资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public void deleteMenu(String tenantId, String appCode, String menuCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_MENU");
        if (ctx != null) {
            boolean ok = ctx.adapter.deleteItem(ModelCode.RES_UI_MENU, menuCode);
            if (!ok) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_MENU");
            }
            return;
        }
        getMenu(tenantId, appCode, menuCode);
        if (resourceRepository.hasMenuReference(tenantId, appCode, menuCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "菜单资源仍被页面或权限项引用，禁止删除");
        }
        resourceRepository.deleteMenu(tenantId, appCode, menuCode);
    }

    // ────────── 页面（RES_UI_PAGE） ──────────

    /**
     * 分页查询页面资源。
     */
    public PageResult<SysResPage> pagePages(String tenantId, String appCode, String keyword,
                                                       int pageNo, int pageSize) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_PAGE");
        if (ctx != null) {
            PageResult<DataItem> result =
                ctx.adapter.pageItems(ModelCode.RES_UI_PAGE, buildKeywordParams(keyword), pageNo, pageSize);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_PAGE");
            }
            return ShadowDataMapper.toModelPage(result, SysResPage.class, ctx.schema, tenantId, appCode);
        }
        return resourceRepository.pagePages(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询页面资源详情。
     */
    public SysResPage getPage(String tenantId, String appCode, String pageCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_PAGE");
        if (ctx != null) {
            DataItem result = ctx.adapter.getItem(ModelCode.RES_UI_PAGE, pageCode);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_PAGE");
            }
            return ShadowDataMapper.toModel(result, SysResPage.class, ctx.schema, tenantId, appCode);
        }
        SysResPage sysResPage = resourceRepository.findPage(tenantId, appCode, pageCode);
        if (sysResPage == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "页面资源不存在");
        }
        return sysResPage;
    }

    /**
     * 创建页面资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysResPage createPage(SysResPage sysResPage) {
        ShadowAdapterContext ctx = tryResolveShadow(sysResPage.getTenantId(), sysResPage.getAppCode(), "RES_UI_PAGE");
        if (ctx != null) {
            DataItem result = ctx.adapter.createItem(ModelCode.RES_UI_PAGE,
                ShadowDataMapper.fromModel(sysResPage, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_PAGE");
            }
            return ShadowDataMapper.toModel(result, SysResPage.class, ctx.schema,
                sysResPage.getTenantId(), sysResPage.getAppCode());
        }
        sysResPage.setStatus(normalizeStatus(sysResPage.getStatus()));
        SysResPage savedPage = resourceRepository.savePage(sysResPage);
        return resourceRepository.findPage(savedPage.getTenantId(), savedPage.getAppCode(), savedPage.getPageCode());
    }

    /**
     * 更新页面资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysResPage updatePage(String tenantId, String appCode, String pageCode, SysResPage sysResPage) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_PAGE");
        if (ctx != null) {
            DataItem result = ctx.adapter.updateItem(ModelCode.RES_UI_PAGE, pageCode,
                ShadowDataMapper.fromModel(sysResPage, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_PAGE");
            }
            return ShadowDataMapper.toModel(result, SysResPage.class, ctx.schema, tenantId, appCode);
        }
        SysResPage existing = getPage(tenantId, appCode, pageCode);
        sysResPage.setId(existing.getId());
        sysResPage.setTenantId(tenantId);
        sysResPage.setAppCode(appCode);
        sysResPage.setPageCode(pageCode);
        sysResPage.setStatus(normalizeStatus(sysResPage.getStatus()));
        SysResPage savedPage = resourceRepository.savePage(sysResPage);
        return resourceRepository.findPage(savedPage.getTenantId(), savedPage.getAppCode(), savedPage.getPageCode());
    }

    /**
     * 删除页面资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public void deletePage(String tenantId, String appCode, String pageCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_PAGE");
        if (ctx != null) {
            boolean ok = ctx.adapter.deleteItem(ModelCode.RES_UI_PAGE, pageCode);
            if (!ok) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_PAGE");
            }
            return;
        }
        getPage(tenantId, appCode, pageCode);
        if (resourceRepository.hasPageReference(tenantId, appCode, pageCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "页面资源仍被组件或权限项引用，禁止删除");
        }
        resourceRepository.deletePage(tenantId, appCode, pageCode);
    }

    // ────────── 组件（RES_UI_COMPONENT） ──────────

    /**
     * 分页查询组件资源。
     */
    public PageResult<SysResComponent> pageComponents(String tenantId, String appCode, String keyword,
                                                                 int pageNo, int pageSize) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_COMPONENT");
        if (ctx != null) {
            PageResult<DataItem> result =
                ctx.adapter.pageItems(ModelCode.RES_UI_COMPONENT, buildKeywordParams(keyword), pageNo, pageSize);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_COMPONENT");
            }
            return ShadowDataMapper.toModelPage(result, SysResComponent.class, ctx.schema, tenantId, appCode);
        }
        return resourceRepository.pageComponents(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询组件资源详情。
     */
    public SysResComponent getComponent(String tenantId, String appCode, String componentCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_COMPONENT");
        if (ctx != null) {
            DataItem result = ctx.adapter.getItem(ModelCode.RES_UI_COMPONENT, componentCode);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_COMPONENT");
            }
            return ShadowDataMapper.toModel(result, SysResComponent.class, ctx.schema, tenantId, appCode);
        }
        SysResComponent sysResComponent = resourceRepository.findComponent(tenantId, appCode, componentCode);
        if (sysResComponent == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "组件资源不存在");
        }
        return sysResComponent;
    }

    /**
     * 创建组件资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysResComponent createComponent(SysResComponent sysResComponent) {
        ShadowAdapterContext ctx = tryResolveShadow(sysResComponent.getTenantId(), sysResComponent.getAppCode(), "RES_UI_COMPONENT");
        if (ctx != null) {
            DataItem result = ctx.adapter.createItem(ModelCode.RES_UI_COMPONENT,
                ShadowDataMapper.fromModel(sysResComponent, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_COMPONENT");
            }
            return ShadowDataMapper.toModel(result, SysResComponent.class, ctx.schema,
                sysResComponent.getTenantId(), sysResComponent.getAppCode());
        }
        sysResComponent.setStatus(normalizeStatus(sysResComponent.getStatus()));
        return resourceRepository.saveComponent(sysResComponent);
    }

    /**
     * 更新组件资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysResComponent updateComponent(String tenantId, String appCode, String componentCode,
                                            SysResComponent sysResComponent) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_COMPONENT");
        if (ctx != null) {
            DataItem result = ctx.adapter.updateItem(ModelCode.RES_UI_COMPONENT, componentCode,
                ShadowDataMapper.fromModel(sysResComponent, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_COMPONENT");
            }
            return ShadowDataMapper.toModel(result, SysResComponent.class, ctx.schema, tenantId, appCode);
        }
        SysResComponent existing = getComponent(tenantId, appCode, componentCode);
        sysResComponent.setId(existing.getId());
        sysResComponent.setTenantId(tenantId);
        sysResComponent.setAppCode(appCode);
        sysResComponent.setComponentCode(componentCode);
        sysResComponent.setStatus(normalizeStatus(sysResComponent.getStatus()));
        return resourceRepository.saveComponent(sysResComponent);
    }

    /**
     * 删除组件资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public void deleteComponent(String tenantId, String appCode, String componentCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_UI_COMPONENT");
        if (ctx != null) {
            boolean ok = ctx.adapter.deleteItem(ModelCode.RES_UI_COMPONENT, componentCode);
            if (!ok) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_UI_COMPONENT");
            }
            return;
        }
        getComponent(tenantId, appCode, componentCode);
        if (resourceRepository.hasComponentReference(tenantId, appCode, componentCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "组件资源仍被权限项引用，禁止删除");
        }
        resourceRepository.deleteComponent(tenantId, appCode, componentCode);
    }

    // ────────── API（RES_API） ──────────

    /**
     * 分页查询 API 资源。
     */
    public PageResult<SysResApi> pageApis(String tenantId, String appCode, String keyword,
                                                     int pageNo, int pageSize) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_API");
        if (ctx != null) {
            PageResult<DataItem> result =
                ctx.adapter.pageItems(ModelCode.RES_API, buildKeywordParams(keyword), pageNo, pageSize);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_API");
            }
            return ShadowDataMapper.toModelPage(result, SysResApi.class, ctx.schema, tenantId, appCode);
        }
        return resourceRepository.pageApis(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询 API 资源详情。
     */
    public SysResApi getApi(String tenantId, String appCode, String apiCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_API");
        if (ctx != null) {
            DataItem result = ctx.adapter.getItem(ModelCode.RES_API, apiCode);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_API");
            }
            return ShadowDataMapper.toModel(result, SysResApi.class, ctx.schema, tenantId, appCode);
        }
        SysResApi sysResApi = resourceRepository.findApi(tenantId, appCode, apiCode);
        if (sysResApi == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "API 资源不存在");
        }
        return sysResApi;
    }

    /**
     * 创建 API 资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysResApi createApi(SysResApi sysResApi) {
        ShadowAdapterContext ctx = tryResolveShadow(sysResApi.getTenantId(), sysResApi.getAppCode(), "RES_API");
        if (ctx != null) {
            validateApiRouteUniqueness(sysResApi, ctx);
            DataItem result = ctx.adapter.createItem(ModelCode.RES_API,
                ShadowDataMapper.fromModel(sysResApi, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_API");
            }
            return ShadowDataMapper.toModel(result, SysResApi.class, ctx.schema,
                sysResApi.getTenantId(), sysResApi.getAppCode());
        }
        return upsertApi(sysResApi);
    }

    /**
     * 更新 API 资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysResApi updateApi(String tenantId, String appCode, String apiCode, SysResApi sysResApi) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_API");
        if (ctx != null) {
            sysResApi.setTenantId(tenantId);
            sysResApi.setAppCode(appCode);
            sysResApi.setApiCode(apiCode);
            validateApiRouteUniqueness(sysResApi, ctx);
            DataItem result = ctx.adapter.updateItem(ModelCode.RES_API, apiCode,
                ShadowDataMapper.fromModel(sysResApi, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_API");
            }
            return ShadowDataMapper.toModel(result, SysResApi.class, ctx.schema, tenantId, appCode);
        }
        SysResApi existing = getApi(tenantId, appCode, apiCode);
        sysResApi.setId(existing.getId());
        sysResApi.setTenantId(tenantId);
        sysResApi.setAppCode(appCode);
        sysResApi.setApiCode(apiCode);
        return upsertApi(sysResApi);
    }

    /**
     * 删除 API 资源。
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public void deleteApi(String tenantId, String appCode, String apiCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "RES_API");
        if (ctx != null) {
            boolean ok = ctx.adapter.deleteItem(ModelCode.RES_API, apiCode);
            if (!ok) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=RES_API");
            }
            return;
        }
        getApi(tenantId, appCode, apiCode);
        if (resourceRepository.hasApiReference(tenantId, appCode, apiCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "API 资源仍被权限项引用，禁止删除");
        }
        resourceRepository.deleteApi(tenantId, appCode, apiCode);
    }

    // ────────── 保留方法 ──────────

    /**
     * 保存或更新 API 资源目录（无 Shadow 路由，供内部调用）。
     *
     * @param sysResApi API 资源定义
     * @return 已保存结果
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysResApi upsertApi(SysResApi sysResApi) {
        sysResApi.setStatus(normalizeStatus(sysResApi.getStatus()));
        validateApiRouteUniqueness(sysResApi);
        return resourceRepository.saveApi(sysResApi);
    }

    /**
     * 查询 API 资源目录（全量列表）。
     *
     * @param tenantId 租户标识
     * @param appCode  应用标识
     * @return API 资源列表
     */
    public List<SysResApi> listApis(String tenantId, String appCode) {
        return resourceRepository.listApis(tenantId, appCode);
    }

    // ────────── 私有辅助方法 ──────────

    /**
     * 尝试解析 Shadow 上下文。若为 Shadow Mode，返回 {@link ShadowAdapterContext}；否则返回 null。
     */
    private ShadowAdapterContext tryResolveShadow(String tenantId, String appCode, String modelCode) {
        AuthMetaModelDefinition metaDef = metaRepository.findAuthMetaModel(tenantId, appCode, modelCode);
        if (metaDef == null) {
            return null;
        }
        String resolver = metaDef.getResolver();
        if (!StringUtils.hasText(resolver) || "noopHook".equalsIgnoreCase(resolver.trim())) {
            return null;
        }
        AuthMetaModelAdapter adapter = authMetaResolverRouter.resolve(metaDef.getAdapterType(), resolver);
        if (adapter == null) {
            log.warn("[ShadowResource] 未找到有效适配器 Bean. tenantId={} appCode={} modelCode={} resolver={}",
                tenantId, appCode, modelCode, resolver);
            return null;
        }
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema(modelCode, metaDef.getSchemaView());
        return new ShadowAdapterContext(adapter, schema);
    }

    private Map<String, String> buildKeywordParams(String keyword) {
        Map<String, String> params = new HashMap<>(2);
        if (StringUtils.hasText(keyword)) {
            params.put("keyword", keyword);
        }
        return params;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status : "ENABLED";
    }

    private void validateApiRouteUniqueness(SysResApi sysResApi) {
        validateApiRouteUniqueness(sysResApi, null);
    }

    private void validateApiRouteUniqueness(SysResApi sysResApi, ShadowAdapterContext ctx) {
        if (sysResApi == null || !StringUtils.hasText(sysResApi.getTenantId()) || !StringUtils.hasText(sysResApi.getAppCode())) {
            return;
        }
        String normalizedMethod = normalizeRouteKey(sysResApi.getHttpMethod(), true);
        String normalizedUriPattern = normalizeRouteKey(sysResApi.getUriPattern(), false);
        if (!StringUtils.hasText(normalizedMethod) || !StringUtils.hasText(normalizedUriPattern)) {
            return;
        }
        boolean duplicated = listApisForRouteValidation(sysResApi.getTenantId(), sysResApi.getAppCode(), ctx).stream()
            .filter(item -> item != null)
            .filter(item -> !StringUtils.hasText(sysResApi.getApiCode()) || !sysResApi.getApiCode().equals(item.getApiCode()))
            .anyMatch(item -> normalizedMethod.equals(normalizeRouteKey(item.getHttpMethod(), true))
                && normalizedUriPattern.equals(normalizeRouteKey(item.getUriPattern(), false)));
        if (duplicated) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "同一 HTTP 方法 + URI 只能定义一个 API 资源，避免路由多命中");
        }
    }

    private List<SysResApi> listApisForRouteValidation(String tenantId, String appCode, ShadowAdapterContext ctx) {
        if (ctx == null) {
            return resourceRepository.listApis(tenantId, appCode);
        }
        List<SysResApi> shadowApis = new ArrayList<>();
        int pageNo = 1;
        int pageSize = 200;
        while (true) {
            PageResult<DataItem> result = ctx.adapter.pageItems(ModelCode.RES_API, Collections.<String, String>emptyMap(), pageNo, pageSize);
            if (result == null) {
                log.warn("[ShadowResource] API 路由唯一性校验回退本地仓储，因 pageItems 未实现 tenantId={} appCode={}", tenantId, appCode);
                return resourceRepository.listApis(tenantId, appCode);
            }
            List<DataItem> records = result.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            for (DataItem record : records) {
                shadowApis.add(ShadowDataMapper.toModel(record, SysResApi.class, ctx.schema, tenantId, appCode));
            }
            if (records.size() < pageSize || shadowApis.size() >= result.getTotal()) {
                break;
            }
            pageNo++;
        }
        return shadowApis;
    }

    private String normalizeRouteKey(String value, boolean upperCase) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return upperCase ? normalized.toUpperCase(Locale.ROOT) : normalized;
    }

    /**
     * Shadow 适配器上下文，合并 adapter + schema，避免两次查库。
     */
    private static final class ShadowAdapterContext {
        final AuthMetaModelAdapter adapter;
        final ModelFieldSchema schema;

        ShadowAdapterContext(AuthMetaModelAdapter adapter, ModelFieldSchema schema) {
            this.adapter = adapter;
            this.schema = schema;
        }
    }
}