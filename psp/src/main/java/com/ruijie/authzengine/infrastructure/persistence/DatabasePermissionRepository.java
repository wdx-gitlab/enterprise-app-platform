package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthPermissionItemEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.AuthPermissionItemMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAssignmentDelegateEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthAssignmentEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthPermissionItemPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAssignmentDelegatePersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthAssignmentPersistenceService;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 权限项治理仓储实现。
 * <p>权限项（PermissionItem）是授权分配的核心参照对象，又被授权分配和委托记录引用，删除前必须检查引用关系。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabasePermissionRepository implements PermissionRepository {

    private final AuthPermissionItemPersistenceService authPermissionItemPersistenceService;

    private final AuthPermissionItemMapper authPermissionItemMapper;

    private final SysAuthAssignmentPersistenceService sysAuthAssignmentPersistenceService;

    private final SysAssignmentDelegatePersistenceService sysAssignmentDelegatePersistenceService;

    private final DerivationPermissionRepository derivationPermissionRepository;

    /**
     * 保存权限项：按租户+应用+permCode 判断是否已存在，存在则更新，不存在则新建。
     * <p>
     * 查找顺序：
     * 1. 按 permCode 查活跃记录（最优先，permCode 相同直接覆盖）
     * 2. 按自然唯一键 (tenantId+appCode+resModelCode+resId+actCode) 查活跃记录
     *    （兜底：permCode 变更后同一资源仍可 UPDATE，避免唯一约束冲突）
     * 3. 按以上两种方式查软删除记录（恢复）
     * 4. 以上均未找到：INSERT
     * </p>
     */
    @Override
    public AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem) {
        // 1. 按 permCode 查活跃记录
        AuthPermissionItemEntity existing = authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, permissionItem.getTenantId())
            .eq(AuthPermissionItemEntity::getAppCode, permissionItem.getAppCode())
            .eq(AuthPermissionItemEntity::getPermCode, permissionItem.getPermCode())
            .one();

        // 2. permCode 未命中时，按自然唯一键查活跃记录（防止 permCode 变更导致重复插入）
        if (existing == null && StringUtils.hasText(permissionItem.getResId())) {
            existing = authPermissionItemPersistenceService.lambdaQuery()
                .eq(AuthPermissionItemEntity::getTenantId, permissionItem.getTenantId())
                .eq(AuthPermissionItemEntity::getAppCode, permissionItem.getAppCode())
                .eq(AuthPermissionItemEntity::getResModelCode, permissionItem.getResModelCode())
                .eq(AuthPermissionItemEntity::getResId, permissionItem.getResId())
                .eq(AuthPermissionItemEntity::getActCode, permissionItem.getActCode())
                .one();
        }

        AuthPermissionItemEntity entity = toEntity(permissionItem);
        boolean isUpdate = existing != null;
        boolean isRestore = false;
        if (isUpdate) {
            entity.setId(existing.getId());
        } else {
            AuthPermissionItemEntity deleted = findRestorablePermissionItem(permissionItem);
            if (deleted != null) {
                entity.setId(deleted.getId());
                authPermissionItemMapper.reviveById(entity);
                isRestore = true;
            }
        }
        log.info("[权限项仓储] {}权限项: tenantId={}, appCode={}, permCode={}, actCode={}",
            isUpdate ? "更新" : (isRestore ? "恢复" : "新增"),
            permissionItem.getTenantId(), permissionItem.getAppCode(),
            permissionItem.getPermCode(), permissionItem.getActCode());
        if (!isRestore) {
            // 明确使用 updateById / save，避免 saveOrUpdate 内部二次 getById 在某些场景选错分支
            if (isUpdate) {
                authPermissionItemPersistenceService.updateById(entity);
            } else {
                authPermissionItemPersistenceService.save(entity);
            }
        }
        log.debug("[权限项仓储] 权限项保存完成: id={}", entity.getId());
        return toDefinition(entity);
    }

    /**
     * 分页查询权限项，支持按 permCode / resId / actCode 模糊匹配。
     */
    @Override
    public PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return pagePermissionItems(tenantId, appCode, keyword, null, null, pageNo, pageSize);
    }

    /**
     * 分页查询权限项，支持按资源模型编码、资源标识和关键词过滤。
     */
    @Override
    public PageResult<AuthPermissionItem> pagePermissionItems(
            String tenantId,
            String appCode,
            String keyword,
            String resModelCode,
            String resId,
            int pageNo,
            int pageSize) {
        String normalizedResModelCode = StringUtils.hasText(resModelCode)
            ? resModelCode.trim().toUpperCase(Locale.ROOT)
            : null;
        String normalizedResId = StringUtils.hasText(resId) ? resId.trim() : null;
        log.debug("[权限项仓储] 分页查询权限项: tenantId={}, appCode={}, keyword={}, pageNo={}, pageSize={}",
            tenantId, appCode, keyword, pageNo, pageSize);
        List<AuthPermissionItem> records = authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .eq(StringUtils.hasText(normalizedResModelCode), AuthPermissionItemEntity::getResModelCode, normalizedResModelCode)
            .eq(StringUtils.hasText(normalizedResId), AuthPermissionItemEntity::getResId, normalizedResId)
            .and(StringUtils.hasText(keyword), wrapper -> wrapper
                .like(AuthPermissionItemEntity::getPermCode, keyword)
                .or()
                .like(AuthPermissionItemEntity::getResId, keyword)
                .or()
                .like(AuthPermissionItemEntity::getActCode, keyword))
            .orderByAsc(AuthPermissionItemEntity::getPermCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
        log.debug("[权限项仓储] 权限项查询结果: total={}", records.size());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode) {
        AuthPermissionItemEntity entity = authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .eq(AuthPermissionItemEntity::getPermCode, permCode)
            .one();
        return entity == null ? null : toDefinition(entity);
    }

    /**
     * 删除权限项，若记录不存在则静默跳过。
     */
    @Override
    public void deletePermissionItem(String tenantId, String appCode, String permCode) {
        log.info("[权限项仓储] 删除权限项: tenantId={}, appCode={}, permCode={}", tenantId, appCode, permCode);
        AuthPermissionItemEntity entity = authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .eq(AuthPermissionItemEntity::getPermCode, permCode)
            .one();
        if (entity != null) {
            derivationPermissionRepository.deleteBindingsByPermissionItemId(tenantId, appCode, entity.getId());
            authPermissionItemPersistenceService.removeById(entity.getId());
            log.info("[权限项仓储] 权限项已删除: id={}, permCode={}", entity.getId(), permCode);
        } else {
            log.warn("[权限项仓储] 待删除的权限项不存在，跳过: permCode={}", permCode);
        }
    }

    /**
     * 检查权限项是否被引用。
     * <p>分别检查授权分配表（authz_assignment）和委托表（authz_assignment_delegate），
     * 任一被引用则返回 true，用于防止误删。</p>
     */
    @Override
    public boolean hasPermissionItemReference(String tenantId, String appCode, String permCode) {
        AuthPermissionItemEntity entity = authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .eq(AuthPermissionItemEntity::getPermCode, permCode)
            .one();
        if (entity == null) {
            return false;
        }
        Long permItemId = entity.getId();
        // 检查分配表是否引用该权限项
        boolean assignmentRef = sysAuthAssignmentPersistenceService.lambdaQuery()
            .eq(SysAuthAssignmentEntity::getTenantId, tenantId)
            .eq(SysAuthAssignmentEntity::getAppCode, appCode)
            .eq(SysAuthAssignmentEntity::getPermItemId, permItemId)
            .count() > 0;
        // 检查委托表是否引用该权限项
        boolean delegateRef = sysAssignmentDelegatePersistenceService.lambdaQuery()
            .eq(SysAssignmentDelegateEntity::getTenantId, tenantId)
            .eq(SysAssignmentDelegateEntity::getAppCode, appCode)
            .eq(SysAssignmentDelegateEntity::getPermItemId, permItemId)
            .count() > 0;
        boolean hasRef = assignmentRef || delegateRef;
        log.debug("[权限项仓储] 引用检查: permCode={}, assignmentRef={}, delegateRef={}, result={}",
            permCode, assignmentRef, delegateRef, hasRef);
        return hasRef;
    }

    /**
     * 按主键 ID 列表批量查询权限项。
     * Q1/Q4 通过授权记录拿到 permItemId 后，批量反查权限项以获取 permCode 列表。
     */
    @Override
    public List<AuthPermissionItem> findPermissionItemsByIds(
            String tenantId, String appCode, List<Long> permItemIds) {
        if (permItemIds == null || permItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .in(AuthPermissionItemEntity::getId, permItemIds)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    /**
     * 按资源模型编码查询权限项列表。
     * Q2 先按 resModelCode 拿到候选权限项，再与主体授权记录取交集，得出可访问资源编码列表。
     */
    @Override
    public List<AuthPermissionItem> findPermissionItemsByResModelCode(
            String tenantId, String appCode, String resModelCode) {
        if (resModelCode == null || resModelCode.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .eq(AuthPermissionItemEntity::getResModelCode, resModelCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    private AuthPermissionItemEntity toEntity(AuthPermissionItem permissionItem) {
        AuthPermissionItemEntity entity = new AuthPermissionItemEntity();
        entity.setTenantId(permissionItem.getTenantId());
        entity.setAppCode(permissionItem.getAppCode());
        entity.setPermCode(permissionItem.getPermCode());
        entity.setResModelCode(permissionItem.getResModelCode());
        entity.setResId(permissionItem.getResId());
        entity.setActCode(permissionItem.getActCode());
        entity.setFailStrategy(permissionItem.getFailStrategy());
        return entity;
    }

    private AuthPermissionItemEntity findRestorablePermissionItem(AuthPermissionItem permissionItem) {
        AuthPermissionItemEntity deleted = authPermissionItemMapper.findDeletedByPermCode(
            permissionItem.getTenantId(),
            permissionItem.getAppCode(),
            permissionItem.getPermCode()
        );
        if (deleted != null) {
            return deleted;
        }
        return authPermissionItemMapper.findDeletedByUniqueKey(
            permissionItem.getTenantId(),
            permissionItem.getAppCode(),
            permissionItem.getResModelCode(),
            permissionItem.getResId(),
            permissionItem.getActCode()
        );
    }

    private AuthPermissionItem toDefinition(AuthPermissionItemEntity entity) {
        return AuthPermissionItem.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .permCode(entity.getPermCode())
            .resModelCode(entity.getResModelCode())
            .resId(entity.getResId())
            .actCode(entity.getActCode())
            .failStrategy(entity.getFailStrategy())
            .build();
    }

    private <T> PageResult<T> buildPage(List<T> records, int pageNo, int pageSize) {
        List<T> safeRecords = records == null ? Collections.emptyList() : records;
        int safePageNo = pageNo <= 0 ? 1 : pageNo;
        int safePageSize = pageSize <= 0 ? 20 : pageSize;
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
