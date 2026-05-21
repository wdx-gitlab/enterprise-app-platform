package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.PermissionGrant;
import com.ruijie.authzengine.domain.repository.AuthorizationPolicyRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * standalone 测试使用的内存授权仓储，不参与 Spring 容器装配。
 */
public class InMemoryAuthorizationPolicyRepository implements AuthorizationPolicyRepository {

    private final List<PermissionGrant> grants = Collections.unmodifiableList(Arrays.asList(
        PermissionGrant.builder()
            .assignmentId(1L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_USER")
            .subjectId("demo-user")
            .resourceType("RES_DATA_BO")
            .resId("")
            .action("APPROVE")
            .permissionCode("CONTRACT_APPROVE")
            .build(),
        PermissionGrant.builder()
            .assignmentId(2L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_ROLE")
            .subjectId("contract-admin")
            .resourceType("RES_DATA_BO")
            .resId("")
            .action("READ")
            .permissionCode("CONTRACT_READ")
            .build(),
        PermissionGrant.builder()
            .assignmentId(3L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectType("SUB_ROLE")
            .subjectId("contract-admin")
            .resourceType("RES_DATA_BO")
            .resId("")
            .action("APPROVE")
            .permissionCode("CONTRACT_APPROVE_BY_ROLE")
            .build()
    ));

    @Override
    public List<PermissionGrant> findBySubjects(String tenantId, String appCode, Set<SubjectKey> subjectKeys) {
        return grants.stream()
            .filter(grant -> grant.getTenantId().equals(tenantId))
            .filter(grant -> grant.getAppCode().equals(appCode))
            .filter(grant -> subjectKeys.stream().anyMatch(subjectKey ->
                grant.getSubjectType().equals(subjectKey.getSubjectType())
                    && grant.getSubjectId().equals(subjectKey.getSubjectId())))
            .collect(Collectors.toList());
    }
}