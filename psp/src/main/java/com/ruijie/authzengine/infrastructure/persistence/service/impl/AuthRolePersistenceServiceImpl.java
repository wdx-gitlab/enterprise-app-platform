package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.AuthRoleEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.AuthRoleMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthRolePersistenceService;
import org.springframework.stereotype.Service;

/**
 * 角色目录持久化服务实现。
 */
@Service
public class AuthRolePersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<AuthRoleMapper, AuthRoleEntity>
    implements AuthRolePersistenceService {
}