package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.AuthSubjectRelationEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.AuthSubjectRelationMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthSubjectRelationPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 主体关系持久化服务实现。
 */
@Service
public class AuthSubjectRelationPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<AuthSubjectRelationMapper, AuthSubjectRelationEntity>
    implements AuthSubjectRelationPersistenceService {
}