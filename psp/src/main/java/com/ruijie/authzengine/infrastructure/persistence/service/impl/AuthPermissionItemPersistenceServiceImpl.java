package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.AuthPermissionItemEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.AuthPermissionItemMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthPermissionItemPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 权限项持久化服务实现。
 */
@Service
public class AuthPermissionItemPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<AuthPermissionItemMapper, AuthPermissionItemEntity>
    implements AuthPermissionItemPersistenceService {
}