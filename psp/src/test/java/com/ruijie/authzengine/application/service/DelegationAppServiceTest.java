package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.ops.AssignmentDelegate;
import com.ruijie.authzengine.domain.repository.DelegationRepository;
import com.ruijie.authzengine.shared.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DelegationAppServiceTest {

    @Test
    void shouldCreateDelegationAsActive() {
        DelegationAppService delegationAppService = new DelegationAppService(new InMemoryDelegationRepository());

        AssignmentDelegate assignmentDelegate = delegationAppService.createDelegation(buildDelegate(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2)
        ));

        Assertions.assertNotNull(assignmentDelegate.getDelegationId());
        Assertions.assertEquals("ACTIVE", assignmentDelegate.getStatus());
    }

    @Test
    void shouldRevokeDelegation() {
        InMemoryDelegationRepository delegationRepository = new InMemoryDelegationRepository();
        DelegationAppService delegationAppService = new DelegationAppService(delegationRepository);
        AssignmentDelegate created = delegationAppService.createDelegation(buildDelegate(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2)
        ));

        AssignmentDelegate revoked = delegationAppService.revokeDelegation("T001", "CRM", created.getDelegationId());

        Assertions.assertEquals("REVOKED", revoked.getStatus());
        Assertions.assertTrue(delegationAppService.listActiveDelegations(
            "T001", "CRM", "SUB_USER", "approver-b", LocalDateTime.now()).isEmpty());
    }

    @Test
    void shouldReturnOnlyEffectiveDelegations() {
        InMemoryDelegationRepository delegationRepository = new InMemoryDelegationRepository();
        DelegationAppService delegationAppService = new DelegationAppService(delegationRepository);
        delegationAppService.createDelegation(buildDelegate(
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().plusHours(2)
        ));
        delegationAppService.createDelegation(buildDelegate(
            LocalDateTime.now().minusDays(2),
            LocalDateTime.now().minusDays(1)
        ));

        List<AssignmentDelegate> activeDelegations = delegationAppService.listActiveDelegations(
            "T001", "CRM", "SUB_USER", "approver-b", LocalDateTime.now());

        Assertions.assertEquals(1, activeDelegations.size());
        Assertions.assertEquals("CONTRACT_APPROVE", activeDelegations.get(0).getPermissionCode());
    }

    @Test
    void shouldRejectInvalidEffectiveWindow() {
        DelegationAppService delegationAppService = new DelegationAppService(new InMemoryDelegationRepository());

        Assertions.assertThrows(BusinessException.class, () -> delegationAppService.createDelegation(buildDelegate(
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now()
        )));
    }

    @Test
    void shouldRejectDelegationWhenGrantorDoesNotOwnPermission() {
        DelegationAppService delegationAppService = new DelegationAppService(new InMemoryDelegationRepository(false));

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> delegationAppService.createDelegation(buildDelegate(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        )));

        Assertions.assertEquals("AUTHZ-409", exception.getCode());
        Assertions.assertEquals("委托人未持有可委托的有效权限", exception.getMessage());
    }

    @Test
    void shouldListGrantablePermissionCodes() {
        DelegationAppService delegationAppService = new DelegationAppService(new InMemoryDelegationRepository());

        List<String> permissionCodes = delegationAppService.listGrantablePermissionCodes(
            "T001",
            "CRM",
            "SUB_USER",
            "approver-a",
            LocalDateTime.now()
        );

        Assertions.assertEquals(Collections.singletonList("CONTRACT_APPROVE"), permissionCodes);
    }

    private AssignmentDelegate buildDelegate(LocalDateTime startTime, LocalDateTime endTime) {
        return AssignmentDelegate.builder()
            .tenantId("T001")
            .appCode("CRM")
            .grantorSubjectModel("SUB_USER")
            .grantorSubjectId("approver-a")
            .delegateSubjectModel("SUB_USER")
            .delegateSubjectId("approver-b")
            .permissionCode("CONTRACT_APPROVE")
            .startTime(startTime)
            .endTime(endTime)
            .reason("请假期间委托")
            .build();
    }

    private static final class InMemoryDelegationRepository implements DelegationRepository {

        private final boolean grantorHasPermission;

        private final AtomicLong idGenerator = new AtomicLong(1L);

        private final List<AssignmentDelegate> values = new ArrayList<>();

        private InMemoryDelegationRepository() {
            this(true);
        }

        private InMemoryDelegationRepository(boolean grantorHasPermission) {
            this.grantorHasPermission = grantorHasPermission;
        }

        @Override
        public boolean hasActiveGrantPermission(
            String tenantId,
            String appCode,
            String grantorSubjectModel,
            String grantorSubjectId,
            String permissionCode,
            LocalDateTime effectiveAt
        ) {
            return grantorHasPermission
                && "T001".equals(tenantId)
                && "CRM".equals(appCode)
                && "SUB_USER".equals(grantorSubjectModel)
                && "approver-a".equals(grantorSubjectId)
                && "CONTRACT_APPROVE".equals(permissionCode)
                && effectiveAt != null;
        }

        @Override
        public List<String> listGrantablePermissionCodes(
            String tenantId,
            String appCode,
            String grantorSubjectModel,
            String grantorSubjectId,
            LocalDateTime effectiveAt
        ) {
            if (hasActiveGrantPermission(
                tenantId,
                appCode,
                grantorSubjectModel,
                grantorSubjectId,
                "CONTRACT_APPROVE",
                effectiveAt
            )) {
                return Collections.singletonList("CONTRACT_APPROVE");
            }
            return Collections.emptyList();
        }

        @Override
        public AssignmentDelegate save(AssignmentDelegate assignmentDelegate) {
            assignmentDelegate.setDelegationId(idGenerator.getAndIncrement());
            values.add(assignmentDelegate);
            return assignmentDelegate;
        }

        @Override
        public AssignmentDelegate revoke(String tenantId, String appCode, Long delegationId) {
            AssignmentDelegate assignmentDelegate = values.stream()
                .filter(item -> tenantId.equals(item.getTenantId()))
                .filter(item -> appCode.equals(item.getAppCode()))
                .filter(item -> delegationId.equals(item.getDelegationId()))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
            assignmentDelegate.setStatus("REVOKED");
            return assignmentDelegate;
        }

        @Override
        public PageResult<AssignmentDelegate> pageDelegations(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return PageResult.<AssignmentDelegate>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(values.size())
                .records(new ArrayList<>(values))
                .build();
        }

        @Override
        public AssignmentDelegate findDelegation(String tenantId, String appCode, Long delegationId) {
            return values.stream()
                .filter(item -> tenantId.equals(item.getTenantId()))
                .filter(item -> appCode.equals(item.getAppCode()))
                .filter(item -> delegationId.equals(item.getDelegationId()))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<AssignmentDelegate> findActiveDelegations(
            String tenantId,
            String appCode,
            String delegateSubjectModel,
            String delegateSubjectId,
            LocalDateTime effectiveAt
        ) {
            return values.stream()
                .filter(item -> tenantId.equals(item.getTenantId()))
                .filter(item -> appCode.equals(item.getAppCode()))
                .filter(item -> delegateSubjectModel.equals(item.getDelegateSubjectModel()))
                .filter(item -> delegateSubjectId.equals(item.getDelegateSubjectId()))
                .filter(item -> "ACTIVE".equals(item.getStatus()))
                .filter(item -> !effectiveAt.isBefore(item.getStartTime()))
                .filter(item -> !effectiveAt.isAfter(item.getEndTime()))
                .collect(Collectors.toList());
        }
    }
}