package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.BoMetaModelEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.BoMetaModelMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.BoMetaModelPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 业务对象元模型持久化服务实现。
 */
@Service
public class BoMetaModelPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<BoMetaModelMapper, BoMetaModelEntity>
    implements BoMetaModelPersistenceService {
}