package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthAssignmentEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.SysAuthAssignmentMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthAssignmentPersistenceService;
import org.springframework.stereotype.Service;

/**
 * 授权分配持久化服务实现。
 */
@Service
public class SysAuthAssignmentPersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<SysAuthAssignmentMapper, SysAuthAssignmentEntity>
    implements SysAuthAssignmentPersistenceService {
}