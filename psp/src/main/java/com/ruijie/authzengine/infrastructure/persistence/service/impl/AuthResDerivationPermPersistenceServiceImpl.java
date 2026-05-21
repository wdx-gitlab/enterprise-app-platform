package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.AuthResDerivationPermEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.AuthResDerivationPermMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthResDerivationPermPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 派生权限关联持久化服务实现。
 */
@Service
public class AuthResDerivationPermPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<AuthResDerivationPermMapper, AuthResDerivationPermEntity>
    implements AuthResDerivationPermPersistenceService {
}