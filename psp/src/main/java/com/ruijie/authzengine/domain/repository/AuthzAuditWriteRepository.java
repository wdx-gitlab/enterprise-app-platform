package com.ruijie.authzengine.domain.repository;

import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;

/**
 * 鉴权审计写入仓储。
 */
public interface AuthzAuditWriteRepository {

    /**
     * 保存审计记录。
     *
     * @param authzAuditRecord 审计记录
     * @return 已保存审计记录
     */
    AuthzAuditRecord save(AuthzAuditRecord authzAuditRecord);
}