package com.ruijie.authzengine.application.sdk.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.application.sdk.model.AssignmentBindingMode;
import com.ruijie.authzengine.application.sdk.model.CreateAssignmentCommand;
import com.ruijie.authzengine.application.sdk.model.CreateSubjectRelationCommand;
import com.ruijie.authzengine.application.sdk.model.CreateUserGroupCommand;
import com.ruijie.authzengine.application.sdk.model.GovernanceRelationType;
import com.ruijie.authzengine.application.sdk.model.GovernanceSubjectModel;
import com.ruijie.authzengine.application.sdk.model.PermissionItemPageQuery;
import com.ruijie.authzengine.application.sdk.model.PermissionResourceModel;
import com.ruijie.authzengine.application.sdk.model.PolicyTemplatePageQuery;
import com.ruijie.authzengine.application.sdk.model.TenantPageQuery;
import com.ruijie.authzengine.application.service.AssignmentAppService;
import com.ruijie.authzengine.application.service.MetaAppService;
import com.ruijie.authzengine.application.service.PermissionAppService;
import com.ruijie.authzengine.application.service.SubjectAppService;
import com.ruijie.authzengine.domain.model.common.PolicyTemplateType;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserGroup;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAuthzGovernanceServiceTest {

    @Mock
    private MetaAppService metaAppService;

    @Mock
    private PermissionAppService permissionAppService;

    @Mock
    private SubjectAppService subjectAppService;

    @Mock
    private AssignmentAppService assignmentAppService;

    private DefaultAuthzGovernanceService governanceService;

    @BeforeEach
    void setUp() {
        governanceService = new DefaultAuthzGovernanceService(
            metaAppService,
            permissionAppService,
            subjectAppService,
            assignmentAppService,
            new ObjectMapper()
        );
    }

    @Test
    void shouldDelegatePageStandardActions() {
        PageResult<StandardActionDefinition> expected = PageResult.<StandardActionDefinition>builder()
            .pageNo(1)
            .pageSize(20)
            .total(1)
            .records(Collections.singletonList(StandardActionDefinition.builder().actCode("READ").build()))
            .build();
        when(metaAppService.pageStandardActions("T001", "合同", 1, 20)).thenReturn(expected);

        PageResult<StandardActionDefinition> result = governanceService.pageStandardActions(TenantPageQuery.builder()
            .tenantId("T001")
            .keyword("合同")
            .pageNo(1)
            .pageSize(20)
            .build());

        verify(metaAppService).pageStandardActions("T001", "合同", 1, 20);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void shouldDelegatePolicyTemplateQueryWithEnumName() {
        PageResult<StandardPolicyTemplateDefinition> expected = PageResult.<StandardPolicyTemplateDefinition>builder()
            .pageNo(1)
            .pageSize(10)
            .total(1)
            .records(Collections.singletonList(StandardPolicyTemplateDefinition.builder().templateCode("TPL_DATA_FILTER").build()))
            .build();
        when(metaAppService.pageStandardPolicyTemplates("T001", "数据", "DATA", 1, 10)).thenReturn(expected);

        PageResult<StandardPolicyTemplateDefinition> result = governanceService.pageStandardPolicyTemplates(PolicyTemplatePageQuery.builder()
            .tenantId("T001")
            .keyword("数据")
            .polType(PolicyTemplateType.DATA)
            .pageNo(1)
            .pageSize(10)
            .build());

        verify(metaAppService).pageStandardPolicyTemplates("T001", "数据", "DATA", 1, 10);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void shouldRequireResourceModelWhenPermissionQueryHasResourceId() {
        assertThatThrownBy(() -> governanceService.pagePermissionItems(PermissionItemPageQuery.builder()
                .tenantId("T001")
                .appCode("CRM")
                .resId("BO_ORDER")
                .pageNo(1)
                .pageSize(20)
                .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("resModelCode");

        verifyNoInteractions(permissionAppService);
    }

    @Test
    void shouldCreateUserGroupByDelegatingSubjectAppService() {
        SysUserGroup saved = SysUserGroup.builder().id(1001L).groupCode("GRP_SALES").groupName("销售组").build();
        when(subjectAppService.createUserGroup(org.mockito.ArgumentMatchers.any(SysUserGroup.class))).thenReturn(saved);

        SysUserGroup result = governanceService.createUserGroup(CreateUserGroupCommand.builder()
            .tenantId("T001")
            .appCode("CRM")
            .groupCode("GRP_SALES")
            .groupName("销售组")
            .status("ENABLED")
            .attributes(Collections.singletonMap("source", "sdk"))
            .build());

        ArgumentCaptor<SysUserGroup> captor = ArgumentCaptor.forClass(SysUserGroup.class);
        verify(subjectAppService).createUserGroup(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("T001");
        assertThat(captor.getValue().getAppCode()).isEqualTo("CRM");
        assertThat(captor.getValue().getGroupCode()).isEqualTo("GRP_SALES");
        assertThat(captor.getValue().getGroupName()).isEqualTo("销售组");
        assertThat(captor.getValue().getStatus()).isEqualTo("ENABLED");
        assertThat(captor.getValue().getAttributes()).containsEntry("source", "sdk");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void shouldRejectPolicyFieldsInDirectAssignmentMode() {
        assertThatThrownBy(() -> governanceService.createAssignment(CreateAssignmentCommand.builder()
                .tenantId("T001")
                .appCode("CRM")
                .subjectModel(GovernanceSubjectModel.SUB_USER)
                .subjectId("U10001")
                .permItemId(100L)
                .bindingMode(AssignmentBindingMode.DIRECT)
                .policyTemplateCode("TPL_DATA_FILTER")
                .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("DIRECT 模式不允许指定 policyTemplateCode");

        verifyNoInteractions(assignmentAppService);
    }

    @Test
    void shouldSerializePolicyParamsWhenCreateTemplateAssignment() {
        SysAuthAssignment saved = SysAuthAssignment.builder().id(2001L).build();
        when(assignmentAppService.createAssignment(org.mockito.ArgumentMatchers.eq("TPL_DATA_FILTER"),
            org.mockito.ArgumentMatchers.any(SysAuthAssignment.class))).thenReturn(saved);
        Map<String, Object> policyParams = new LinkedHashMap<>();
        policyParams.put("deptId", "D001");
        policyParams.put("limit", 10);
        LocalDateTime expireTime = LocalDateTime.of(2026, 5, 1, 12, 0);

        SysAuthAssignment result = governanceService.createAssignment(CreateAssignmentCommand.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subjectModel(GovernanceSubjectModel.SUB_ROLE)
            .subjectId("ROLE_MANAGER")
            .permItemId(100L)
            .bindingMode(AssignmentBindingMode.TEMPLATE)
            .policyTemplateCode("TPL_DATA_FILTER")
            .policyParams(policyParams)
            .expireTime(expireTime)
            .build());

        ArgumentCaptor<SysAuthAssignment> captor = ArgumentCaptor.forClass(SysAuthAssignment.class);
        verify(assignmentAppService).createAssignment(org.mockito.ArgumentMatchers.eq("TPL_DATA_FILTER"), captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("T001");
        assertThat(captor.getValue().getAppCode()).isEqualTo("CRM");
        assertThat(captor.getValue().getSubjectModel()).isEqualTo("SUB_ROLE");
        assertThat(captor.getValue().getSubjectId()).isEqualTo("ROLE_MANAGER");
        assertThat(captor.getValue().getPermItemId()).isEqualTo(100L);
        assertThat(captor.getValue().getPolicyParams()).isEqualTo("{\"deptId\":\"D001\",\"limit\":10}");
        assertThat(captor.getValue().getExpireTime()).isEqualTo(expireTime);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void shouldMapSubjectRelationToDomainModel() {
        AuthSubjectRelation saved = AuthSubjectRelation.builder().id(3001L).build();
        when(subjectAppService.createSubjectRelation(org.mockito.ArgumentMatchers.any(AuthSubjectRelation.class))).thenReturn(saved);

        AuthSubjectRelation result = governanceService.createSubjectRelation(CreateSubjectRelationCommand.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subjectModel(GovernanceSubjectModel.SUB_USER)
            .subjectId("U10001")
            .relatedSubjectModel(GovernanceSubjectModel.SUB_GROUP)
            .relatedSubjectId("GRP_SALES")
            .relationType(GovernanceRelationType.GROUP)
            .build());

        ArgumentCaptor<AuthSubjectRelation> captor = ArgumentCaptor.forClass(AuthSubjectRelation.class);
        verify(subjectAppService).createSubjectRelation(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("T001");
        assertThat(captor.getValue().getAppCode()).isEqualTo("CRM");
        assertThat(captor.getValue().getSubjectModel()).isEqualTo("SUB_USER");
        assertThat(captor.getValue().getSubjectId()).isEqualTo("U10001");
        assertThat(captor.getValue().getRelatedSubjectModel()).isEqualTo("SUB_GROUP");
        assertThat(captor.getValue().getRelatedSubjectId()).isEqualTo("GRP_SALES");
        assertThat(captor.getValue().getRelationType()).isEqualTo("GROUP");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void shouldMapPermissionItemQueryEnumToString() {
        PageResult<AuthPermissionItem> expected = PageResult.<AuthPermissionItem>builder()
            .pageNo(1)
            .pageSize(20)
            .total(1)
            .records(Collections.singletonList(AuthPermissionItem.builder().permCode("PERM_ORDER_READ").build()))
            .build();
        when(permissionAppService.pagePermissionItems("T001", "CRM", "订单", "RES_DATA_BO", "BO_ORDER", 1, 20))
            .thenReturn(expected);

        PageResult<AuthPermissionItem> result = governanceService.pagePermissionItems(PermissionItemPageQuery.builder()
            .tenantId("T001")
            .appCode("CRM")
            .keyword("订单")
            .resModelCode(PermissionResourceModel.RES_DATA_BO)
            .resId("BO_ORDER")
            .pageNo(1)
            .pageSize(20)
            .build());

        verify(permissionAppService).pagePermissionItems("T001", "CRM", "订单", "RES_DATA_BO", "BO_ORDER", 1, 20);
        assertThat(result).isSameAs(expected);
    }
}