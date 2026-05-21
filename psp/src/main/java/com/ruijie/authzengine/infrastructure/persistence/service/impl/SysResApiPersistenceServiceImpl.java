package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysResApiEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysResApiMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResApiPersistenceService;
import org.springframework.stereotype.Service;

/**
 * API 资源目录持久化服务实现。
 */
@Service
public class SysResApiPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysResApiMapper, SysResApiEntity>
    implements SysResApiPersistenceService {
}