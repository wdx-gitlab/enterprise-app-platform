package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysAssignmentDelegateEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysAssignmentDelegateMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAssignmentDelegatePersistenceService;
import org.springframework.stereotype.Service;

/**
 * 委托授权持久化服务实现。
 */
@Service
public class SysAssignmentDelegatePersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysAssignmentDelegateMapper, SysAssignmentDelegateEntity>
    implements SysAssignmentDelegatePersistenceService {
}