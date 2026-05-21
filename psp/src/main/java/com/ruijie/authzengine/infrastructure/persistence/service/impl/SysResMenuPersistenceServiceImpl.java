package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysResMenuEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysResMenuMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResMenuPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 菜单资源持久化服务实现。
 */
@Service
public class SysResMenuPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysResMenuMapper, SysResMenuEntity>
    implements SysResMenuPersistenceService {
}