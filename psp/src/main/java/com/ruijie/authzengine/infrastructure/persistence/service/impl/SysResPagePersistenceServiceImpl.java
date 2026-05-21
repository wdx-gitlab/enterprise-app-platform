package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysResPageEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysResPageMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResPagePersistenceService;
import org.springframework.stereotype.Service;

/**
 * 页面资源持久化服务实现。
 */
@Service
public class SysResPagePersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysResPageMapper, SysResPageEntity>
    implements SysResPagePersistenceService {
}