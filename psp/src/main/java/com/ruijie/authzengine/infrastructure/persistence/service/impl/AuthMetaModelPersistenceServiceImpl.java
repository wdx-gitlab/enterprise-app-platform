package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.AuthMetaModelEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.AuthMetaModelMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthMetaModelPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 权限元模型持久化服务实现。
 */
@Service
public class AuthMetaModelPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<AuthMetaModelMapper, AuthMetaModelEntity>
    implements AuthMetaModelPersistenceService {
}