package com.ruijie.authzengine.application.sdk.impl;

import com.ruijie.authzengine.api.dto.request.DataScopeResolveRequest;
import com.ruijie.authzengine.api.dto.response.DataScopeContractResponse;
import com.ruijie.authzengine.application.sdk.model.AuthzResourceRef;
import com.ruijie.authzengine.application.sdk.model.AuthzSubjectRef;
import com.ruijie.authzengine.application.sdk.model.DataScopeResolveCommand;
import com.ruijie.authzengine.application.sdk.model.DataScopeResult;
import com.ruijie.authzengine.application.service.AuthzContractAppService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAuthzDataScopeServiceTest {

    @Mock
    private AuthzContractAppService authzContractAppService;

    private DefaultAuthzDataScopeService dataScopeService;

    @BeforeEach
    void setUp() {
        dataScopeService = new DefaultAuthzDataScopeService(authzContractAppService);
    }

    @Test
    void shouldDelegateResolveDataScopeAndMapResponse() {
        when(authzContractAppService.resolveDataScopeContract(any(DataScopeResolveRequest.class)))
            .thenReturn(DataScopeContractResponse.builder()
                .capabilityStatus("AVAILABLE")
                .plannedScope("已支持单条 SQL 翻译")
                .translatedSql("dept_id = 1001")
                .scopeFragments(Collections.singletonList(Collections.<String, Object>singletonMap("type", "SQL_WHERE")))
                .build());

        DataScopeResult result = dataScopeService.resolveDataScope(DataScopeResolveCommand.builder()
            .tenantId("T001")
            .appCode("CRM")
            .policyTemplateCode("DATA_SCOPE_DEPT")
            .subject(AuthzSubjectRef.builder().subjectId("U10001").subjectModel("SUB_USER").build())
            .resource(AuthzResourceRef.builder().resourceModel("RES_DATA_BO").resourceId("101").build())
            .semanticCondition("deptId = #param['deptId']")
            .build());

        ArgumentCaptor<DataScopeResolveRequest> captor = ArgumentCaptor.forClass(DataScopeResolveRequest.class);
        verify(authzContractAppService).resolveDataScopeContract(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("T001");
        assertThat(captor.getValue().getAppCode()).isEqualTo("CRM");
        assertThat(captor.getValue().getPolicyTemplateCode()).isEqualTo("DATA_SCOPE_DEPT");
        assertThat(captor.getValue().getSubject().getSubjectId()).isEqualTo("U10001");
        assertThat(captor.getValue().getResource().getResourceModel()).isEqualTo("RES_DATA_BO");

        assertThat(result.getCapabilityStatus()).isEqualTo("AVAILABLE");
        assertThat(result.getTranslatedSql()).isEqualTo("dept_id = 1001");
        assertThat(result.getScopeFragments()).hasSize(1);
    }
}