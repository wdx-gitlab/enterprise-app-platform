package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.ruijie.authzengine.infrastructure.persistence.entity.StandardPolicyTemplateEntity;
import com.ruijie.authzengine.infrastructure.persistence.mapper.StandardPolicyTemplateMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.StandardPolicyTemplatePersistenceService;
import org.springframework.stereotype.Service;

/**
 * 标准策略模板持久化服务实现。
 */
@Service
public class StandardPolicyTemplatePersistenceServiceImpl
    extends AuthzPersistenceServiceImpl<StandardPolicyTemplateMapper, StandardPolicyTemplateEntity>
    implements StandardPolicyTemplatePersistenceService {
}