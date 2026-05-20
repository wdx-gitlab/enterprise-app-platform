package com.ruijie.dapengine.admin.service;

import com.ruijie.dapengine.common.model.RemoteResult;
import com.ruijie.dapengine.common.model.StaffExtendInfo;
import com.ruijie.dapengine.repository.DapSysUserRepository;
import com.ruijie.dapengine.sync.OSDSService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueMessageHandleServiceTest {

    @Test
    void shouldSyncStaffWhenEventIsSupported() {
        OSDSService osdsService = mock(OSDSService.class);
        OrgSyncService orgSyncService = mock(OrgSyncService.class);
        DapSysUserRepository repository = mock(DapSysUserRepository.class);
        QueueMessageHandleService service = new QueueMessageHandleService(osdsService, orgSyncService, repository);

        StaffExtendInfo staff = new StaffExtendInfo();
        staff.setStaffNo("R13174");
        staff.setUserId("wangdaoxin");
        staff.setStaffCompanyName("王道鑫");
        staff.setDepartmentCode("000023662002");
        when(osdsService.getStaff("R13174")).thenReturn(RemoteResult.success(staff));
        when(orgSyncService.ensureDepartmentChain("000023662002")).thenReturn(101L);

        service.updateStaffInfo("{\"eventType\":\"StaffEntry\",\"parameter\":[{\"value\":\"R13174\"}]}");

        verify(osdsService).getStaff("R13174");
        verify(orgSyncService).ensureDepartmentChain("000023662002");
        verify(repository).saveOrUpdate(eq(staff), eq(101L), eq("hr-notify"), anyString());
    }

    @Test
    void shouldIgnoreUnsupportedEvent() {
        OSDSService osdsService = mock(OSDSService.class);
        OrgSyncService orgSyncService = mock(OrgSyncService.class);
        DapSysUserRepository repository = mock(DapSysUserRepository.class);
        QueueMessageHandleService service = new QueueMessageHandleService(osdsService, orgSyncService, repository);

        service.updateStaffInfo("{\"eventType\":\"UnknownEvent\",\"parameter\":[{\"value\":\"R13174\"}]}");

        verify(osdsService, never()).getStaff(anyString());
        verify(orgSyncService, never()).ensureDepartmentChain(anyString());
        verify(repository, never()).saveOrUpdate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyString(), anyString());
    }
}

