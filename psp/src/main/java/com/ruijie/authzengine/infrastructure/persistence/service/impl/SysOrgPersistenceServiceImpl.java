package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysOrgEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysOrgMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysOrgPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 组织目录持久化服务实现。
 */
@Service
public class SysOrgPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysOrgMapper, SysOrgEntity>
    implements SysOrgPersistenceService {
}