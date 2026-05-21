package com.ruijie.authzengine.domain.repository;

import com.ruijie.authzengine.domain.model.ops.AuthzAuditPage;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditQuery;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;

/**
 * 鉴权审计查询仓储。
 */
public interface AuthzAuditRepository {

    /**
     * 按条件分页查询审计记录。
     *
     * @param query 查询条件
     * @return 审计分页结果
     */
    AuthzAuditPage query(AuthzAuditQuery query);

    /**
     * 查询审计日志详情。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param auditLogId 审计日志标识
     * @return 审计记录，不存在返回 null
     */
    AuthzAuditRecord findById(String tenantId, String appCode, Long auditLogId);
}