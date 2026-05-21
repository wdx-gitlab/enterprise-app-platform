package com.ruijie.authzengine.application.sdk.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.application.sdk.AuthzGovernanceService;
import com.ruijie.authzengine.application.sdk.model.AssignmentBindingMode;
import com.ruijie.authzengine.application.sdk.model.CreateAssignmentCommand;
import com.ruijie.authzengine.application.sdk.model.CreateSubjectRelationCommand;
import com.ruijie.authzengine.application.sdk.model.CreateUserGroupCommand;
import com.ruijie.authzengine.application.sdk.model.GovernanceRelationType;
import com.ruijie.authzengine.application.sdk.model.GovernanceSubjectModel;
import com.ruijie.authzengine.application.sdk.model.PermissionItemPageQuery;
import com.ruijie.authzengine.application.sdk.model.PolicyTemplatePageQuery;
import com.ruijie.authzengine.application.sdk.model.TenantAppPageQuery;
import com.ruijie.authzengine.application.sdk.model.TenantPageQuery;
import com.ruijie.authzengine.application.service.AssignmentAppService;
import com.ruijie.authzengine.application.service.MetaAppService;
import com.ruijie.authzengine.application.service.PermissionAppService;
import com.ruijie.authzengine.application.service.SubjectAppService;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserGroup;
import java.util.Collections;
import java.util.Map;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 治理型 SDK 默认实现。
 *
 * <p>职责：
 * <ul>
 *   <li>把业务系统的 SDK 调用路由到现有 PAP 应用服务；</li>
 *   <li>在 SDK 层补齐输入约束，避免把 PAP 的自由写入能力原样暴露出去；</li>
 *   <li>对“直接授权 / 模板授权”做模式化收口，减少业务系统误传 policyTemplateCode / policyParams。</li>
 * </ul>
 */
public class DefaultAuthzGovernanceService implements AuthzGovernanceService {

    private final MetaAppService metaAppService;

    private final PermissionAppService permissionAppService;

    private final SubjectAppService subjectAppService;

    private final AssignmentAppService assignmentAppService;

    private final ObjectMapper objectMapper;

    public DefaultAuthzGovernanceService(
        MetaAppService metaAppService,
        PermissionAppService permissionAppService,
        SubjectAppService subjectAppService,
        AssignmentAppService assignmentAppService,
        ObjectMapper objectMapper
    ) {
        this.metaAppService = metaAppService;
        this.permissionAppService = permissionAppService;
        this.subjectAppService = subjectAppService;
        this.assignmentAppService = assignmentAppService;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<AuthMetaModelDefinition> pageMetaModels(TenantAppPageQuery query) {
        validateTenantAppPageQuery(query);
        return metaAppService.pageMetaModels(
            query.getTenantId(),
            query.getAppCode(),
            query.getKeyword(),
            query.getPageNo(),
            query.getPageSize()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthMetaModelDefinition getMetaModel(String tenantId, String appCode, String modelCode) {
        validateTenantAppIdentity(tenantId, appCode);
        Assert.hasText(modelCode, "modelCode 不能为空");
        return metaAppService.getMetaModel(tenantId, appCode, modelCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<StandardActionDefinition> pageStandardActions(TenantPageQuery query) {
        validateTenantPageQuery(query);
        return metaAppService.pageStandardActions(
            query.getTenantId(),
            query.getKeyword(),
            query.getPageNo(),
            query.getPageSize()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StandardActionDefinition getStandardAction(String tenantId, String actCode) {
        Assert.hasText(tenantId, "tenantId 不能为空");
        Assert.hasText(actCode, "actCode 不能为空");
        return metaAppService.getStandardAction(tenantId, actCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<StandardPolicyTemplateDefinition> pageStandardPolicyTemplates(PolicyTemplatePageQuery query) {
        validatePolicyTemplatePageQuery(query);
        return metaAppService.pageStandardPolicyTemplates(
            query.getTenantId(),
            query.getKeyword(),
            query.getPolType() == null ? null : query.getPolType().name(),
            query.getPageNo(),
            query.getPageSize()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StandardPolicyTemplateDefinition getStandardPolicyTemplate(String tenantId, String templateCode) {
        Assert.hasText(tenantId, "tenantId 不能为空");
        Assert.hasText(templateCode, "templateCode 不能为空");
        return metaAppService.getStandardPolicyTemplate(tenantId, templateCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<AuthPermissionItem> pagePermissionItems(PermissionItemPageQuery query) {
        validatePermissionItemPageQuery(query);
        return permissionAppService.pagePermissionItems(
            query.getTenantId(),
            query.getAppCode(),
            query.getKeyword(),
            query.getResModelCode() == null ? null : query.getResModelCode().name(),
            query.getResId(),
            query.getPageNo(),
            query.getPageSize()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthPermissionItem getPermissionItem(String tenantId, String appCode, String permCode) {
        validateTenantAppIdentity(tenantId, appCode);
        Assert.hasText(permCode, "permCode 不能为空");
        return permissionAppService.getPermissionItem(tenantId, appCode, permCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<AuthSubjectRelation> pageSubjectRelations(TenantAppPageQuery query) {
        validateTenantAppPageQuery(query);
        return subjectAppService.pageSubjectRelations(
            query.getTenantId(),
            query.getAppCode(),
            query.getKeyword(),
            query.getPageNo(),
            query.getPageSize()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthSubjectRelation getSubjectRelation(String tenantId, String appCode, Long relationId) {
        validateTenantAppIdentity(tenantId, appCode);
        Assert.notNull(relationId, "relationId 不能为空");
        return subjectAppService.getSubjectRelation(tenantId, appCode, relationId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysUserGroup createUserGroup(CreateUserGroupCommand command) {
        validateCreateUserGroupCommand(command);
        return subjectAppService.createUserGroup(SysUserGroup.builder()
            .tenantId(command.getTenantId())
            .appCode(command.getAppCode())
            .groupCode(command.getGroupCode())
            .groupName(command.getGroupName())
            .status(command.getStatus())
            .attributes(command.getAttributes())
            .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysAuthAssignment createAssignment(CreateAssignmentCommand command) {
        validateCreateAssignmentCommand(command);
        String policyParamsJson = serializePolicyParams(command.getPolicyParams());
        String policyTemplateCode = command.getBindingMode() == AssignmentBindingMode.TEMPLATE
            ? command.getPolicyTemplateCode()
            : null;
        return assignmentAppService.createAssignment(policyTemplateCode, SysAuthAssignment.builder()
            .tenantId(command.getTenantId())
            .appCode(command.getAppCode())
            .subjectModel(command.getSubjectModel().name())
            .subjectId(command.getSubjectId())
            .permItemId(command.getPermItemId())
            .policyParams(policyParamsJson)
            .expireTime(command.getExpireTime())
            .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthSubjectRelation createSubjectRelation(CreateSubjectRelationCommand command) {
        validateCreateSubjectRelationCommand(command);
        return subjectAppService.createSubjectRelation(AuthSubjectRelation.builder()
            .tenantId(command.getTenantId())
            .appCode(command.getAppCode())
            .subjectModel(command.getSubjectModel().name())
            .subjectId(command.getSubjectId())
            .relatedSubjectModel(command.getRelatedSubjectModel().name())
            .relatedSubjectId(command.getRelatedSubjectId())
            .relationType(resolveRelationType(command.getRelatedSubjectModel(), command.getRelationType()))
            .build());
    }

    private void validateTenantPageQuery(TenantPageQuery query) {
        Assert.notNull(query, "query 不能为空");
        Assert.hasText(query.getTenantId(), "tenantId 不能为空");
        validatePageWindow(query.getPageNo(), query.getPageSize());
    }

    private void validateTenantAppPageQuery(TenantAppPageQuery query) {
        Assert.notNull(query, "query 不能为空");
        validateTenantAppIdentity(query.getTenantId(), query.getAppCode());
        validatePageWindow(query.getPageNo(), query.getPageSize());
    }

    private void validatePolicyTemplatePageQuery(PolicyTemplatePageQuery query) {
        Assert.notNull(query, "query 不能为空");
        Assert.hasText(query.getTenantId(), "tenantId 不能为空");
        validatePageWindow(query.getPageNo(), query.getPageSize());
    }

    private void validatePermissionItemPageQuery(PermissionItemPageQuery query) {
        Assert.notNull(query, "query 不能为空");
        validateTenantAppIdentity(query.getTenantId(), query.getAppCode());
        validatePageWindow(query.getPageNo(), query.getPageSize());
        if (StringUtils.hasText(query.getResId())) {
            Assert.notNull(query.getResModelCode(), "指定 resId 时必须同时指定 resModelCode");
        }
    }

    private void validateCreateUserGroupCommand(CreateUserGroupCommand command) {
        Assert.notNull(command, "command 不能为空");
        validateTenantAppIdentity(command.getTenantId(), command.getAppCode());
        Assert.hasText(command.getGroupCode(), "groupCode 不能为空");
        Assert.hasText(command.getGroupName(), "groupName 不能为空");
    }

    private void validateCreateSubjectRelationCommand(CreateSubjectRelationCommand command) {
        Assert.notNull(command, "command 不能为空");
        validateTenantAppIdentity(command.getTenantId(), command.getAppCode());
        Assert.notNull(command.getSubjectModel(), "subjectModel 不能为空");
        Assert.hasText(command.getSubjectId(), "subjectId 不能为空");
        Assert.notNull(command.getRelatedSubjectModel(), "relatedSubjectModel 不能为空");
        Assert.hasText(command.getRelatedSubjectId(), "relatedSubjectId 不能为空");
        Assert.isTrue(command.getRelatedSubjectModel() != GovernanceSubjectModel.SUB_USER,
            "relatedSubjectModel 不支持 SUB_USER，用户间授权请走委托模型");
    }

    private void validateCreateAssignmentCommand(CreateAssignmentCommand command) {
        Assert.notNull(command, "command 不能为空");
        validateTenantAppIdentity(command.getTenantId(), command.getAppCode());
        Assert.notNull(command.getSubjectModel(), "subjectModel 不能为空");
        Assert.hasText(command.getSubjectId(), "subjectId 不能为空");
        Assert.notNull(command.getPermItemId(), "permItemId 不能为空");
        Assert.notNull(command.getBindingMode(), "bindingMode 不能为空");
        if (command.getBindingMode() == AssignmentBindingMode.DIRECT) {
            Assert.isTrue(!StringUtils.hasText(command.getPolicyTemplateCode()),
                "DIRECT 模式不允许指定 policyTemplateCode");
            Assert.isTrue(command.getPolicyParams() == null || command.getPolicyParams().isEmpty(),
                "DIRECT 模式不允许指定 policyParams");
            return;
        }
        Assert.hasText(command.getPolicyTemplateCode(), "TEMPLATE 模式必须指定 policyTemplateCode");
    }

    private void validateTenantAppIdentity(String tenantId, String appCode) {
        Assert.hasText(tenantId, "tenantId 不能为空");
        Assert.hasText(appCode, "appCode 不能为空");
    }

    private void validatePageWindow(int pageNo, int pageSize) {
        Assert.isTrue(pageNo > 0, "pageNo 必须大于 0");
        Assert.isTrue(pageSize > 0, "pageSize 必须大于 0");
    }

    private String resolveRelationType(GovernanceSubjectModel relatedSubjectModel, GovernanceRelationType relationType) {
        if (relationType != null) {
            return relationType.name();
        }
        if (relatedSubjectModel == null) {
            return null;
        }
        switch (relatedSubjectModel) {
            case SUB_ROLE:
                return GovernanceRelationType.ROLE.name();
            case SUB_ORG:
                return GovernanceRelationType.ORG.name();
            case SUB_POSITION:
                return GovernanceRelationType.POSITION.name();
            case SUB_GROUP:
                return GovernanceRelationType.GROUP.name();
            default:
                return null;
        }
    }

    private String serializePolicyParams(Map<String, Object> policyParams) {
        if (policyParams == null || policyParams.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(policyParams);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("policyParams 不是可序列化的 JSON 对象", ex);
        }
    }
}