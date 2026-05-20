package com.ruijie.dapengine.admin.service;

import com.ruijie.dapengine.common.model.DepartmentExtendInfo;
import com.ruijie.dapengine.common.model.RemoteResult;
import com.ruijie.dapengine.entity.DapSysOrgEntity;
import com.ruijie.dapengine.repository.DapSysOrgRepository;
import com.ruijie.dapengine.sync.OSDSService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrgSyncServiceTest {

    @Test
    void shouldCreateParentBeforeCurrentDepartment() {
        OSDSService osdsService = mock(OSDSService.class);
        DapSysOrgRepository repository = mock(DapSysOrgRepository.class);
        OrgSyncService service = new OrgSyncService(osdsService, repository);

        DepartmentExtendInfo parent = new DepartmentExtendInfo();
        parent.setDepartmentCode("P001");
        parent.setDepartmentName("父部门");

        DepartmentExtendInfo child = new DepartmentExtendInfo();
        child.setDepartmentCode("C001");
        child.setDepartmentName("子部门");
        child.setParentDepartmentCode("P001");
        child.setParentDepartmentName("父部门");

        DapSysOrgEntity parentEntity = new DapSysOrgEntity();
        parentEntity.setId(11L);
        parentEntity.setDepartmentCode("P001");
        parentEntity.setOrgPath("P001");

        DapSysOrgEntity childEntity = new DapSysOrgEntity();
        childEntity.setId(22L);
        childEntity.setDepartmentCode("C001");
        childEntity.setOrgPath("P001/C001");

        when(repository.findActiveByDepartmentCode("C001")).thenReturn(null, childEntity);
        when(repository.findActiveByDepartmentCode("P001")).thenReturn(null, parentEntity);
        when(osdsService.getDepartment("P001")).thenReturn(RemoteResult.success(parent));
        when(osdsService.getDepartment("C001")).thenReturn(RemoteResult.success(child));
        when(repository.insert(eq(parent), eq(null), eq("hr-notify"), any())).thenReturn(parentEntity);
        when(repository.insert(eq(child), eq(parentEntity), eq("hr-notify"), any())).thenReturn(childEntity);

        Long orgId = service.ensureDepartmentChain("C001");

        assertEquals(22L, orgId);
        verify(osdsService).getDepartment("P001");
        verify(osdsService).getDepartment("C001");
        verify(repository).insert(eq(parent), eq(null), eq("hr-notify"), any());
        verify(repository).insert(eq(child), eq(parentEntity), eq("hr-notify"), any());
        verify(repository, times(2)).findActiveByDepartmentCode("P001");
    }
}

