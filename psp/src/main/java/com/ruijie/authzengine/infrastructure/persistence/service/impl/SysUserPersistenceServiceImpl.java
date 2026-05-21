package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysUserEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysUserMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysUserPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 用户目录持久化服务实现。
 */
@Service
public class SysUserPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysUserMapper, SysUserEntity>
    implements SysUserPersistenceService {
}