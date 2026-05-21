package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthzAuditLogEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysAuthzAuditLogMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthzAuditLogPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 鉴权审计日志持久化服务实现。
 */
@Service
public class SysAuthzAuditLogPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysAuthzAuditLogMapper, SysAuthzAuditLogEntity>
    implements SysAuthzAuditLogPersistenceService {
}