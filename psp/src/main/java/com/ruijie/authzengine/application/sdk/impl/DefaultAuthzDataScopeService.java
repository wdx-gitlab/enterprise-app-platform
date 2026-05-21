package com.ruijie.authzengine.application.sdk.impl;

import com.ruijie.authzengine.api.dto.request.AuthzResourceRequest;
import com.ruijie.authzengine.api.dto.request.AuthzSubjectRequest;
import com.ruijie.authzengine.api.dto.request.DataScopeResolveRequest;
import com.ruijie.authzengine.api.dto.response.DataScopeContractResponse;
import com.ruijie.authzengine.application.sdk.AuthzDataScopeService;
import com.ruijie.authzengine.application.sdk.model.DataScopeResolveCommand;
import com.ruijie.authzengine.application.sdk.model.DataScopeResult;
import com.ruijie.authzengine.application.service.AuthzContractAppService;
import java.util.Collections;
import org.springframework.util.Assert;

/**
 * 默认数据范围服务实现。
 */
public class DefaultAuthzDataScopeService implements AuthzDataScopeService {

    private final AuthzContractAppService authzContractAppService;

    public DefaultAuthzDataScopeService(AuthzContractAppService authzContractAppService) {
        this.authzContractAppService = authzContractAppService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataScopeResult resolveDataScope(DataScopeResolveCommand command) {
        validateCommand(command);
        DataScopeContractResponse response = authzContractAppService.resolveDataScopeContract(toRequest(command));
        return DataScopeResult.builder()
            .capabilityStatus(response == null ? null : response.getCapabilityStatus())
            .plannedScope(response == null ? null : response.getPlannedScope())
            .translatedSql(response == null ? null : response.getTranslatedSql())
            .scopeFragments(response == null || response.getScopeFragments() == null
                ? Collections.<java.util.Map<String, Object>>emptyList() : response.getScopeFragments())
            .build();
    }

    private void validateCommand(DataScopeResolveCommand command) {
        Assert.notNull(command, "command 不能为空");
        Assert.hasText(command.getTenantId(), "tenantId 不能为空");
        Assert.hasText(command.getAppCode(), "appCode 不能为空");
        Assert.hasText(command.getPolicyTemplateCode(), "policyTemplateCode 不能为空");
        Assert.notNull(command.getSubject(), "subject 不能为空");
        Assert.notNull(command.getResource(), "resource 不能为空");
        Assert.hasText(command.getSubject().getSubjectId(), "subject.subjectId 不能为空");
        Assert.hasText(command.getSubject().getSubjectModel(), "subject.subjectModel 不能为空");
        Assert.hasText(command.getResource().getResourceModel(), "resource.resourceModel 不能为空");
        Assert.hasText(command.getResource().getResourceId(), "resource.resourceId 不能为空");
    }

    private DataScopeResolveRequest toRequest(DataScopeResolveCommand command) {
        DataScopeResolveRequest request = new DataScopeResolveRequest();
        request.setTenantId(command.getTenantId());
        request.setAppCode(command.getAppCode());
        request.setPolicyTemplateCode(command.getPolicyTemplateCode());
        request.setSemanticCondition(command.getSemanticCondition());
        request.setContext(command.getContext());

        AuthzSubjectRequest subjectRequest = new AuthzSubjectRequest();
        subjectRequest.setSubjectId(command.getSubject().getSubjectId());
        subjectRequest.setSubjectModel(command.getSubject().getSubjectModel());
        request.setSubject(subjectRequest);

        AuthzResourceRequest resourceRequest = new AuthzResourceRequest();
        resourceRequest.setResourceModel(command.getResource().getResourceModel());
        resourceRequest.setResourceId(command.getResource().getResourceId());
        request.setResource(resourceRequest);
        return request;
    }
}