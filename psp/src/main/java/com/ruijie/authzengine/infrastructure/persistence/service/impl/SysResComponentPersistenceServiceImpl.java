package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysResComponentEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysResComponentMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResComponentPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 组件资源持久化服务实现。
 */
@Service
public class SysResComponentPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysResComponentMapper, SysResComponentEntity>
    implements SysResComponentPersistenceService {
}