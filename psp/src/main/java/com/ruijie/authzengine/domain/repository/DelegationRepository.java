package com.ruijie.authzengine.domain.repository;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.ops.AssignmentDelegate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 委托授权仓储。
 */
public interface DelegationRepository {

    /**
     * 校验委托人是否持有可委托的有效权限。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param grantorSubjectModel 委托人主体模型
     * @param grantorSubjectId 委托人主体标识
     * @param permissionCode 权限项编码
     * @param effectiveAt 校验时点
     * @return 是否持有有效权限
     */
    boolean hasActiveGrantPermission(
        String tenantId,
        String appCode,
        String grantorSubjectModel,
        String grantorSubjectId,
        String permissionCode,
        LocalDateTime effectiveAt
    );

    /**
     * 查询委托人在指定时点可委托的权限编码列表。
     *
     * <p>当前口径仅包含 authz_assignment 中对 grantor 的有效直接分配，
     * 不展开角色、组织或已有委托带来的运行时最终权限。</p>
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param grantorSubjectModel 委托人主体模型
     * @param grantorSubjectId 委托人主体标识
     * @param effectiveAt 判定时点
     * @return 可委托权限编码列表
     */
    List<String> listGrantablePermissionCodes(
        String tenantId,
        String appCode,
        String grantorSubjectModel,
        String grantorSubjectId,
        LocalDateTime effectiveAt
    );

    /**
     * 创建委托授权。
     *
     * @param assignmentDelegate 委托授权
     * @return 已保存委托授权
     */
    AssignmentDelegate save(AssignmentDelegate assignmentDelegate);

    /**
     * 撤销委托授权。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param delegationId 委托记录标识
     * @return 撤销后的委托授权
     */
    AssignmentDelegate revoke(String tenantId, String appCode, Long delegationId);

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
    PageResult<AssignmentDelegate> pageDelegations(String tenantId, String appCode, String keyword, int pageNo, int pageSize);

    /**
     * 查询委托授权详情。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param delegationId 委托记录标识
     * @return 委托授权详情，不存在返回 null
     */
    AssignmentDelegate findDelegation(String tenantId, String appCode, Long delegationId);

    /**
     * 查询指定时点仍然有效的委托授权。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param delegateSubjectModel 被委托主体模型
     * @param delegateSubjectId 被委托主体标识
     * @param effectiveAt 判定时点
     * @return 生效中的委托授权列表
     */
    List<AssignmentDelegate> findActiveDelegations(
        String tenantId,
        String appCode,
        String delegateSubjectModel,
        String delegateSubjectId,
        LocalDateTime effectiveAt
    );
}