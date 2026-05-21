package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.StandardActionEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.StandardActionMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.StandardActionPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 标准动作持久化服务实现。
 */
@Service
public class StandardActionPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<StandardActionMapper, StandardActionEntity>
    implements StandardActionPersistenceService {
}