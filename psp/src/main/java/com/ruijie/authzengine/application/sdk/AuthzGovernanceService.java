package com.ruijie.authzengine.application.sdk;

import com.ruijie.authzengine.application.sdk.model.CreateAssignmentCommand;
import com.ruijie.authzengine.application.sdk.model.CreateSubjectRelationCommand;
import com.ruijie.authzengine.application.sdk.model.CreateUserGroupCommand;
import com.ruijie.authzengine.application.sdk.model.PermissionItemPageQuery;
import com.ruijie.authzengine.application.sdk.model.PolicyTemplatePageQuery;
import com.ruijie.authzengine.application.sdk.model.TenantAppPageQuery;
import com.ruijie.authzengine.application.sdk.model.TenantPageQuery;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserGroup;

/**
 * 面向业务系统的治理型 SDK 服务。
 *
 * <p>用于开放 PAP 中较通用、较高频的治理查询与轻量写操作，
 * 让业务系统在不直接依赖 PAP 页面流程的前提下，也能完成常见目录查询与授权写入。
 *
 * <p>当前只开放：
 * <ul>
 *   <li>查询：权限元模型、标准动作、标准策略模板、权限项、主体关系</li>
 *   <li>写入：创建用户组、创建主体关系、创建授权分配</li>
 * </ul>
 *
 * <p>复杂治理能力（如策略模板配置、BO 元模型创建等）仍应继续通过 PAP 承载，
 * 避免业务系统绕过完整治理约束。</p>
 */
public interface AuthzGovernanceService {

    /**
     * 分页查询权限元模型。
     *
     * @param query 分页查询条件
     * @return 权限元模型分页结果
     */
    PageResult<AuthMetaModelDefinition> pageMetaModels(TenantAppPageQuery query);

    /**
     * 查询权限元模型详情。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param modelCode 模型编码
     * @return 权限元模型详情
     */
    AuthMetaModelDefinition getMetaModel(String tenantId, String appCode, String modelCode);

    /**
     * 分页查询标准动作。
     *
     * @param query 分页查询条件
     * @return 标准动作分页结果
     */
    PageResult<StandardActionDefinition> pageStandardActions(TenantPageQuery query);

    /**
     * 查询标准动作详情。
     *
     * @param tenantId 租户标识
     * @param actCode 动作编码
     * @return 标准动作详情
     */
    StandardActionDefinition getStandardAction(String tenantId, String actCode);

    /**
     * 分页查询标准策略模板。
     *
     * @param query 分页查询条件
     * @return 策略模板分页结果
     */
    PageResult<StandardPolicyTemplateDefinition> pageStandardPolicyTemplates(PolicyTemplatePageQuery query);

    /**
     * 查询标准策略模板详情。
     *
     * @param tenantId 租户标识
     * @param templateCode 模板编码
     * @return 标准策略模板详情
     */
    StandardPolicyTemplateDefinition getStandardPolicyTemplate(String tenantId, String templateCode);

    /**
     * 分页查询权限项。
     *
     * @param query 分页查询条件
     * @return 权限项分页结果
     */
    PageResult<AuthPermissionItem> pagePermissionItems(PermissionItemPageQuery query);

    /**
     * 查询权限项详情。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param permCode 权限项编码
     * @return 权限项详情
     */
    AuthPermissionItem getPermissionItem(String tenantId, String appCode, String permCode);

    /**
     * 分页查询主体关系。
     *
     * @param query 分页查询条件
     * @return 主体关系分页结果
     */
    PageResult<AuthSubjectRelation> pageSubjectRelations(TenantAppPageQuery query);

    /**
     * 查询主体关系详情。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param relationId 主体关系主键
     * @return 主体关系详情
     */
    AuthSubjectRelation getSubjectRelation(String tenantId, String appCode, Long relationId);

    /**
     * 创建用户组。
     *
     * @param command 用户组创建命令
     * @return 已保存的用户组
     */
    SysUserGroup createUserGroup(CreateUserGroupCommand command);

    /**
     * 创建授权分配。
     *
     * @param command 授权分配创建命令
     * @return 已保存的授权分配
     */
    SysAuthAssignment createAssignment(CreateAssignmentCommand command);

    /**
     * 创建主体关系。
     *
     * @param command 主体关系创建命令
     * @return 已保存的主体关系
     */
    AuthSubjectRelation createSubjectRelation(CreateSubjectRelationCommand command);
}