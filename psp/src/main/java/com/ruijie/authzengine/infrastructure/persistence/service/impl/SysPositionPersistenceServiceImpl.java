package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysPositionEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysPositionMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysPositionPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 岗位目录持久化服务实现。
 */
@Service
public class SysPositionPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysPositionMapper, SysPositionEntity>
    implements SysPositionPersistenceService {
}