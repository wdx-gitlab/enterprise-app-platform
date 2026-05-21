package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.common.DelegationStatus;
import com.ruijie.authzengine.domain.model.ops.AssignmentDelegate;
import com.ruijie.authzengine.domain.repository.DelegationRepository;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 委托授权应用服务。
 * <p>委托允许授权人将自身权限在时间范围内转备给被委托人。
 * 创建前校验：时间窗口合法性、委托人是否在时点内持有该权限。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelegationAppService {

    private final DelegationRepository delegationRepository;

    /**
     * 创建委托授权。
     *
     * @param assignmentDelegate 委托授权
     * @return 已保存委托授权
     */
    public AssignmentDelegate createDelegation(AssignmentDelegate assignmentDelegate) {
        // 校验 1：生效时间窗口必须合法（start < end）
        validateEffectiveWindow(assignmentDelegate.getStartTime(), assignmentDelegate.getEndTime());
        // 校验 2：委托人在 startTime 时刻必须持有该权限的有效分配
        validateGrantorPermission(assignmentDelegate);
        assignmentDelegate.setStatus(DelegationStatus.ACTIVE.name());
        log.info("[委托服务] 创建委托: tenantId={}, appCode={}, grantorSubjectId={}, delegateSubjectId={}, permCode={}, window=[{}, {}]",
            assignmentDelegate.getTenantId(), assignmentDelegate.getAppCode(),
            assignmentDelegate.getGrantorSubjectId(), assignmentDelegate.getDelegateSubjectId(),
            assignmentDelegate.getPermissionCode(),
            assignmentDelegate.getStartTime(), assignmentDelegate.getEndTime());
        AssignmentDelegate saved = delegationRepository.save(assignmentDelegate);
        log.debug("[委托服务] 委托创建完成: delegationId={}", saved.getDelegationId());
        return saved;
    }

    /**
     * 撤销委托授权。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param delegationId 委托记录标识
     * @return 撤销后的委托授权
     */
    public AssignmentDelegate revokeDelegation(String tenantId, String appCode, Long delegationId) {
        log.info("[委托服务] 撤销委托: tenantId={}, appCode={}, delegationId={}", tenantId, appCode, delegationId);
        return delegationRepository.revoke(tenantId, appCode, delegationId);
    }

    /**
     * 分页查询委托授权记录。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 分页大小
     * @return 分页结果
     */
    public PageResult<AssignmentDelegate> pageDelegations(
        String tenantId,
        String appCode,
        String keyword,
        int pageNo,
        int pageSize
    ) {
        return delegationRepository.pageDelegations(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询委托授权详情。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param delegationId 委托记录标识
     * @return 委托授权详情
     */
    public AssignmentDelegate getDelegation(String tenantId, String appCode, Long delegationId) {
        AssignmentDelegate delegation = delegationRepository.findDelegation(tenantId, appCode, delegationId);
        if (delegation == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "委托记录不存在");
        }
        return delegation;
    }

    /**
     * 查询当前时点有效的委托授权。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param delegateSubjectModel 被委托主体模型
     * @param delegateSubjectId 被委托主体标识
     * @param effectiveAt 判定时点
     * @return 生效委托列表
     */
    public List<AssignmentDelegate> listActiveDelegations(
        String tenantId,
        String appCode,
        String delegateSubjectModel,
        String delegateSubjectId,
        LocalDateTime effectiveAt
    ) {
        return delegationRepository.findActiveDelegations(
            tenantId,
            appCode,
            delegateSubjectModel,
            delegateSubjectId,
            effectiveAt
        );
    }

    /**
     * 查询委托人在指定时点可委托的权限编码列表。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param grantorSubjectModel 委托人主体模型
     * @param grantorSubjectId 委托人主体标识
     * @param effectiveAt 判定时点
     * @return 可委托权限编码列表
     */
    public List<String> listGrantablePermissionCodes(
        String tenantId,
        String appCode,
        String grantorSubjectModel,
        String grantorSubjectId,
        LocalDateTime effectiveAt
    ) {
        return delegationRepository.listGrantablePermissionCodes(
            tenantId,
            appCode,
            grantorSubjectModel,
            grantorSubjectId,
            effectiveAt
        );
    }

    private void validateEffectiveWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "委托失效时间必须晚于生效时间");
        }
    }

    private void validateGrantorPermission(AssignmentDelegate assignmentDelegate) {
        boolean hasGrantPermission = delegationRepository.hasActiveGrantPermission(
            assignmentDelegate.getTenantId(),
            assignmentDelegate.getAppCode(),
            assignmentDelegate.getGrantorSubjectModel(),
            assignmentDelegate.getGrantorSubjectId(),
            assignmentDelegate.getPermissionCode(),
            assignmentDelegate.getStartTime()
        );
        if (!hasGrantPermission) {
            log.warn("[委托服务] 委托人权限校验失败: grantorSubjectId={}, permCode={}, startTime={}",
                assignmentDelegate.getGrantorSubjectId(),
                assignmentDelegate.getPermissionCode(),
                assignmentDelegate.getStartTime());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "委托人未持有可委托的有效权限");
        }
    }
}