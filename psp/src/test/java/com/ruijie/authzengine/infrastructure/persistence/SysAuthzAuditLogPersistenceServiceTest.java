package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthzAuditLogEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthzAuditLogPersistenceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * SysAuthzAuditLogPersistenceService 持久化测试，验证 Hook 审计字段可正确保存与查询。
 */
@SpringBootTest
@ActiveProfiles("test")
class SysAuthzAuditLogPersistenceServiceTest {

    @Autowired
    private SysAuthzAuditLogPersistenceService sysAuthzAuditLogPersistenceService;

    @Test
    void shouldPersistAndQueryHookTraceFieldsInAuditLog() {
        SysAuthzAuditLogEntity entity = new SysAuthzAuditLogEntity();
        entity.setTenantId("T099");
        entity.setAppCode("HOOK_PERSIST_TEST");
        entity.setRequestId("REQ-HOOK-PERSIST-001");
        entity.setSubjectModel("SUB_USER");
        entity.setSubjectId("hook-persist-user");
        entity.setResourceModel("RES_DATA_BO");
        entity.setResId("HOOK_CONTRACT");
        entity.setActionCode("READ");
        entity.setDecision("PERMIT");
        entity.setCostMs(123L);
        entity.setHookStatus("SUCCESS");
        entity.setHookCostMs(50L);
        entity.setAttributeSnapshot("{\"dept\":\"SALES\"}");

        sysAuthzAuditLogPersistenceService.save(entity);

        SysAuthzAuditLogEntity loaded = sysAuthzAuditLogPersistenceService.lambdaQuery()
            .eq(SysAuthzAuditLogEntity::getTenantId, "T099")
            .eq(SysAuthzAuditLogEntity::getAppCode, "HOOK_PERSIST_TEST")
            .eq(SysAuthzAuditLogEntity::getRequestId, "REQ-HOOK-PERSIST-001")
            .one();

        Assertions.assertNotNull(loaded, "应能查询到已保存的审计记录");
        Assertions.assertEquals("SUCCESS", loaded.getHookStatus(), "hook_status 应正确持久化");
        Assertions.assertEquals(50L, loaded.getHookCostMs(), "hook_cost_ms 应正确持久化");
        Assertions.assertEquals("{\"dept\":\"SALES\"}", loaded.getAttributeSnapshot(), "attribute_snapshot 应正确持久化");
        Assertions.assertEquals("PERMIT", loaded.getDecision());
        Assertions.assertEquals(123L, loaded.getCostMs());
    }

    @Test
    void shouldAllowNullHookTraceFieldsInAuditLog() {
        SysAuthzAuditLogEntity entity = new SysAuthzAuditLogEntity();
        entity.setTenantId("T099");
        entity.setAppCode("HOOK_PERSIST_TEST");
        entity.setRequestId("REQ-HOOK-PERSIST-002");
        entity.setSubjectModel("SUB_USER");
        entity.setSubjectId("no-hook-user");
        entity.setResourceModel("RES_PAGE");
        entity.setResId("PAGE_INDEX");
        entity.setActionCode("READ");
        entity.setDecision("PERMIT");
        entity.setCostMs(10L);
        // hookStatus, hookCostMs, attributeSnapshot 均为 null

        sysAuthzAuditLogPersistenceService.save(entity);

        SysAuthzAuditLogEntity loaded = sysAuthzAuditLogPersistenceService.lambdaQuery()
            .eq(SysAuthzAuditLogEntity::getTenantId, "T099")
            .eq(SysAuthzAuditLogEntity::getAppCode, "HOOK_PERSIST_TEST")
            .eq(SysAuthzAuditLogEntity::getRequestId, "REQ-HOOK-PERSIST-002")
            .one();

        Assertions.assertNotNull(loaded, "应能查询到已保存的审计记录");
        Assertions.assertNull(loaded.getHookStatus(), "未触发 Hook 时 hook_status 应为 null");
        Assertions.assertNull(loaded.getHookCostMs(), "未触发 Hook 时 hook_cost_ms 应为 null");
        Assertions.assertNull(loaded.getAttributeSnapshot(), "未触发 Hook 时 attribute_snapshot 应为 null");
    }

    @Test
    void shouldPersistEmptyResultHookStatusInAuditLog() {
        SysAuthzAuditLogEntity entity = new SysAuthzAuditLogEntity();
        entity.setTenantId("T099");
        entity.setAppCode("HOOK_PERSIST_TEST");
        entity.setRequestId("REQ-HOOK-PERSIST-003");
        entity.setSubjectModel("SUB_EXTERNAL");
        entity.setSubjectId("ext-user-001");
        entity.setResourceModel("RES_DATA_BO");
        entity.setResId("EXT_CONTRACT");
        entity.setActionCode("READ");
        entity.setDecision("NOT_PERMIT");
        entity.setCostMs(8L);
        entity.setHookStatus("EMPTY_RESULT");
        entity.setHookCostMs(3L);

        sysAuthzAuditLogPersistenceService.save(entity);

        SysAuthzAuditLogEntity loaded = sysAuthzAuditLogPersistenceService.lambdaQuery()
            .eq(SysAuthzAuditLogEntity::getTenantId, "T099")
            .eq(SysAuthzAuditLogEntity::getAppCode, "HOOK_PERSIST_TEST")
            .eq(SysAuthzAuditLogEntity::getRequestId, "REQ-HOOK-PERSIST-003")
            .one();

        Assertions.assertNotNull(loaded);
        Assertions.assertEquals("EMPTY_RESULT", loaded.getHookStatus());
        Assertions.assertEquals(3L, loaded.getHookCostMs());
        Assertions.assertNull(loaded.getAttributeSnapshot());
    }
}
