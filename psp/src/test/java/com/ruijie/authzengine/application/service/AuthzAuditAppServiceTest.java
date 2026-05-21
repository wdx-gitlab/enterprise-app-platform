package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.ops.AuthzAuditPage;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditQuery;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;
import com.ruijie.authzengine.domain.repository.AuthzAuditRepository;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AuthzAuditAppServiceTest {

    @Test
    void shouldQueryAuditLogsAndDetail() {
        AuthzAuditAppService appService = new AuthzAuditAppService(new AuthzAuditRepository() {
            @Override
            public AuthzAuditPage query(AuthzAuditQuery query) {
                return AuthzAuditPage.builder()
                    .records(Collections.singletonList(AuthzAuditRecord.builder()
                        .auditLogId(1L)
                        .requestId("TRACE-1")
                        .tenantId(query.getTenantId())
                        .appCode(query.getAppCode())
                        .subjectId("demo-user")
                        .subjectModel("SUB_USER")
                        .resId("CONTRACT")
                        .resourceModel("RES_DATA_BO")
                        .actionCode("READ")
                        .decision("PERMIT")
                        .matchedPermissionCodes(Arrays.asList("P1"))
                        .build()))
                    .pageNo(query.getPageNo())
                    .pageSize(query.getPageSize())
                    .total(1)
                    .build();
            }

            @Override
            public AuthzAuditRecord findById(String tenantId, String appCode, Long auditLogId) {
                return AuthzAuditRecord.builder()
                    .auditLogId(auditLogId)
                    .requestId("TRACE-1")
                    .tenantId(tenantId)
                    .appCode(appCode)
                    .decision("PERMIT")
                    .build();
            }
        });

        AuthzAuditPage page = appService.queryAuditLogs(AuthzAuditQuery.builder()
            .tenantId("T001")
            .appCode("CRM")
            .pageNo(1)
            .pageSize(20)
            .build());
        Assertions.assertEquals(1, page.getTotal());
        Assertions.assertEquals("TRACE-1", page.getRecords().get(0).getRequestId());

        AuthzAuditRecord detail = appService.getAuditLog("T001", "CRM", 1L);
        Assertions.assertNotNull(detail);
        Assertions.assertEquals("PERMIT", detail.getDecision());
    }
}
