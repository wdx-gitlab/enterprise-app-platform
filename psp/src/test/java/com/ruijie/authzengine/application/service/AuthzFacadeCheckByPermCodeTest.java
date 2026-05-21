package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.DecisionType;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.service.PolicyEnforcementPoint;
import com.ruijie.authzengine.domain.service.PermissionDecisionService;
import com.ruijie.authzengine.domain.service.SubjectExpansionService;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyDecisionPoint;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyEnforcementPoint;
import com.ruijie.authzengine.infrastructure.authz.DefaultPolicyInformationPoint;
import com.ruijie.authzengine.infrastructure.authz.InMemoryAuthorizationPolicyRepository;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthzFacade.checkByPermCode() 单元测试。
 */
class AuthzFacadeCheckByPermCodeTest {

    private AuthzFacade facade;
    private StubPermissionRepository permissionRepository;
    private StubPolicyEnforcementPoint pep;

    @BeforeEach
    void setUp() {
        permissionRepository = new StubPermissionRepository();
        pep = new StubPolicyEnforcementPoint();
        facade = new AuthzFacade(pep, permissionRepository);
    }

    @Test
    void 权限项不存在时返回INDETERMINATE() {
        permissionRepository.item = null;

        AuthzDecision decision = facade.checkByPermCode("T001", "APP", "user1", "not-exist", "");
        assertEquals(DecisionType.INDETERMINATE, decision.getDecision());
        assertTrue(decision.getReason().contains("not-exist"));
    }

    @Test
    void 权限项存在且鉴权通过() {
        permissionRepository.item = AuthPermissionItem.builder()
                .permCode("order:view")
                .resModelCode("MODEL_ORDER")
                .resId("default-res")
                .actCode("VIEW")
                .build();
        pep.decisionToReturn = AuthzDecision.builder().decision(DecisionType.PERMIT).build();

        AuthzDecision decision = facade.checkByPermCode("T001", "APP", "user1", "order:view", "");
        assertEquals(DecisionType.PERMIT, decision.getDecision());
        // 验证 PEP 收到的请求
        assertNotNull(pep.lastRequest);
        assertEquals("user1", pep.lastRequest.getSubject().getId());
        assertEquals("SUB_USER", pep.lastRequest.getSubject().getType());
        assertEquals("MODEL_ORDER", pep.lastRequest.getResource().getResourceType());
        assertEquals("default-res", pep.lastRequest.getResource().getResId());
        assertEquals("VIEW", pep.lastRequest.getAction());
        assertEquals("DENY", pep.lastRequestDecision().getObligations().get("failStrategy"));
    }

    @Test
    void 传入instanceId不覆盖resId而是写入context() {
        permissionRepository.item = AuthPermissionItem.builder()
                .permCode("order:view")
                .resModelCode("MODEL_ORDER")
                .resId("default-res")
                .actCode("VIEW")
                .build();
        pep.decisionToReturn = AuthzDecision.builder().decision(DecisionType.PERMIT).build();

        facade.checkByPermCode("T001", "APP", "user1", "order:view", "instance-123");
        // resId 始终取自权限项配置（authz_bo_meta_model 主键），不被 instanceId 覆盖
        assertEquals("default-res", pep.lastRequest.getResource().getResId());
        // instanceId 写入 context，供 BO Hook 按需使用
        assertNotNull(pep.lastRequest.getContext());
        assertEquals("instance-123", pep.lastRequest.getContext().get("instanceId"));
    }

    @Test
    void instanceId为空时resId取自权限项且context不含instanceId() {
        permissionRepository.item = AuthPermissionItem.builder()
                .permCode("order:view")
                .resModelCode("MODEL_ORDER")
                .resId("item-res-id")
                .actCode("VIEW")
                .build();
        pep.decisionToReturn = AuthzDecision.builder().decision(DecisionType.PERMIT).build();

        facade.checkByPermCode("T001", "APP", "user1", "order:view", "");
        assertEquals("item-res-id", pep.lastRequest.getResource().getResId());
        // 无 instanceId 时 context 中不应包含 instanceId 键
        assertFalse(pep.lastRequest.getContext().containsKey("instanceId"));
    }

    @Test
    void 模型级资源应回落为模型编码且不携带实例ID() {
    permissionRepository.item = AuthPermissionItem.builder()
        .permCode("order:view")
        .resModelCode("MODEL_ORDER")
        .resId("")
        .actCode("VIEW")
        .build();
    pep.decisionToReturn = AuthzDecision.builder().decision(DecisionType.PERMIT).build();

    facade.checkByPermCode("T001", "APP", "user1", "order:view", "");
    assertEquals("", pep.lastRequest.getResource().getResId());
    }

    @Test
    void 应透传权限项失败策略到决策元数据() {
    permissionRepository.item = AuthPermissionItem.builder()
        .permCode("order:view")
        .resModelCode("MODEL_ORDER")
        .resId("")
        .actCode("VIEW")
        .failStrategy("ALLOW")
        .build();
    pep.decisionToReturn = AuthzDecision.builder().decision(DecisionType.INDETERMINATE).build();

    AuthzDecision decision = facade.checkByPermCode("T001", "APP", "user1", "order:view", "");

    assertEquals(DecisionType.INDETERMINATE, decision.getDecision());
    assertEquals("ALLOW", decision.getObligations().get("failStrategy"));
    assertEquals("order:view", decision.getObligations().get("permCode"));
    }

    @Test
    void 真实链路下标准化请求应命中权限() {
    StubPermissionRepository repository = new StubPermissionRepository();
    repository.item = AuthPermissionItem.builder()
        .tenantId("T001")
        .appCode("CRM")
        .permCode("CONTRACT_APPROVE")
        .resModelCode("RES_DATA_BO")
        .resId("")
        .actCode("APPROVE")
        .build();
    DefaultPolicyInformationPoint pip = new DefaultPolicyInformationPoint(new SubjectExpansionService());
    DefaultPolicyDecisionPoint pdp = new DefaultPolicyDecisionPoint(
        pip,
        new InMemoryAuthorizationPolicyRepository(),
        new PermissionDecisionService(null, new com.fasterxml.jackson.databind.ObjectMapper()));
    DefaultPolicyEnforcementPoint realPep = new DefaultPolicyEnforcementPoint(pdp);
    AuthzFacade realFacade = new AuthzFacade(realPep, repository);

    AuthzDecision decision = realFacade.checkByPermCode("T001", "CRM", "demo-user", "CONTRACT_APPROVE", "");

    assertEquals(DecisionType.PERMIT, decision.getDecision());
    assertTrue(decision.getMatchedPermissions().contains("CONTRACT_APPROVE"));
    }

    // ========== 内部桩 ==========

    private static class StubPermissionRepository implements PermissionRepository {
        AuthPermissionItem item;

        @Override
        public AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem) {
            return permissionItem;
        }

        @Override
        public PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return PageResult.<AuthPermissionItem>builder().pageNo(pageNo).pageSize(pageSize).total(0).records(Collections.emptyList()).build();
        }

        @Override
        public AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode) {
            return item;
        }

        @Override
        public void deletePermissionItem(String tenantId, String appCode, String permCode) {
            throw new UnsupportedOperationException("测试桩不支持删除权限项");
        }

        @Override
        public boolean hasPermissionItemReference(String tenantId, String appCode, String permCode) {
            return false;
        }
    }

    private static class StubPolicyEnforcementPoint implements PolicyEnforcementPoint {
        AuthzDecision decisionToReturn = AuthzDecision.builder().decision(DecisionType.PERMIT).build();
        AuthzRequest lastRequest;
        AuthzDecision lastDecision;

        @Override
        public AuthzDecision checkWithGovernance(AuthzRequest request) {
            lastRequest = request;
            lastDecision = decisionToReturn;
            return decisionToReturn;
        }

        private AuthzDecision lastRequestDecision() {
            return lastDecision;
        }
    }
}
