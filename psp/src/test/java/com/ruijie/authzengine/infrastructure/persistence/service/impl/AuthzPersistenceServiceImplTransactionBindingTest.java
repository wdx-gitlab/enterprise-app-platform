package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * 校验 authz 持久化实现统一绑定专属事务管理器，避免与宿主事务管理器冲突。
 */
class AuthzPersistenceServiceImplTransactionBindingTest {

    @Test
    void shouldBindAuthzBaseSaveOrUpdateMethodToAuthzTransactionManager() throws NoSuchMethodException {
        Method method = AuthzPersistenceServiceImpl.class.getMethod("saveOrUpdate", Object.class);
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
        assertNotNull(transactional, "AuthzPersistenceServiceImpl.saveOrUpdate 应声明事务注解");
        assertEquals("authzTransactionManager", transactional.transactionManager(),
            "AuthzPersistenceServiceImpl.saveOrUpdate 必须绑定 authzTransactionManager");
    }

    @Test
    void shouldBindAllAuthzPersistenceServicesToAuthzTransactionManager() {
        List<Class<?>> serviceTypes = Arrays.asList(
            AuthMetaModelPersistenceServiceImpl.class,
            AuthPermissionItemPersistenceServiceImpl.class,
            AuthRolePersistenceServiceImpl.class,
            AuthSubjectRelationPersistenceServiceImpl.class,
            BoMetaModelPersistenceServiceImpl.class,
            StandardActionPersistenceServiceImpl.class,
            StandardPolicyTemplatePersistenceServiceImpl.class,
            SysAssignmentDelegatePersistenceServiceImpl.class,
            SysAuthAssignmentPersistenceServiceImpl.class,
            SysAuthzAuditLogPersistenceServiceImpl.class,
            SysOrgPersistenceServiceImpl.class,
            SysPositionPersistenceServiceImpl.class,
            SysResApiPersistenceServiceImpl.class,
            SysResComponentPersistenceServiceImpl.class,
            SysResMenuPersistenceServiceImpl.class,
            SysResPagePersistenceServiceImpl.class,
            SysUserGroupPersistenceServiceImpl.class,
            SysUserPersistenceServiceImpl.class
        );

        for (Class<?> serviceType : serviceTypes) {
            Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(serviceType, Transactional.class);
            assertNotNull(transactional, serviceType.getSimpleName() + " 应声明事务注解");
            assertEquals("authzTransactionManager", transactional.transactionManager(),
                serviceType.getSimpleName() + " 必须绑定 authzTransactionManager");
        }
    }
}