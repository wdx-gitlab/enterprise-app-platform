package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.common.DelegationStatus;
import com.ruijie.authzengine.domain.model.ops.AssignmentDelegate;
import com.ruijie.authzengine.domain.repository.DelegationRepository;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthPermissionItemEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAssignmentDelegateEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthAssignmentEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthPermissionItemPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAssignmentDelegatePersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthAssignmentPersistenceService;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 委托授权仓储实现。
 * <p>委托允许授权人（grantor）将自身拥有的某项权限在时间范围内转备给被委托人（delegatee）。
 * 委托记录有生效期，仅 ACTIVE 状态且在时间范围内的委托才会被鉴权引擎识别。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabaseDelegationRepository implements DelegationRepository {

    private final SysAssignmentDelegatePersistenceService sysAssignmentDelegatePersistenceService;

    private final AuthPermissionItemPersistenceService authPermissionItemPersistenceService;

    private final SysAuthAssignmentPersistenceService sysAuthAssignmentPersistenceService;

    /**
     * 检查授权人在指定时刻是否拥有有效的某项权限分配（用于委托前置校验）。
     * <p>算法：先源权限项 -> 再查 grantorSubject 在 effectiveAt 时刻是否有授权分配。</p>
     */
    @Override
    public boolean hasActiveGrantPermission(
        String tenantId,
        String appCode,
        String grantorSubjectModel,
        String grantorSubjectId,
        String permissionCode,
        LocalDateTime effectiveAt
    ) {
        if (effectiveAt == null) {
            log.debug("[委托仓储] effectiveAt 为空，直接返回 false");
            return false;
        }
        // 定位权限项
        AuthPermissionItemEntity permissionItemEntity = authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .eq(AuthPermissionItemEntity::getPermCode, permissionCode)
            .one();
        if (permissionItemEntity == null) {
            log.warn("[委托仓储] 权限项不存在无法校验授权: permCode={}", permissionCode);
            return false;
        }
        // 检查授权人在 effectiveAt 时刻是否有未过期的分配
        boolean hasGrant = sysAuthAssignmentPersistenceService.lambdaQuery()
            .eq(SysAuthAssignmentEntity::getTenantId, tenantId)
            .eq(SysAuthAssignmentEntity::getAppCode, appCode)
            .eq(SysAuthAssignmentEntity::getSubjectModel, grantorSubjectModel)
            .eq(SysAuthAssignmentEntity::getSubjectId, grantorSubjectId)
            .eq(SysAuthAssignmentEntity::getPermItemId, permissionItemEntity.getId())
            .and(wrapper -> wrapper.isNull(SysAuthAssignmentEntity::getExpireTime)
                .or()
                .gt(SysAuthAssignmentEntity::getExpireTime, effectiveAt))
            .count() > 0;
        log.debug("[委托仓储] 授权人权限校验: grantorSubjectModel={}, grantorSubjectId={}, permCode={}, hasGrant={}",
            grantorSubjectModel, grantorSubjectId, permissionCode, hasGrant);
        return hasGrant;
    }

    /**
     * 查询委托人在指定时点可委托的权限编码列表。
     */
    @Override
    public List<String> listGrantablePermissionCodes(
        String tenantId,
        String appCode,
        String grantorSubjectModel,
        String grantorSubjectId,
        LocalDateTime effectiveAt
    ) {
        if (!StringUtils.hasText(grantorSubjectModel)
            || !StringUtils.hasText(grantorSubjectId)
            || effectiveAt == null) {
            return Collections.emptyList();
        }
        List<Long> permItemIds = sysAuthAssignmentPersistenceService.lambdaQuery()
            .eq(SysAuthAssignmentEntity::getTenantId, tenantId)
            .eq(SysAuthAssignmentEntity::getAppCode, appCode)
            .eq(SysAuthAssignmentEntity::getSubjectModel, grantorSubjectModel)
            .eq(SysAuthAssignmentEntity::getSubjectId, grantorSubjectId)
            .and(wrapper -> wrapper.isNull(SysAuthAssignmentEntity::getExpireTime)
                .or()
                .gt(SysAuthAssignmentEntity::getExpireTime, effectiveAt))
            .orderByAsc(SysAuthAssignmentEntity::getId)
            .list()
            .stream()
            .map(SysAuthAssignmentEntity::getPermItemId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (permItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> permCodeById = authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .in(AuthPermissionItemEntity::getId, permItemIds)
            .list()
            .stream()
            .filter(entity -> StringUtils.hasText(entity.getPermCode()))
            .collect(Collectors.toMap(AuthPermissionItemEntity::getId, AuthPermissionItemEntity::getPermCode, (left, right) -> left));
        List<String> permissionCodes = permItemIds.stream()
            .map(permCodeById::get)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
        log.debug("[委托仓储] 查询可委托权限完成: grantorSubjectModel={}, grantorSubjectId={}, effectiveAt={}, count={}",
            grantorSubjectModel, grantorSubjectId, effectiveAt, permissionCodes.size());
        return permissionCodes;
    }

    /**
     * 创建新的委托记录。
     * <p>保存前先校验权限项是否存在（不存在报业务异常），再将 permCode 转换为内部 permItemId 写入。</p>
     */
    @Override
    public AssignmentDelegate save(AssignmentDelegate assignmentDelegate) {
        log.info("[委托仓储] 创建委托: tenantId={}, appCode={}, grantorSubjectId={}, delegateSubjectId={}, permCode={}, startTime={}, endTime={}",
            assignmentDelegate.getTenantId(), assignmentDelegate.getAppCode(),
            assignmentDelegate.getGrantorSubjectId(), assignmentDelegate.getDelegateSubjectId(),
            assignmentDelegate.getPermissionCode(), assignmentDelegate.getStartTime(), assignmentDelegate.getEndTime());
        AuthPermissionItemEntity permissionItemEntity = requirePermissionItem(
            assignmentDelegate.getTenantId(),
            assignmentDelegate.getAppCode(),
            assignmentDelegate.getPermissionCode()
        );
        SysAssignmentDelegateEntity entity = toEntity(assignmentDelegate, permissionItemEntity.getId());
        sysAssignmentDelegatePersistenceService.save(entity);
        log.info("[委托仓储] 委托创建完成: delegationId={}", entity.getId());
        return toDefinition(entity, permissionItemEntity.getPermCode());
    }

    /**
     * 撤销委托：将委托记录状态设为 REVOKED。
     * <p>委托记录不存在时抛出业务异常。</p>
     */
    @Override
    public AssignmentDelegate revoke(String tenantId, String appCode, Long delegationId) {
        log.info("[委托仓储] 撤销委托: tenantId={}, appCode={}, delegationId={}", tenantId, appCode, delegationId);
        SysAssignmentDelegateEntity entity = sysAssignmentDelegatePersistenceService.lambdaQuery()
            .eq(SysAssignmentDelegateEntity::getTenantId, tenantId)
            .eq(SysAssignmentDelegateEntity::getAppCode, appCode)
            .eq(SysAssignmentDelegateEntity::getId, delegationId)
            .one();
        if (entity == null) {
            log.warn("[委托仓储] 委托记录不存在: delegationId={}", delegationId);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "委托记录不存在");
        }
        entity.setStatus(DelegationStatus.REVOKED.name());
        sysAssignmentDelegatePersistenceService.updateById(entity);
        log.info("[委托仓储] 委托已撤销: delegationId={}", delegationId);
        return toDefinition(entity, resolvePermissionCode(entity.getPermItemId()));
    }

    @Override
    public PageResult<AssignmentDelegate> pageDelegations(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<AssignmentDelegate> allRecords = sysAssignmentDelegatePersistenceService.lambdaQuery()
            .eq(SysAssignmentDelegateEntity::getTenantId, tenantId)
            .eq(SysAssignmentDelegateEntity::getAppCode, appCode)
            .and(StringUtils.hasText(keyword), wrapper -> wrapper
                .like(SysAssignmentDelegateEntity::getGrantorSubjectId, keyword)
                .or()
                .like(SysAssignmentDelegateEntity::getDelegateSubjectId, keyword)
                .or()
                .like(SysAssignmentDelegateEntity::getReason, keyword))
            .orderByAsc(SysAssignmentDelegateEntity::getId)
            .list()
            .stream()
            .map(entity -> toDefinition(entity, resolvePermissionCode(entity.getPermItemId())))
            .collect(Collectors.toList());

        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : pageSize;
        int fromIndex = (normalizedPageNo - 1) * normalizedPageSize;
        List<AssignmentDelegate> pageRecords;
        if (fromIndex >= allRecords.size()) {
            pageRecords = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + normalizedPageSize, allRecords.size());
            pageRecords = allRecords.subList(fromIndex, toIndex);
        }

        return PageResult.<AssignmentDelegate>builder()
            .pageNo(normalizedPageNo)
            .pageSize(normalizedPageSize)
            .total(allRecords.size())
            .records(pageRecords)
            .build();
    }

    @Override
    public AssignmentDelegate findDelegation(String tenantId, String appCode, Long delegationId) {
        SysAssignmentDelegateEntity entity = sysAssignmentDelegatePersistenceService.lambdaQuery()
            .eq(SysAssignmentDelegateEntity::getTenantId, tenantId)
            .eq(SysAssignmentDelegateEntity::getAppCode, appCode)
            .eq(SysAssignmentDelegateEntity::getId, delegationId)
            .one();
        return entity == null ? null : toDefinition(entity, resolvePermissionCode(entity.getPermItemId()));
    }

    /**
     * 查询指定主体在指定时刻的所有有效委托列表。
     * <p>有效条件：startTime <= effectiveAt <= endTime 且状态为 ACTIVE。</p>
     */
    @Override
    public List<AssignmentDelegate> findActiveDelegations(
        String tenantId,
        String appCode,
        String delegateSubjectModel,
        String delegateSubjectId,
        LocalDateTime effectiveAt
    ) {
        if (effectiveAt == null) {
            log.debug("[委托仓储] effectiveAt 为空，返回空活跃委托列表");
            return Collections.emptyList();
        }
        log.debug("[委托仓储] 查询活跃委托: delegateSubjectModel={}, delegateSubjectId={}, effectiveAt={}",
            delegateSubjectModel, delegateSubjectId, effectiveAt);
        List<AssignmentDelegate> result = sysAssignmentDelegatePersistenceService.lambdaQuery()
            .eq(SysAssignmentDelegateEntity::getTenantId, tenantId)
            .eq(SysAssignmentDelegateEntity::getAppCode, appCode)
            .eq(SysAssignmentDelegateEntity::getDelegateSubjectModel, delegateSubjectModel)
            .eq(SysAssignmentDelegateEntity::getDelegateSubjectId, delegateSubjectId)
            .eq(SysAssignmentDelegateEntity::getStatus, DelegationStatus.ACTIVE.name())
            .le(SysAssignmentDelegateEntity::getStartTime, effectiveAt)
            .ge(SysAssignmentDelegateEntity::getEndTime, effectiveAt)
            .orderByAsc(SysAssignmentDelegateEntity::getId)
            .list()
            .stream()
            .map(entity -> toDefinition(entity, resolvePermissionCode(entity.getPermItemId())))
            .collect(Collectors.toList());
        log.debug("[委托仓储] 活跃委托查询完成: found={}", result.size());
        return result;
    }

    private AuthPermissionItemEntity requirePermissionItem(String tenantId, String appCode, String permissionCode) {
        AuthPermissionItemEntity permissionItemEntity = authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .eq(AuthPermissionItemEntity::getPermCode, permissionCode)
            .one();
        if (permissionItemEntity == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "权限项不存在");
        }
        return permissionItemEntity;
    }

    /**
     * 此方法每次调用都会发起单次 DB 查询。
     * 若需批量解析，可考虑在上层缓存 permItemId -> permCode。
     */
    private String resolvePermissionCode(Long permItemId) {
        if (permItemId == null) {
            return null;
        }
        AuthPermissionItemEntity permissionItemEntity = authPermissionItemPersistenceService.getById(permItemId);
        return permissionItemEntity == null ? null : permissionItemEntity.getPermCode();
    }

    private SysAssignmentDelegateEntity toEntity(AssignmentDelegate assignmentDelegate, Long permItemId) {
        SysAssignmentDelegateEntity entity = new SysAssignmentDelegateEntity();
        entity.setTenantId(assignmentDelegate.getTenantId());
        entity.setAppCode(assignmentDelegate.getAppCode());
        entity.setGrantorSubjectModel(assignmentDelegate.getGrantorSubjectModel());
        entity.setGrantorSubjectId(assignmentDelegate.getGrantorSubjectId());
        entity.setDelegateSubjectModel(assignmentDelegate.getDelegateSubjectModel());
        entity.setDelegateSubjectId(assignmentDelegate.getDelegateSubjectId());
        entity.setPermItemId(permItemId);
        entity.setStartTime(assignmentDelegate.getStartTime());
        entity.setEndTime(assignmentDelegate.getEndTime());
        entity.setStatus(assignmentDelegate.getStatus());
        entity.setReason(assignmentDelegate.getReason());
        return entity;
    }

    private AssignmentDelegate toDefinition(SysAssignmentDelegateEntity entity, String permissionCode) {
        return AssignmentDelegate.builder()
            .delegationId(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .grantorSubjectModel(entity.getGrantorSubjectModel())
            .grantorSubjectId(entity.getGrantorSubjectId())
            .delegateSubjectModel(entity.getDelegateSubjectModel())
            .delegateSubjectId(entity.getDelegateSubjectId())
            .permissionCode(permissionCode)
            .startTime(entity.getStartTime())
            .endTime(entity.getEndTime())
            .status(entity.getStatus())
            .reason(entity.getReason())
            .build();
    }
}