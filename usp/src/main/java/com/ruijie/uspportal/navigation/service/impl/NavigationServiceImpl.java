package com.ruijie.uspportal.navigation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruijie.uspportal.appregistry.entity.AppRegistryEntity;
import com.ruijie.uspportal.appregistry.mapper.AppRegistryMapper;
import com.ruijie.uspportal.cache.CacheKeys;
import com.ruijie.uspportal.cache.PortalCacheService;
import com.ruijie.uspportal.common.BusinessException;
import com.ruijie.uspportal.integration.psp.PspUiVisibilityClient;
import com.ruijie.uspportal.navigation.dto.MenuSaveRequest;
import com.ruijie.uspportal.navigation.dto.NavigationNode;
import com.ruijie.uspportal.navigation.entity.MenuItemEntity;
import com.ruijie.uspportal.navigation.mapper.MenuItemMapper;
import com.ruijie.uspportal.navigation.service.NavigationService;
import com.ruijie.uspportal.security.CurrentUserContext;
import com.ruijie.uspportal.tenant.entity.TenantEntity;
import com.ruijie.uspportal.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 门户导航服务实现。
 *
 * <p>负责菜单数据的维护，并结合权限可见性与缓存生成最终导航树。</p>
 */
@Service
public class NavigationServiceImpl implements NavigationService {

    private final MenuItemMapper menuItemMapper;

    private final AppRegistryMapper appRegistryMapper;

    private final TenantRepository tenantRepository;

    private final PspUiVisibilityClient pspUiVisibilityClient;

    private final PortalCacheService portalCacheService;

    @Autowired
    public NavigationServiceImpl(MenuItemMapper menuItemMapper,
                                 AppRegistryMapper appRegistryMapper,
                                 TenantRepository tenantRepository,
                                 PspUiVisibilityClient pspUiVisibilityClient,
                                 PortalCacheService portalCacheService) {
        this.menuItemMapper = menuItemMapper;
        this.appRegistryMapper = appRegistryMapper;
        this.tenantRepository = tenantRepository;
        this.pspUiVisibilityClient = pspUiVisibilityClient;
        this.portalCacheService = portalCacheService;
    }

    @Value("${usp.portal.default-tenant-code}")
    private String defaultTenantCode;

    /**
     * 查询菜单配置列表。
     *
     * @return 菜单列表
     */
    @Override
    public List<MenuItemEntity> listMenus() {
        QueryWrapper<MenuItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0).orderByAsc("sort_no");
        return menuItemMapper.selectList(wrapper);
    }

    /**
     * 查询指定菜单详情。
     *
     * @param id 菜单主键
     * @return 菜单详情
     */
    @Override
    public MenuItemEntity getMenu(Long id) {
        MenuItemEntity entity = menuItemMapper.selectById(id);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BusinessException("菜单不存在");
        }
        return entity;
    }

    /**
     * 创建菜单配置。
     *
     * @param request 菜单保存请求
     * @return 创建后的菜单信息
     */
    @Override
    public MenuItemEntity create(MenuSaveRequest request) {
        MenuItemEntity entity = new MenuItemEntity();
        apply(entity, request);
        entity.setPublishStatus("DRAFT");
        entity.setStatus("ENABLED");
        menuItemMapper.insert(entity);
        clearTreeCache();
        return entity;
    }

    /**
     * 更新指定菜单配置。
     *
     * @param id 菜单主键
     * @param request 菜单保存请求
     * @return 更新后的菜单信息
     */
    @Override
    public MenuItemEntity update(Long id, MenuSaveRequest request) {
        MenuItemEntity entity = getMenu(id);
        apply(entity, request);
        menuItemMapper.updateById(entity);
        clearTreeCache();
        return getMenu(id);
    }

    /**
     * 发布指定菜单配置。
     *
     * @param id 菜单主键
     */
    @Override
    public void publish(Long id) {
        MenuItemEntity entity = getMenu(id);
        entity.setPublishStatus("PUBLISHED");
        menuItemMapper.updateById(entity);
        clearTreeCache();
    }

    /**
     * 查询当前用户可见的导航树。
     *
     * @return 导航树节点列表
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<NavigationNode> tree() {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.get();
        String cacheKey = CacheKeys.NAVIGATION_TREE_PREFIX + (currentUser == null ? "anonymous" : currentUser.getUserId());
        List<NavigationNode> cached = portalCacheService.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }
        QueryWrapper<MenuItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
                .eq("publish_status", "PUBLISHED")
                .eq("status", "ENABLED")
                .orderByAsc("sort_no");
        List<MenuItemEntity> entities = menuItemMapper.selectList(wrapper);
        Map<String, Boolean> visibility = pspUiVisibilityClient.batchCheck(entities.stream()
                .map(MenuItemEntity::getPermissionCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList()));
        List<MenuItemEntity> filtered = entities.stream()
                .filter(item -> !StringUtils.hasText(item.getPermissionCode()) || Boolean.TRUE.equals(visibility.get(item.getPermissionCode())))
                .collect(Collectors.toList());
        List<NavigationNode> nodes = buildTree(filtered);
        portalCacheService.put(cacheKey, nodes);
        return nodes;
    }

    /**
     * 根据菜单实体构建树形导航结构。
     *
     * @param entities 菜单实体集合
     * @return 树形节点列表
     */
    private List<NavigationNode> buildTree(List<MenuItemEntity> entities) {
        Map<Long, NavigationNode> nodeMap = new LinkedHashMap<>();
        for (MenuItemEntity entity : entities) {
            nodeMap.put(entity.getId(), NavigationNode.from(entity));
        }
        List<NavigationNode> roots = new ArrayList<>();
        entities.sort(Comparator.comparing(item -> item.getSortNo() == null ? 0 : item.getSortNo()));
        for (MenuItemEntity entity : entities) {
            NavigationNode current = nodeMap.get(entity.getId());
            if (entity.getParentId() == null || !nodeMap.containsKey(entity.getParentId())) {
                roots.add(current);
            } else {
                nodeMap.get(entity.getParentId()).getChildren().add(current);
            }
        }
        return roots;
    }

    /**
     * 将菜单保存请求写入菜单实体。
     *
     * @param entity 菜单实体
     * @param request 菜单保存请求
     */
    private void apply(MenuItemEntity entity, MenuSaveRequest request) {
        entity.setMenuCode(request.getMenuCode());
        entity.setMenuName(request.getMenuName());
        entity.setMenuType(request.getMenuType());
        entity.setAppId(request.getAppId());
        entity.setAppCode(resolveAppCode(request.getAppId()));
        entity.setParentId(request.getParentId());
        entity.setRoutePath(request.getRoutePath());
        entity.setTargetUrl(request.getTargetUrl());
        entity.setMenuIcon(request.getMenuIcon());
        entity.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        entity.setTreeLevel(request.getParentId() == null ? 1 : 2);
        entity.setTreePath(request.getParentId() == null ? "/" + request.getMenuCode() : null);
        entity.setPermissionCode(request.getPermissionCode());
        TenantEntity tenant = resolveTenant(request.getTenantCode());
        entity.setTenantId(String.valueOf(tenant.getId()));
        entity.setTenantCode(tenant.getTenantCode());
    }

    /**
     * 解析并校验菜单所属租户。
     *
     * @param requestedTenantCode 请求中的租户编码
     * @return 租户实体
     */
    private TenantEntity resolveTenant(String requestedTenantCode) {
        String tenantCode = requestedTenantCode;
        if (!StringUtils.hasText(tenantCode)) {
            CurrentUserContext.CurrentUser currentUser = CurrentUserContext.get();
            tenantCode = currentUser == null ? defaultTenantCode : currentUser.getTenantCode();
        }
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException("所属租户不能为空");
        }
        TenantEntity tenant = tenantRepository.findByCode(tenantCode.trim());
        if (tenant == null) {
            throw new BusinessException(404, "所属租户不存在");
        }
        return tenant;
    }

    /**
     * 根据应用主键解析应用编码。
     *
     * @param appId 应用主键
     * @return 应用编码
     */
    private String resolveAppCode(Long appId) {
        if (appId == null) {
            throw new BusinessException("所属应用不能为空");
        }
        AppRegistryEntity app = appRegistryMapper.selectById(appId);
        if (app == null || Integer.valueOf(1).equals(app.getDeleted())) {
            throw new BusinessException(404, "所属应用不存在");
        }
        return app.getAppCode();
    }

    /**
     * 清理当前用户对应的导航树缓存。
     */
    private void clearTreeCache() {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.get();
        if (currentUser != null) {
            portalCacheService.evict(CacheKeys.NAVIGATION_TREE_PREFIX + currentUser.getUserId());
        }
    }
}
