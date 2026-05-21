package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.common.ResourceModelCode;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.resource.SysResComponent;
import com.ruijie.authzengine.domain.model.governance.resource.SysResMenu;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthPermissionItemEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysResApiEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysResComponentEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysResMenuEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysResPageEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthPermissionItemPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResApiPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResComponentPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResMenuPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResPagePersistenceService;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 治理资源目录仓储实现。
 * <p>管理菜单（Menu）、页面（Page）、组件（Component）、API 四类资源的 CRUD。
 * 资源类型对应权限项中的 resModelCode，删除前必须检查权限项引用关系。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabaseResourceRepository implements ResourceRepository {

    private final SysResMenuPersistenceService sysResMenuPersistenceService;

    private final SysResPagePersistenceService sysResPagePersistenceService;

    private final SysResComponentPersistenceService sysResComponentPersistenceService;

    private final SysResApiPersistenceService sysResApiPersistenceService;

    private final AuthPermissionItemPersistenceService authPermissionItemPersistenceService;

    private final DerivationPermissionRepository derivationPermissionRepository;

    /**
     * 保存菜单资源，已存在则更新，不存在则新建。
     */
    @Override
    public SysResMenu saveMenu(SysResMenu sysResMenu) {
        SysResMenuEntity existing = findMenuEntity(sysResMenu.getTenantId(), sysResMenu.getAppCode(), sysResMenu.getMenuCode());
        SysResMenuEntity entity = toEntity(sysResMenu);
        boolean isUpdate = existing != null;
        if (isUpdate) {
            entity.setId(existing.getId());
        }
        log.info("[资源仓储] {}菜单: tenantId={}, appCode={}, menuCode={}",
            isUpdate ? "更新" : "新增",
            sysResMenu.getTenantId(), sysResMenu.getAppCode(), sysResMenu.getMenuCode());
        sysResMenuPersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<SysResMenu> pageMenus(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<SysResMenu> records = sysResMenuPersistenceService.lambdaQuery()
            .eq(SysResMenuEntity::getTenantId, tenantId)
            .eq(SysResMenuEntity::getAppCode, appCode)
            .orderByAsc(SysResMenuEntity::getSortNo)
            .orderByAsc(SysResMenuEntity::getMenuCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .filter(item -> matchesKeyword(keyword, item.getMenuCode(), item.getMenuName(), item.getParentMenuCode(), item.getRoutePath()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public SysResMenu findMenu(String tenantId, String appCode, String menuCode) {
        return toDefinition(findMenuEntity(tenantId, appCode, menuCode));
    }

    @Override
    public List<SysResMenu> listMenus(String tenantId, String appCode) {
        return sysResMenuPersistenceService.lambdaQuery()
            .eq(SysResMenuEntity::getTenantId, tenantId)
            .eq(SysResMenuEntity::getAppCode, appCode)
            .orderByAsc(SysResMenuEntity::getSortNo)
            .orderByAsc(SysResMenuEntity::getMenuCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    /**
     * 删除菜单资源，删除前建议先调用 hasMenuReference 确认无下游引用。
     */
    @Override
    public void deleteMenu(String tenantId, String appCode, String menuCode) {
        log.info("[资源仓储] 删除菜单: tenantId={}, appCode={}, menuCode={}", tenantId, appCode, menuCode);
        SysResMenuEntity entity = findMenuEntity(tenantId, appCode, menuCode);
        if (entity != null) {
            sysResMenuPersistenceService.removeById(entity.getId());
            log.info("[资源仓储] 菜单已删除: id={}", entity.getId());
        } else {
            log.warn("[资源仓储] 待删除的菜单不存在，跳过: menuCode={}", menuCode);
        }
    }

    @Override
    public boolean hasMenuReference(String tenantId, String appCode, String menuCode) {
        SysResMenuEntity entity = findMenuEntity(tenantId, appCode, menuCode);
        if (entity == null) {
            return false;
        }
        if (sysResPagePersistenceService.lambdaQuery()
            .eq(SysResPageEntity::getTenantId, tenantId)
            .eq(SysResPageEntity::getAppCode, appCode)
            .eq(SysResPageEntity::getMenuId, entity.getId())
            .count() > 0) {
            return true;
        }
        return hasPermissionReference(tenantId, appCode, ResourceModelCode.RES_UI_MENU.name(), menuCode);
    }

    /**
     * 保存页面资源，已存在则更新，不存在则新建。
     */
    @Override
    public SysResPage savePage(SysResPage sysResPage) {
        SysResPageEntity existing = findPageEntity(sysResPage.getTenantId(), sysResPage.getAppCode(), sysResPage.getPageCode());
        SysResPageEntity entity = toEntity(sysResPage);
        boolean isUpdate = existing != null;
        if (isUpdate) {
            entity.setId(existing.getId());
        }
        log.info("[资源仓储] {}页面: tenantId={}, appCode={}, pageCode={}",
            isUpdate ? "更新" : "新增",
            sysResPage.getTenantId(), sysResPage.getAppCode(), sysResPage.getPageCode());
        sysResPagePersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<SysResPage> pagePages(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<SysResPage> records = toPageDefinitions(sysResPagePersistenceService.lambdaQuery()
            .eq(SysResPageEntity::getTenantId, tenantId)
            .eq(SysResPageEntity::getAppCode, appCode)
            .orderByAsc(SysResPageEntity::getPageCode)
            .list()
        ).stream()
            .filter(item -> matchesKeyword(keyword, item.getPageCode(), item.getPageName(), item.getMenuCode(), item.getPagePath()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public SysResPage findPage(String tenantId, String appCode, String pageCode) {
        return toDefinition(findPageEntity(tenantId, appCode, pageCode));
    }

    @Override
    public SysResPage findPageById(String tenantId, String appCode, Long pageId) {
        if (pageId == null) {
            return null;
        }
        return toDefinition(sysResPagePersistenceService.lambdaQuery()
            .eq(SysResPageEntity::getTenantId, tenantId)
            .eq(SysResPageEntity::getAppCode, appCode)
            .eq(SysResPageEntity::getId, pageId)
            .one());
    }

    @Override
    public List<SysResPage> listPages(String tenantId, String appCode) {
        return toPageDefinitions(sysResPagePersistenceService.lambdaQuery()
            .eq(SysResPageEntity::getTenantId, tenantId)
            .eq(SysResPageEntity::getAppCode, appCode)
            .orderByAsc(SysResPageEntity::getPageCode)
            .list()
        );
    }

    /**
     * 删除页面资源，删除前建议先调用 hasPageReference 确认无下游引用。
     */
    @Override
    public void deletePage(String tenantId, String appCode, String pageCode) {
        log.info("[资源仓储] 删除页面: tenantId={}, appCode={}, pageCode={}", tenantId, appCode, pageCode);
        SysResPageEntity entity = findPageEntity(tenantId, appCode, pageCode);
        if (entity != null) {
            derivationPermissionRepository.deleteBindingsByResource(
                tenantId, appCode, ResourceModelCode.RES_UI_PAGE.name(), entity.getId());
            sysResPagePersistenceService.removeById(entity.getId());
            log.info("[资源仓储] 页面已删除: id={}", entity.getId());
        } else {
            log.warn("[资源仓储] 待删除的页面不存在，跳过: pageCode={}", pageCode);
        }
    }

    @Override
    public boolean hasPageReference(String tenantId, String appCode, String pageCode) {
        SysResPageEntity entity = findPageEntity(tenantId, appCode, pageCode);
        if (entity == null) {
            return false;
        }
        if (sysResComponentPersistenceService.lambdaQuery()
            .eq(SysResComponentEntity::getTenantId, tenantId)
            .eq(SysResComponentEntity::getAppCode, appCode)
            .eq(SysResComponentEntity::getPageId, entity.getId())
            .count() > 0) {
            return true;
        }
        return hasPermissionReference(tenantId, appCode, ResourceModelCode.RES_UI_PAGE.name(), pageCode);
    }

    @Override
    public SysResComponent saveComponent(SysResComponent sysResComponent) {
        SysResComponentEntity existing = findComponentEntity(
            sysResComponent.getTenantId(),
            sysResComponent.getAppCode(),
            sysResComponent.getComponentCode()
        );
        SysResComponentEntity entity = toEntity(sysResComponent);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        sysResComponentPersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<SysResComponent> pageComponents(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<SysResComponent> records = sysResComponentPersistenceService.lambdaQuery()
            .eq(SysResComponentEntity::getTenantId, tenantId)
            .eq(SysResComponentEntity::getAppCode, appCode)
            .orderByAsc(SysResComponentEntity::getComponentCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .filter(item -> matchesKeyword(
                keyword,
                item.getComponentCode(),
                item.getComponentName(),
                item.getPageCode(),
                item.getComponentType()
            ))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public SysResComponent findComponent(String tenantId, String appCode, String componentCode) {
        return toDefinition(findComponentEntity(tenantId, appCode, componentCode));
    }

    @Override
    public SysResComponent findComponentById(String tenantId, String appCode, Long componentId) {
        if (componentId == null) {
            return null;
        }
        return toDefinition(sysResComponentPersistenceService.lambdaQuery()
            .eq(SysResComponentEntity::getTenantId, tenantId)
            .eq(SysResComponentEntity::getAppCode, appCode)
            .eq(SysResComponentEntity::getId, componentId)
            .one());
    }

    @Override
    public List<SysResComponent> listComponents(String tenantId, String appCode) {
        return sysResComponentPersistenceService.lambdaQuery()
            .eq(SysResComponentEntity::getTenantId, tenantId)
            .eq(SysResComponentEntity::getAppCode, appCode)
            .orderByAsc(SysResComponentEntity::getComponentCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteComponent(String tenantId, String appCode, String componentCode) {
        SysResComponentEntity entity = findComponentEntity(tenantId, appCode, componentCode);
        if (entity != null) {
            derivationPermissionRepository.deleteBindingsByResource(
                tenantId, appCode, ResourceModelCode.RES_UI_COMPONENT.name(), entity.getId());
            sysResComponentPersistenceService.removeById(entity.getId());
        }
    }

    @Override
    public boolean hasComponentReference(String tenantId, String appCode, String componentCode) {
        return hasPermissionReference(tenantId, appCode, ResourceModelCode.RES_UI_COMPONENT.name(), componentCode);
    }

    /**
     * 保存 API 资源。
     */
    @Override
    public SysResApi saveApi(SysResApi sysResApi) {
        SysResApiEntity existing = findApiEntity(sysResApi.getTenantId(), sysResApi.getAppCode(), sysResApi.getApiCode());
        SysResApiEntity entity = toEntity(sysResApi);
        boolean isUpdate = existing != null;
        if (isUpdate) {
            entity.setId(existing.getId());
        }
        log.info("[资源仓储] {}API: tenantId={}, appCode={}, apiCode={}, method={}",
            isUpdate ? "更新" : "新增",
            sysResApi.getTenantId(), sysResApi.getAppCode(), sysResApi.getApiCode(), sysResApi.getHttpMethod());
        sysResApiPersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<SysResApi> pageApis(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<SysResApi> records = listApis(tenantId, appCode).stream()
            .filter(item -> matchesKeyword(keyword, item.getApiCode(), item.getApiName(), item.getHttpMethod(), item.getUriPattern()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public SysResApi findApi(String tenantId, String appCode, String apiCode) {
        return toDefinition(findApiEntity(tenantId, appCode, apiCode));
    }

    @Override
    public SysResApi findApiById(String tenantId, String appCode, Long apiId) {
        if (apiId == null) {
            return null;
        }
        return toDefinition(sysResApiPersistenceService.lambdaQuery()
            .eq(SysResApiEntity::getTenantId, tenantId)
            .eq(SysResApiEntity::getAppCode, appCode)
            .eq(SysResApiEntity::getId, apiId)
            .one());
    }

    /**
     * 删除 API 资源。
     */
    @Override
    public void deleteApi(String tenantId, String appCode, String apiCode) {
        log.info("[资源仓储] 删除API: tenantId={}, appCode={}, apiCode={}", tenantId, appCode, apiCode);
        SysResApiEntity entity = findApiEntity(tenantId, appCode, apiCode);
        if (entity != null) {
            derivationPermissionRepository.deleteBindingsByResource(
                tenantId, appCode, ResourceModelCode.RES_API.name(), entity.getId());
            sysResApiPersistenceService.removeById(entity.getId());
            log.info("[资源仓储] API已删除: id={}", entity.getId());
        } else {
            log.warn("[资源仓储] 待删除的API不存在，跳过: apiCode={}", apiCode);
        }
    }

    @Override
    public boolean hasApiReference(String tenantId, String appCode, String apiCode) {
        return hasPermissionReference(tenantId, appCode, ResourceModelCode.RES_API.name(), apiCode);
    }

    @Override
    public List<SysResApi> listApis(String tenantId, String appCode) {
        return sysResApiPersistenceService.lambdaQuery()
            .eq(SysResApiEntity::getTenantId, tenantId)
            .eq(SysResApiEntity::getAppCode, appCode)
            .orderByAsc(SysResApiEntity::getApiCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    private SysResMenuEntity findMenuEntity(String tenantId, String appCode, String menuCode) {
        return sysResMenuPersistenceService.lambdaQuery()
            .eq(SysResMenuEntity::getTenantId, tenantId)
            .eq(SysResMenuEntity::getAppCode, appCode)
            .eq(SysResMenuEntity::getMenuCode, menuCode)
            .one();
    }

    private SysResPageEntity findPageEntity(String tenantId, String appCode, String pageCode) {
        return sysResPagePersistenceService.lambdaQuery()
            .eq(SysResPageEntity::getTenantId, tenantId)
            .eq(SysResPageEntity::getAppCode, appCode)
            .eq(SysResPageEntity::getPageCode, pageCode)
            .one();
    }

    private SysResComponentEntity findComponentEntity(String tenantId, String appCode, String componentCode) {
        return sysResComponentPersistenceService.lambdaQuery()
            .eq(SysResComponentEntity::getTenantId, tenantId)
            .eq(SysResComponentEntity::getAppCode, appCode)
            .eq(SysResComponentEntity::getComponentCode, componentCode)
            .one();
    }

    private SysResApiEntity findApiEntity(String tenantId, String appCode, String apiCode) {
        return sysResApiPersistenceService.lambdaQuery()
            .eq(SysResApiEntity::getTenantId, tenantId)
            .eq(SysResApiEntity::getAppCode, appCode)
            .eq(SysResApiEntity::getApiCode, apiCode)
            .one();
    }

    private SysResMenuEntity toEntity(SysResMenu sysResMenu) {
        SysResMenuEntity entity = new SysResMenuEntity();
        entity.setTenantId(sysResMenu.getTenantId());
        entity.setTenantCode(sysResMenu.getTenantCode() != null ? sysResMenu.getTenantCode() : sysResMenu.getTenantId());
        entity.setAppCode(sysResMenu.getAppCode());
        entity.setAppId(sysResMenu.getAppId());
        entity.setMenuCode(sysResMenu.getMenuCode());
        entity.setMenuName(sysResMenu.getMenuName());
        entity.setMenuIcon(sysResMenu.getMenuIcon());
        entity.setMenuType(sysResMenu.getMenuType() != null ? sysResMenu.getMenuType() : "MENU");
        entity.setRoutePath(sysResMenu.getRoutePath());
        entity.setTargetUrl(sysResMenu.getTargetUrl());
        entity.setParentId(resolveMenuId(sysResMenu.getTenantId(), sysResMenu.getAppCode(), sysResMenu.getParentMenuCode()));
        entity.setSortNo(sysResMenu.getSortNo() != null ? sysResMenu.getSortNo() : 0);
        entity.setTreeLevel(sysResMenu.getTreeLevel() != null ? sysResMenu.getTreeLevel() : 1);
        entity.setTreePath(sysResMenu.getTreePath());
        entity.setPermissionCode(sysResMenu.getPermissionCode());
        entity.setVisibleExpression(sysResMenu.getVisibleExpression());
        entity.setPublishStatus(sysResMenu.getPublishStatus() != null ? sysResMenu.getPublishStatus() : "DRAFT");
        entity.setStatus(sysResMenu.getStatus());
        return entity;
    }

    private SysResMenu toDefinition(SysResMenuEntity entity) {
        if (entity == null) {
            return null;
        }
        return SysResMenu.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .tenantCode(entity.getTenantCode())
            .appCode(entity.getAppCode())
            .appId(entity.getAppId())
            .menuCode(entity.getMenuCode())
            .menuName(entity.getMenuName())
            .menuIcon(entity.getMenuIcon())
            .menuType(entity.getMenuType())
            .routePath(entity.getRoutePath())
            .targetUrl(entity.getTargetUrl())
            .parentMenuCode(resolveMenuCode(entity.getTenantId(), entity.getAppCode(), entity.getParentId()))
            .parentId(entity.getParentId())
            .sortNo(entity.getSortNo())
            .treeLevel(entity.getTreeLevel())
            .treePath(entity.getTreePath())
            .permissionCode(entity.getPermissionCode())
            .visibleExpression(entity.getVisibleExpression())
            .publishStatus(entity.getPublishStatus())
            .status(entity.getStatus())
            .build();
    }

    private SysResPageEntity toEntity(SysResPage sysResPage) {
        SysResPageEntity entity = new SysResPageEntity();
        entity.setTenantId(sysResPage.getTenantId());
        entity.setAppCode(sysResPage.getAppCode());
        entity.setPageCode(sysResPage.getPageCode());
        entity.setPageName(sysResPage.getPageName());
        entity.setMenuId(resolveMenuId(sysResPage.getTenantId(), sysResPage.getAppCode(), sysResPage.getMenuCode()));
        entity.setPagePath(sysResPage.getPagePath());
        entity.setStatus(sysResPage.getStatus());
        entity.setSortOrder(sysResPage.getSortOrder() != null ? sysResPage.getSortOrder() : 0);
        return entity;
    }

    private SysResPage toDefinition(SysResPageEntity entity) {
        if (entity == null) {
            return null;
        }
        return SysResPage.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .pageCode(entity.getPageCode())
            .pageName(entity.getPageName())
            .menuCode(resolveMenuCode(entity.getTenantId(), entity.getAppCode(), entity.getMenuId()))
            .pagePath(entity.getPagePath())
            .status(entity.getStatus())
            .sortOrder(entity.getSortOrder())
            .build();
    }

    private List<SysResPage> toPageDefinitions(List<SysResPageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
            .map(entity -> SysResPage.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .appCode(entity.getAppCode())
                .pageCode(entity.getPageCode())
                .pageName(entity.getPageName())
                .menuCode(resolveMenuCode(entity.getTenantId(), entity.getAppCode(), entity.getMenuId()))
                .pagePath(entity.getPagePath())
                .status(entity.getStatus())
                .sortOrder(entity.getSortOrder())
                .build())
            .collect(Collectors.toList());
    }

    private SysResComponentEntity toEntity(SysResComponent sysResComponent) {
        SysResComponentEntity entity = new SysResComponentEntity();
        entity.setTenantId(sysResComponent.getTenantId());
        entity.setAppCode(sysResComponent.getAppCode());
        entity.setComponentCode(sysResComponent.getComponentCode());
        entity.setComponentName(sysResComponent.getComponentName());
        entity.setPageId(resolvePageId(sysResComponent.getTenantId(), sysResComponent.getAppCode(), sysResComponent.getPageCode()));
        entity.setComponentType(sysResComponent.getComponentType());
        entity.setStatus(sysResComponent.getStatus());
        return entity;
    }

    private SysResComponent toDefinition(SysResComponentEntity entity) {
        if (entity == null) {
            return null;
        }
        return SysResComponent.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .componentCode(entity.getComponentCode())
            .componentName(entity.getComponentName())
            .pageCode(resolvePageCode(entity.getTenantId(), entity.getAppCode(), entity.getPageId()))
            .componentType(entity.getComponentType())
            .status(entity.getStatus())
            .build();
    }

    private SysResApiEntity toEntity(SysResApi sysResApi) {
        SysResApiEntity entity = new SysResApiEntity();
        entity.setTenantId(sysResApi.getTenantId());
        entity.setAppCode(sysResApi.getAppCode());
        entity.setApiCode(sysResApi.getApiCode());
        entity.setApiName(StringUtils.hasText(sysResApi.getApiName())
            ? sysResApi.getApiName()
            : sysResApi.getApiCode());
        entity.setHttpMethod(sysResApi.getHttpMethod());
        entity.setUriPattern(sysResApi.getUriPattern());
        entity.setStatus(sysResApi.getStatus());
        return entity;
    }

    private SysResApi toDefinition(SysResApiEntity entity) {
        if (entity == null) {
            return null;
        }
        return SysResApi.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .apiCode(entity.getApiCode())
            .apiName(entity.getApiName())
            .httpMethod(entity.getHttpMethod())
            .uriPattern(entity.getUriPattern())
            .status(entity.getStatus())
            .build();
    }

    private Long resolveMenuId(String tenantId, String appCode, String menuCode) {
        if (!StringUtils.hasText(menuCode)) {
            return null;
        }
        SysResMenuEntity menuEntity = findMenuEntity(tenantId, appCode, menuCode);
        return menuEntity == null ? null : menuEntity.getId();
    }

    private String resolveMenuCode(String tenantId, String appCode, Long menuId) {
        if (menuId == null) {
            return null;
        }
        SysResMenuEntity menuEntity = sysResMenuPersistenceService.lambdaQuery()
            .eq(SysResMenuEntity::getTenantId, tenantId)
            .eq(SysResMenuEntity::getAppCode, appCode)
            .eq(SysResMenuEntity::getId, menuId)
            .one();
        return menuEntity == null ? null : menuEntity.getMenuCode();
    }

    private Long resolvePageId(String tenantId, String appCode, String pageCode) {
        if (!StringUtils.hasText(pageCode)) {
            return null;
        }
        SysResPageEntity pageEntity = findPageEntity(tenantId, appCode, pageCode);
        return pageEntity == null ? null : pageEntity.getId();
    }

    private String resolvePageCode(String tenantId, String appCode, Long pageId) {
        if (pageId == null) {
            return null;
        }
        SysResPageEntity pageEntity = sysResPagePersistenceService.lambdaQuery()
            .eq(SysResPageEntity::getTenantId, tenantId)
            .eq(SysResPageEntity::getAppCode, appCode)
            .eq(SysResPageEntity::getId, pageId)
            .one();
        return pageEntity == null ? null : pageEntity.getPageCode();
    }

    private boolean hasPermissionReference(String tenantId, String appCode, String resourceModelCode, String resourceId) {
        return authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .eq(AuthPermissionItemEntity::getResModelCode, resourceModelCode)
            .eq(AuthPermissionItemEntity::getResId, resourceId)
            .count() > 0;
    }

    private List<String> sanitizeCodes(Collection<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return rawCodes.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
    }

    private boolean matchesKeyword(String keyword, String... values) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    private <T> PageResult<T> buildPage(List<T> records, int pageNo, int pageSize) {
        List<T> safeRecords = records == null ? Collections.<T>emptyList() : records;
        int safePageNo = pageNo > 0 ? pageNo : 1;
        int safePageSize = pageSize > 0 ? pageSize : 20;
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, safeRecords.size());
        int toIndex = Math.min(fromIndex + safePageSize, safeRecords.size());
        return PageResult.<T>builder()
            .pageNo(safePageNo)
            .pageSize(safePageSize)
            .total(safeRecords.size())
            .records(safeRecords.subList(fromIndex, toIndex))
            .build();
    }
}