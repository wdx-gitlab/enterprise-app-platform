package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysUserGroupEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysUserGroupMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysUserGroupPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 用户组目录持久化服务实现。
 */
@Service
public class SysUserGroupPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysUserGroupMapper, SysUserGroupEntity>
    implements SysUserGroupPersistenceService {
}