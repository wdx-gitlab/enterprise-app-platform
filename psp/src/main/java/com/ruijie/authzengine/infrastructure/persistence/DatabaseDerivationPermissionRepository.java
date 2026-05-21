package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.common.ResourceModelCode;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.ResourceDerivationPermission;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthPermissionItemEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthResDerivationPermEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.AuthResDerivationPermMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysResApiEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysResComponentEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysResPageEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthPermissionItemPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthResDerivationPermPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResApiPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResComponentPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResPagePersistenceService;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 派生权限仓储数据库实现。
 *
 * <p>查询侧统一按 `tenant_id + app_code + res_type` 读取派生关系，
 * 再将 `res_id` 映射回页面、组件或 API 的业务编码。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabaseDerivationPermissionRepository implements DerivationPermissionRepository {

    private final AuthResDerivationPermPersistenceService authResDerivationPermPersistenceService;

    private final AuthResDerivationPermMapper authResDerivationPermMapper;

    private final SysResPagePersistenceService sysResPagePersistenceService;

    private final SysResComponentPersistenceService sysResComponentPersistenceService;

    private final SysResApiPersistenceService sysResApiPersistenceService;

    private final AuthPermissionItemPersistenceService authPermissionItemPersistenceService;

    @Override
    public PageResult<ResourceDerivationPermission> pageBindings(String tenantId, String appCode,
                                                                  String resType, String keyword,
                                                                  int pageNo, int pageSize) {
        List<AuthResDerivationPermEntity> all = authResDerivationPermPersistenceService.lambdaQuery()
            .eq(AuthResDerivationPermEntity::getTenantId, tenantId)
            .eq(AuthResDerivationPermEntity::getAppCode, appCode)
            .eq(StringUtils.hasText(resType), AuthResDerivationPermEntity::getResType, resType)
            .orderByAsc(AuthResDerivationPermEntity::getResType)
            .orderByAsc(AuthResDerivationPermEntity::getSortOrder)
            .orderByAsc(AuthResDerivationPermEntity::getId)
            .list();

        // 收集 resId -> resCode/resName 映射（按 resType 分组批量查）
        Map<Long, String> resCodeMap = new LinkedHashMap<>();
        Map<Long, String> resNameMap = new LinkedHashMap<>();
        buildResDisplayMaps(tenantId, appCode, all, resCodeMap, resNameMap);

        // 收集 permItemId -> permCode 映射
        Set<Long> permItemIds = all.stream()
            .map(AuthResDerivationPermEntity::getPermItemId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> permCodeMap = new LinkedHashMap<>();
        if (!permItemIds.isEmpty()) {
            authPermissionItemPersistenceService.lambdaQuery()
                .in(AuthPermissionItemEntity::getId, permItemIds)
                .list()
                .forEach(e -> permCodeMap.put(e.getId(), e.getPermCode()));
        }

        // keyword 后过滤（resCode / permCode 模糊匹配）
        List<ResourceDerivationPermission> filtered = all.stream()
            .filter(e -> {
                if (!StringUtils.hasText(keyword)) return true;
                String rc = resCodeMap.getOrDefault(e.getResId(), "");
                String pc = permCodeMap.getOrDefault(e.getPermItemId(), "");
                String kw = keyword.toLowerCase();
                return rc.toLowerCase().contains(kw) || pc.toLowerCase().contains(kw);
            })
            .map(e -> {
                ResourceDerivationPermission def = toDefinition(e);
                def.setResCode(resCodeMap.get(e.getResId()));
                def.setResName(resNameMap.get(e.getResId()));
                def.setPermCode(permCodeMap.get(e.getPermItemId()));
                return def;
            })
            .collect(Collectors.toList());

        // 内存分页
        int total = filtered.size();
        int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<ResourceDerivationPermission> records = fromIndex >= total
            ? Collections.emptyList()
            : filtered.subList(fromIndex, toIndex);

        return PageResult.<ResourceDerivationPermission>builder()
            .pageNo(pageNo).pageSize(pageSize).total(total).records(records).build();
    }

    /**
     * 按 res_type 分组批量查询 usp_* 表，构建 resId -> code/name 映射。
     */
    private void buildResDisplayMaps(String tenantId, String appCode,
                                     List<AuthResDerivationPermEntity> entities,
                                     Map<Long, String> resCodeMap,
                                     Map<Long, String> resNameMap) {
        Map<String, Set<Long>> byType = entities.stream()
            .collect(Collectors.groupingBy(
                AuthResDerivationPermEntity::getResType,
                Collectors.mapping(AuthResDerivationPermEntity::getResId,
                    Collectors.toCollection(LinkedHashSet::new))));

        byType.forEach((type, ids) -> {
            if (ids.isEmpty()) return;
            if (ResourceModelCode.RES_UI_PAGE.name().equals(type)) {
                sysResPagePersistenceService.lambdaQuery()
                    .eq(SysResPageEntity::getTenantId, tenantId)
                    .eq(SysResPageEntity::getAppCode, appCode)
                    .in(SysResPageEntity::getId, ids)
                    .list()
                    .forEach(e -> { resCodeMap.put(e.getId(), e.getPageCode()); resNameMap.put(e.getId(), e.getPageName()); });
            } else if (ResourceModelCode.RES_UI_COMPONENT.name().equals(type)) {
                sysResComponentPersistenceService.lambdaQuery()
                    .eq(SysResComponentEntity::getTenantId, tenantId)
                    .eq(SysResComponentEntity::getAppCode, appCode)
                    .in(SysResComponentEntity::getId, ids)
                    .list()
                    .forEach(e -> { resCodeMap.put(e.getId(), e.getComponentCode()); resNameMap.put(e.getId(), e.getComponentName()); });
            } else if (ResourceModelCode.RES_API.name().equals(type)) {
                sysResApiPersistenceService.lambdaQuery()
                    .eq(SysResApiEntity::getTenantId, tenantId)
                    .eq(SysResApiEntity::getAppCode, appCode)
                    .in(SysResApiEntity::getId, ids)
                    .list()
                    .forEach(e -> { resCodeMap.put(e.getId(), e.getApiCode()); resNameMap.put(e.getId(), e.getApiName()); });
            }
        });
    }

    @Override
    public ResourceDerivationPermission saveBinding(ResourceDerivationPermission binding) {
        AuthResDerivationPermEntity entity = toEntity(binding);
        boolean isRestore = false;
        if (binding != null && binding.getId() != null) {
            entity.setId(binding.getId());
        } else {
            AuthResDerivationPermEntity deleted = authResDerivationPermMapper.findDeletedBinding(
                binding.getTenantId(),
                binding.getAppCode(),
                binding.getResType(),
                binding.getResId(),
                binding.getPermItemId()
            );
            if (deleted != null) {
                entity.setId(deleted.getId());
                authResDerivationPermMapper.reviveById(entity);
                isRestore = true;
            }
        }
        if (!isRestore) {
            authResDerivationPermPersistenceService.saveOrUpdate(entity);
        }
        return toDefinition(entity);
    }

    @Override
    public ResourceDerivationPermission findBinding(String tenantId, String appCode, Long bindingId) {
        if (bindingId == null) {
            return null;
        }
        AuthResDerivationPermEntity entity = authResDerivationPermPersistenceService.lambdaQuery()
            .eq(AuthResDerivationPermEntity::getTenantId, tenantId)
            .eq(AuthResDerivationPermEntity::getAppCode, appCode)
            .eq(AuthResDerivationPermEntity::getId, bindingId)
            .one();
        return toDefinition(entity);
    }

    @Override
    public List<ResourceDerivationPermission> listBindingsByResource(String tenantId, String appCode,
                                                                    String resType, Long resId) {
        if (!StringUtils.hasText(resType) || resId == null) {
            return Collections.emptyList();
        }
        return authResDerivationPermPersistenceService.lambdaQuery()
            .eq(AuthResDerivationPermEntity::getTenantId, tenantId)
            .eq(AuthResDerivationPermEntity::getAppCode, appCode)
            .eq(AuthResDerivationPermEntity::getResType, resType)
            .eq(AuthResDerivationPermEntity::getResId, resId)
            .orderByAsc(AuthResDerivationPermEntity::getSortOrder)
            .orderByAsc(AuthResDerivationPermEntity::getId)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteBinding(String tenantId, String appCode, Long bindingId) {
        if (bindingId == null) {
            return;
        }
        int deleted = authResDerivationPermMapper.deletePhysicalById(tenantId, appCode, bindingId);
        if (deleted > 0) {
            log.info("[派生权限仓储] 物理删除派生关系: tenantId={}, appCode={}, bindingId={}", tenantId, appCode, bindingId);
        }
    }

    @Override
    public void deleteBindingsByResource(String tenantId, String appCode, String resType, Long resId) {
        if (!StringUtils.hasText(resType) || resId == null) {
            return;
        }
        List<Long> bindingIds = authResDerivationPermPersistenceService.lambdaQuery()
            .eq(AuthResDerivationPermEntity::getTenantId, tenantId)
            .eq(AuthResDerivationPermEntity::getAppCode, appCode)
            .eq(AuthResDerivationPermEntity::getResType, resType)
            .eq(AuthResDerivationPermEntity::getResId, resId)
            .list()
            .stream()
            .map(AuthResDerivationPermEntity::getId)
            .collect(Collectors.toList());
        if (!bindingIds.isEmpty()) {
            log.info("[派生权限仓储] 删除资源派生关系: tenantId={}, appCode={}, resType={}, resId={}, size={}",
                tenantId, appCode, resType, resId, bindingIds.size());
            authResDerivationPermMapper.deletePhysicalByIds(tenantId, appCode, bindingIds);
        }
    }

    @Override
    public void deleteBindingsByPermissionItemId(String tenantId, String appCode, Long permItemId) {
        if (permItemId == null) {
            return;
        }
        List<Long> bindingIds = authResDerivationPermPersistenceService.lambdaQuery()
            .eq(AuthResDerivationPermEntity::getTenantId, tenantId)
            .eq(AuthResDerivationPermEntity::getAppCode, appCode)
            .eq(AuthResDerivationPermEntity::getPermItemId, permItemId)
            .list()
            .stream()
            .map(AuthResDerivationPermEntity::getId)
            .collect(Collectors.toList());
        if (!bindingIds.isEmpty()) {
            log.info("[派生权限仓储] 删除权限项派生关系: tenantId={}, appCode={}, permItemId={}, size={}",
                tenantId, appCode, permItemId, bindingIds.size());
            authResDerivationPermMapper.deletePhysicalByIds(tenantId, appCode, bindingIds);
        }
    }

    @Override
    public boolean hasDerivationBindings(String tenantId, String appCode, String resType) {
        if (!StringUtils.hasText(resType)) {
            return false;
        }
        return authResDerivationPermPersistenceService.lambdaQuery()
            .eq(AuthResDerivationPermEntity::getTenantId, tenantId)
            .eq(AuthResDerivationPermEntity::getAppCode, appCode)
            .eq(AuthResDerivationPermEntity::getResType, resType)
            .count() > 0;
    }

    @Override
    public List<String> findAllDerivedResourceCodes(String tenantId, String appCode, String resType) {
        List<Long> resIds = authResDerivationPermPersistenceService.lambdaQuery()
            .eq(AuthResDerivationPermEntity::getTenantId, tenantId)
            .eq(AuthResDerivationPermEntity::getAppCode, appCode)
            .eq(AuthResDerivationPermEntity::getResType, resType)
            .orderByAsc(AuthResDerivationPermEntity::getSortOrder)
            .orderByAsc(AuthResDerivationPermEntity::getId)
            .list()
            .stream()
            .map(AuthResDerivationPermEntity::getResId)
            .collect(Collectors.toList());
        return resolveResourceCodes(tenantId, appCode, resType, resIds);
    }

    @Override
    public List<String> findDerivedResourceCodesByPermItemIds(String tenantId, String appCode,
                                                              String resType, Collection<Long> permItemIds) {
        if (permItemIds == null || permItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> resIds = authResDerivationPermPersistenceService.lambdaQuery()
            .eq(AuthResDerivationPermEntity::getTenantId, tenantId)
            .eq(AuthResDerivationPermEntity::getAppCode, appCode)
            .eq(AuthResDerivationPermEntity::getResType, resType)
            .in(AuthResDerivationPermEntity::getPermItemId, permItemIds)
            .orderByAsc(AuthResDerivationPermEntity::getSortOrder)
            .orderByAsc(AuthResDerivationPermEntity::getId)
            .list()
            .stream()
            .map(AuthResDerivationPermEntity::getResId)
            .collect(Collectors.toList());
        return resolveResourceCodes(tenantId, appCode, resType, resIds);
    }

    private List<String> resolveResourceCodes(String tenantId, String appCode, String resType, List<Long> rawResIds) {
        if (!StringUtils.hasText(resType) || rawResIds == null || rawResIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderedResIds = rawResIds.stream()
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
        if (orderedResIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> distinctResIds = new LinkedHashSet<>(orderedResIds);
        Map<Long, String> codeMap = new LinkedHashMap<>();
        if (ResourceModelCode.RES_UI_PAGE.name().equals(resType)) {
            codeMap.putAll(sysResPagePersistenceService.lambdaQuery()
                .eq(SysResPageEntity::getTenantId, tenantId)
                .eq(SysResPageEntity::getAppCode, appCode)
                .in(SysResPageEntity::getId, distinctResIds)
                .list()
                .stream()
                .collect(Collectors.toMap(
                    SysResPageEntity::getId,
                    SysResPageEntity::getPageCode,
                    (left, right) -> left,
                    LinkedHashMap::new
                )));
        } else if (ResourceModelCode.RES_UI_COMPONENT.name().equals(resType)) {
            codeMap.putAll(sysResComponentPersistenceService.lambdaQuery()
                .eq(SysResComponentEntity::getTenantId, tenantId)
                .eq(SysResComponentEntity::getAppCode, appCode)
                .in(SysResComponentEntity::getId, distinctResIds)
                .list()
                .stream()
                .collect(Collectors.toMap(
                    SysResComponentEntity::getId,
                    SysResComponentEntity::getComponentCode,
                    (left, right) -> left,
                    LinkedHashMap::new
                )));
        } else if (ResourceModelCode.RES_API.name().equals(resType)) {
            codeMap.putAll(sysResApiPersistenceService.lambdaQuery()
                .eq(SysResApiEntity::getTenantId, tenantId)
                .eq(SysResApiEntity::getAppCode, appCode)
                .in(SysResApiEntity::getId, distinctResIds)
                .list()
                .stream()
                .collect(Collectors.toMap(
                    SysResApiEntity::getId,
                    SysResApiEntity::getApiCode,
                    (left, right) -> left,
                    LinkedHashMap::new
                )));
        }
        return orderedResIds.stream()
            .map(codeMap::get)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
    }

    private AuthResDerivationPermEntity toEntity(ResourceDerivationPermission binding) {
        AuthResDerivationPermEntity entity = new AuthResDerivationPermEntity();
        if (binding == null) {
            return entity;
        }
        entity.setId(binding.getId());
        entity.setTenantId(binding.getTenantId());
        entity.setAppCode(binding.getAppCode());
        entity.setResType(binding.getResType());
        entity.setResId(binding.getResId());
        entity.setPermItemId(binding.getPermItemId());
        entity.setSortOrder(binding.getSortOrder() == null ? 0 : binding.getSortOrder());
        return entity;
    }

    private ResourceDerivationPermission toDefinition(AuthResDerivationPermEntity entity) {
        if (entity == null) {
            return null;
        }
        return ResourceDerivationPermission.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .resType(entity.getResType())
            .resId(entity.getResId())
            .permItemId(entity.getPermItemId())
            .sortOrder(entity.getSortOrder())
            .build();
    }
}