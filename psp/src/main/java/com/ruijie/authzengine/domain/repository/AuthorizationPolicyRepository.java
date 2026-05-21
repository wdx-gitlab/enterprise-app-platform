package com.ruijie.authzengine.domain.repository;

import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.PermissionGrant;
import java.util.List;
import java.util.Set;

/**
 * 授权策略仓储抽象。
 */
public interface AuthorizationPolicyRepository {

    /**
     * 按主体集合查询最小授权记录。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param subjectKeys 主体集合
     * @return 命中的授权记录
     */
    List<PermissionGrant> findBySubjects(String tenantId, String appCode, Set<SubjectKey> subjectKeys);

    /**
     * 按主体集合和治理上下文查询授权记录。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param subjectKeys 主体集合
     * @param context 治理上下文
     * @return 命中的授权记录
     */
    default List<PermissionGrant> findBySubjects(
        String tenantId,
        String appCode,
        Set<SubjectKey> subjectKeys,
        AuthzContext context
    ) {
        return findBySubjects(tenantId, appCode, subjectKeys);
    }
}