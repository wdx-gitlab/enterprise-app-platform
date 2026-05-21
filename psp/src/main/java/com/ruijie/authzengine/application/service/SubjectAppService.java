package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.application.spi.AuthMetaModelAdapter;
import com.ruijie.authzengine.application.spi.DataItem;
import com.ruijie.authzengine.application.spi.ModelCode;
import com.ruijie.authzengine.application.spi.ModelFieldSchema;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.subject.AuthRole;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SubjectRelationTypeNormalizer;
import com.ruijie.authzengine.domain.model.governance.subject.SysOrgNode;
import com.ruijie.authzengine.domain.model.governance.subject.SysPosition;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserGroup;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.SubjectRepository;
import com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 治理主体目录应用服务。
 *
 * <p>所有 CRUD 方法内嵌 Shadow Mode 路由判断：
 * <ul>
 *   <li>resolver 有效（非空且非 noopHook）→ 调用宿主 {@link AuthMetaModelAdapter}</li>
 *   <li>否则 → 走引擎内置 Repository（现有逻辑不变）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SubjectAppService {

    private static final Logger log = LoggerFactory.getLogger(SubjectAppService.class);
    private static final String USER_SUBJECT_MODEL = "SUB_USER";
    private static final List<String> SUPPORTED_RELATED_SUBJECT_MODELS = Collections.unmodifiableList(
        Arrays.asList("SUB_ROLE", "SUB_ORG", "SUB_POSITION", "SUB_GROUP"));

    private final SubjectRepository subjectRepository;
    private final MetaRepository metaRepository;
    private final AuthMetaResolverRouter authMetaResolverRouter;

    // ────────── 组织（SUB_ORG） ──────────

    /**
     * 分页查询组织。
     */
    public PageResult<SysOrgNode> pageOrgs(String tenantId, String appCode, String keyword,
                                                      int pageNo, int pageSize) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_ORG");
        if (ctx != null) {
            PageResult<DataItem> result =
                ctx.adapter.pageItems(ModelCode.SUB_ORG, buildKeywordParams(keyword), pageNo, pageSize);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_ORG");
            }
            return ShadowDataMapper.toModelPage(result, SysOrgNode.class, ctx.schema, tenantId, appCode);
        }
        return subjectRepository.pageOrgs(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询组织详情。
     */
    public SysOrgNode getOrg(String tenantId, String appCode, String orgCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_ORG");
        if (ctx != null) {
            DataItem result = ctx.adapter.getItem(ModelCode.SUB_ORG, orgCode);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_ORG");
            }
            return ShadowDataMapper.toModel(result, SysOrgNode.class, ctx.schema, tenantId, appCode);
        }
        SysOrgNode sysOrgNode = subjectRepository.findOrg(tenantId, appCode, orgCode);
        if (sysOrgNode == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "组织不存在");
        }
        return sysOrgNode;
    }

    /**
     * 创建组织目录（引擎模式）/委托宿主创建（Shadow Mode）。
     */
    public SysOrgNode createOrg(SysOrgNode sysOrgNode) {
        ShadowAdapterContext ctx = tryResolveShadow(sysOrgNode.getTenantId(), sysOrgNode.getAppCode(), "SUB_ORG");
        if (ctx != null) {
            DataItem result = ctx.adapter.createItem(ModelCode.SUB_ORG,
                ShadowDataMapper.fromModel(sysOrgNode, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_ORG");
            }
            return ShadowDataMapper.toModel(result, SysOrgNode.class, ctx.schema,
                sysOrgNode.getTenantId(), sysOrgNode.getAppCode());
        }
        sysOrgNode.setStatus(normalizeStatus(sysOrgNode.getStatus()));
        return subjectRepository.saveOrg(sysOrgNode);
    }

    /**
     * 更新组织目录。
     */
    public SysOrgNode updateOrg(String tenantId, String appCode, String orgCode, SysOrgNode sysOrgNode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_ORG");
        if (ctx != null) {
            DataItem result = ctx.adapter.updateItem(ModelCode.SUB_ORG, orgCode,
                ShadowDataMapper.fromModel(sysOrgNode, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_ORG");
            }
            return ShadowDataMapper.toModel(result, SysOrgNode.class, ctx.schema, tenantId, appCode);
        }
        SysOrgNode existing = getOrg(tenantId, appCode, orgCode);
        sysOrgNode.setId(existing.getId());
        sysOrgNode.setTenantId(tenantId);
        sysOrgNode.setAppCode(appCode);
        sysOrgNode.setDepartmentCode(orgCode);
        sysOrgNode.setStatus(normalizeStatus(sysOrgNode.getStatus()));
        return subjectRepository.saveOrg(sysOrgNode);
    }

    /**
     * 删除组织目录。
     */
    public void deleteOrg(String tenantId, String appCode, String orgCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_ORG");
        if (ctx != null) {
            boolean ok = ctx.adapter.deleteItem(ModelCode.SUB_ORG, orgCode);
            if (!ok) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_ORG");
            }
            return;
        }
        getOrg(tenantId, appCode, orgCode);
        if (subjectRepository.hasOrgReference(tenantId, appCode, orgCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "组织仍被用户、岗位或主体关系引用，禁止删除");
        }
        subjectRepository.deleteOrg(tenantId, appCode, orgCode);
    }

    // ────────── 用户（SUB_USER） ──────────

    /**
     * 分页查询用户。
     */
    public PageResult<SysUserAccount> pageUsers(String tenantId, String appCode, String keyword,
                                                           int pageNo, int pageSize) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_USER");
        if (ctx != null) {
            PageResult<DataItem> result =
                ctx.adapter.pageItems(ModelCode.SUB_USER, buildKeywordParams(keyword), pageNo, pageSize);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_USER");
            }
            return ShadowDataMapper.toModelPage(result, SysUserAccount.class, ctx.schema, tenantId, appCode);
        }
        return subjectRepository.pageUsers(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询用户详情。
     */
    public SysUserAccount getUser(String tenantId, String appCode, String subjectKey) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_USER");
        if (ctx != null) {
            DataItem result = ctx.adapter.getItem(ModelCode.SUB_USER, subjectKey);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_USER");
            }
            return ShadowDataMapper.toModel(result, SysUserAccount.class, ctx.schema, tenantId, appCode);
        }
        SysUserAccount userAccount = subjectRepository.findUser(tenantId, appCode, subjectKey);
        if (userAccount == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        return userAccount;
    }

    /**
     * 创建用户目录。
     */
    public SysUserAccount createUser(SysUserAccount userAccount) {
        SysUserAccount normalizedUser = normalizeUserAccount(userAccount, null);
        ShadowAdapterContext ctx = tryResolveShadow(normalizedUser.getTenantId(), normalizedUser.getAppCode(), "SUB_USER");
        if (ctx != null) {
            DataItem result = ctx.adapter.createItem(ModelCode.SUB_USER,
                ShadowDataMapper.fromModel(normalizedUser, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_USER");
            }
            return ShadowDataMapper.toModel(result, SysUserAccount.class, ctx.schema,
                normalizedUser.getTenantId(), normalizedUser.getAppCode());
        }
        return upsertUser(normalizedUser);
    }

    /**
     * 更新用户目录。
     */
    public SysUserAccount updateUser(String tenantId, String appCode, String subjectKey, SysUserAccount userAccount) {
        SysUserAccount normalizedUser = normalizeUserAccount(userAccount, null);
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_USER");
        if (ctx != null) {
            DataItem result = ctx.adapter.updateItem(ModelCode.SUB_USER, subjectKey,
                ShadowDataMapper.fromModel(normalizedUser, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_USER");
            }
            return ShadowDataMapper.toModel(result, SysUserAccount.class, ctx.schema, tenantId, appCode);
        }
        SysUserAccount existing = getUser(tenantId, appCode, subjectKey);
        normalizedUser = normalizeUserAccount(normalizedUser, existing);
        normalizedUser.setId(existing.getId());
        normalizedUser.setTenantId(tenantId);
        normalizedUser.setAppCode(appCode);
        return upsertUser(normalizedUser);
    }

    /**
     * 删除用户目录。
     */
    public void deleteUser(String tenantId, String appCode, String subjectKey) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_USER");
        if (ctx != null) {
            boolean ok = ctx.adapter.deleteItem(ModelCode.SUB_USER, subjectKey);
            if (!ok) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_USER");
            }
            return;
        }
        getUser(tenantId, appCode, subjectKey);
        if (subjectRepository.hasUserReference(tenantId, appCode, subjectKey)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "用户仍被关系、授权或委托引用，禁止删除");
        }
        subjectRepository.deleteUser(tenantId, appCode, subjectKey);
    }

    // ────────── 岗位（SUB_POSITION） ──────────

    /**
     * 分页查询岗位。
     */
    public PageResult<SysPosition> pagePositions(String tenantId, String appCode, String keyword,
                                                            int pageNo, int pageSize) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_POSITION");
        if (ctx != null) {
            PageResult<DataItem> result =
                ctx.adapter.pageItems(ModelCode.SUB_POSITION, buildKeywordParams(keyword), pageNo, pageSize);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_POSITION");
            }
            return ShadowDataMapper.toModelPage(result, SysPosition.class, ctx.schema, tenantId, appCode);
        }
        return subjectRepository.pagePositions(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询岗位详情。
     */
    public SysPosition getPosition(String tenantId, String appCode, String positionCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_POSITION");
        if (ctx != null) {
            DataItem result = ctx.adapter.getItem(ModelCode.SUB_POSITION, positionCode);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_POSITION");
            }
            return ShadowDataMapper.toModel(result, SysPosition.class, ctx.schema, tenantId, appCode);
        }
        SysPosition sysPosition = subjectRepository.findPosition(tenantId, appCode, positionCode);
        if (sysPosition == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "岗位不存在");
        }
        return sysPosition;
    }

    /**
     * 创建岗位目录。
     */
    public SysPosition createPosition(SysPosition sysPosition) {
        ShadowAdapterContext ctx = tryResolveShadow(sysPosition.getTenantId(), sysPosition.getAppCode(), "SUB_POSITION");
        if (ctx != null) {
            DataItem result = ctx.adapter.createItem(ModelCode.SUB_POSITION,
                ShadowDataMapper.fromModel(sysPosition, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_POSITION");
            }
            return ShadowDataMapper.toModel(result, SysPosition.class, ctx.schema,
                sysPosition.getTenantId(), sysPosition.getAppCode());
        }
        sysPosition.setStatus(normalizeStatus(sysPosition.getStatus()));
        return subjectRepository.savePosition(sysPosition);
    }

    /**
     * 更新岗位目录。
     */
    public SysPosition updatePosition(String tenantId, String appCode, String positionCode, SysPosition sysPosition) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_POSITION");
        if (ctx != null) {
            DataItem result = ctx.adapter.updateItem(ModelCode.SUB_POSITION, positionCode,
                ShadowDataMapper.fromModel(sysPosition, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_POSITION");
            }
            return ShadowDataMapper.toModel(result, SysPosition.class, ctx.schema, tenantId, appCode);
        }
        SysPosition existing = getPosition(tenantId, appCode, positionCode);
        sysPosition.setId(existing.getId());
        sysPosition.setTenantId(tenantId);
        sysPosition.setAppCode(appCode);
        sysPosition.setPositionCode(positionCode);
        sysPosition.setStatus(normalizeStatus(sysPosition.getStatus()));
        return subjectRepository.savePosition(sysPosition);
    }

    /**
     * 删除岗位目录。
     */
    public void deletePosition(String tenantId, String appCode, String positionCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_POSITION");
        if (ctx != null) {
            boolean ok = ctx.adapter.deleteItem(ModelCode.SUB_POSITION, positionCode);
            if (!ok) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_POSITION");
            }
            return;
        }
        getPosition(tenantId, appCode, positionCode);
        if (subjectRepository.hasPositionReference(tenantId, appCode, positionCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "岗位仍被关系或授权引用，禁止删除");
        }
        subjectRepository.deletePosition(tenantId, appCode, positionCode);
    }

    // ────────── 用户组（SUB_GROUP） ──────────

    /**
     * 分页查询用户组。
     */
    public PageResult<SysUserGroup> pageUserGroups(String tenantId, String appCode, String keyword,
                                                              int pageNo, int pageSize) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_GROUP");
        if (ctx != null) {
            PageResult<DataItem> result =
                ctx.adapter.pageItems(ModelCode.SUB_GROUP, buildKeywordParams(keyword), pageNo, pageSize);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_GROUP");
            }
            return ShadowDataMapper.toModelPage(result, SysUserGroup.class, ctx.schema, tenantId, appCode);
        }
        return subjectRepository.pageUserGroups(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询用户组详情。
     */
    public SysUserGroup getUserGroup(String tenantId, String appCode, String groupCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_GROUP");
        if (ctx != null) {
            DataItem result = ctx.adapter.getItem(ModelCode.SUB_GROUP, groupCode);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_GROUP");
            }
            return ShadowDataMapper.toModel(result, SysUserGroup.class, ctx.schema, tenantId, appCode);
        }
        SysUserGroup sysUserGroup = subjectRepository.findUserGroup(tenantId, appCode, groupCode);
        if (sysUserGroup == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户组不存在");
        }
        return sysUserGroup;
    }

    /**
     * 创建用户组目录。
     */
    public SysUserGroup createUserGroup(SysUserGroup sysUserGroup) {
        ShadowAdapterContext ctx = tryResolveShadow(sysUserGroup.getTenantId(), sysUserGroup.getAppCode(), "SUB_GROUP");
        if (ctx != null) {
            DataItem result = ctx.adapter.createItem(ModelCode.SUB_GROUP,
                ShadowDataMapper.fromModel(sysUserGroup, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_GROUP");
            }
            return ShadowDataMapper.toModel(result, SysUserGroup.class, ctx.schema,
                sysUserGroup.getTenantId(), sysUserGroup.getAppCode());
        }
        sysUserGroup.setStatus(normalizeStatus(sysUserGroup.getStatus()));
        return subjectRepository.saveUserGroup(sysUserGroup);
    }

    /**
     * 更新用户组目录。
     */
    public SysUserGroup updateUserGroup(String tenantId, String appCode, String groupCode, SysUserGroup sysUserGroup) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_GROUP");
        if (ctx != null) {
            DataItem result = ctx.adapter.updateItem(ModelCode.SUB_GROUP, groupCode,
                ShadowDataMapper.fromModel(sysUserGroup, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_GROUP");
            }
            return ShadowDataMapper.toModel(result, SysUserGroup.class, ctx.schema, tenantId, appCode);
        }
        SysUserGroup existing = getUserGroup(tenantId, appCode, groupCode);
        sysUserGroup.setId(existing.getId());
        sysUserGroup.setTenantId(tenantId);
        sysUserGroup.setAppCode(appCode);
        sysUserGroup.setGroupCode(groupCode);
        sysUserGroup.setStatus(normalizeStatus(sysUserGroup.getStatus()));
        return subjectRepository.saveUserGroup(sysUserGroup);
    }

    /**
     * 删除用户组目录。
     */
    public void deleteUserGroup(String tenantId, String appCode, String groupCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_GROUP");
        if (ctx != null) {
            boolean ok = ctx.adapter.deleteItem(ModelCode.SUB_GROUP, groupCode);
            if (!ok) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_GROUP");
            }
            return;
        }
        getUserGroup(tenantId, appCode, groupCode);
        if (subjectRepository.hasUserGroupReference(tenantId, appCode, groupCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "用户组仍被关系或授权引用，禁止删除");
        }
        subjectRepository.deleteUserGroup(tenantId, appCode, groupCode);
    }

    // ────────── 角色（SUB_ROLE） ──────────

    /**
     * 分页查询角色。
     */
    public PageResult<AuthRole> pageRoles(String tenantId, String appCode, String keyword,
                                                     int pageNo, int pageSize) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_ROLE");
        if (ctx != null) {
            PageResult<DataItem> result =
                ctx.adapter.pageItems(ModelCode.SUB_ROLE, buildKeywordParams(keyword), pageNo, pageSize);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_ROLE");
            }
            return ShadowDataMapper.toModelPage(result, AuthRole.class, ctx.schema, tenantId, appCode);
        }
        return subjectRepository.pageRoles(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询角色详情。
     */
    public AuthRole getRole(String tenantId, String appCode, String roleCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_ROLE");
        if (ctx != null) {
            DataItem result = ctx.adapter.getItem(ModelCode.SUB_ROLE, roleCode);
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_ROLE");
            }
            return ShadowDataMapper.toModel(result, AuthRole.class, ctx.schema, tenantId, appCode);
        }
        AuthRole authRole = subjectRepository.findRole(tenantId, appCode, roleCode);
        if (authRole == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在");
        }
        return authRole;
    }

    /**
     * 创建角色目录。
     */
    public AuthRole createRole(AuthRole authRole) {
        ShadowAdapterContext ctx = tryResolveShadow(authRole.getTenantId(), authRole.getAppCode(), "SUB_ROLE");
        if (ctx != null) {
            DataItem result = ctx.adapter.createItem(ModelCode.SUB_ROLE,
                ShadowDataMapper.fromModel(authRole, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_ROLE");
            }
            return ShadowDataMapper.toModel(result, AuthRole.class, ctx.schema,
                authRole.getTenantId(), authRole.getAppCode());
        }
        authRole.setStatus(normalizeStatus(authRole.getStatus()));
        return subjectRepository.saveRole(authRole);
    }

    /**
     * 更新角色目录。
     */
    public AuthRole updateRole(String tenantId, String appCode, String roleCode, AuthRole authRole) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_ROLE");
        if (ctx != null) {
            DataItem result = ctx.adapter.updateItem(ModelCode.SUB_ROLE, roleCode,
                ShadowDataMapper.fromModel(authRole, ctx.schema));
            if (result == null) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_ROLE");
            }
            return ShadowDataMapper.toModel(result, AuthRole.class, ctx.schema, tenantId, appCode);
        }
        AuthRole existing = getRole(tenantId, appCode, roleCode);
        authRole.setId(existing.getId());
        authRole.setTenantId(tenantId);
        authRole.setAppCode(appCode);
        authRole.setRoleCode(roleCode);
        authRole.setStatus(normalizeStatus(authRole.getStatus()));
        return subjectRepository.saveRole(authRole);
    }

    /**
     * 删除角色目录。
     */
    public void deleteRole(String tenantId, String appCode, String roleCode) {
        ShadowAdapterContext ctx = tryResolveShadow(tenantId, appCode, "SUB_ROLE");
        if (ctx != null) {
            boolean ok = ctx.adapter.deleteItem(ModelCode.SUB_ROLE, roleCode);
            if (!ok) {
                throw new BusinessException(ErrorCode.ADAPTER_NOT_IMPLEMENTED,
                    "业务系统暂未完成此接口的适配开发工作, modelCode=SUB_ROLE");
            }
            return;
        }
        getRole(tenantId, appCode, roleCode);
        if (subjectRepository.hasRoleReference(tenantId, appCode, roleCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "角色仍被关系或授权引用，禁止删除");
        }
        subjectRepository.deleteRole(tenantId, appCode, roleCode);
    }

    // ────────── 主体关系（不参与 Shadow 路由） ──────────

    /**
    * 跨类型关联查询。
    *
     * <p>关联边统一来自引擎库 {@code authz_subject_relation}，再按目标主体类型解析目录详情。
    * 适用于"给定某个主体实例，查询与之关联的另一类型实例列表"的场景，例如：
     * <ul>
     *   <li>角色 → 拥有该角色的用户（SUB_ROLE → SUB_USER）</li>
     *   <li>角色 → 数据范围组织（SUB_ROLE → SUB_ORG）</li>
     *   <li>组织 → 直属用户（SUB_ORG → SUB_USER）</li>
     *   <li>组织 → 直属子组织（SUB_ORG → SUB_ORG）</li>
     * </ul>
     *
     * @param tenantId        租户标识
     * @param appCode         应用标识
     * @param sourceModelCode 来源模型编码，如 {@code SUB_ROLE}
     * @param sourceId        来源实例标识（数据库主键）
     * @param targetModelCode 目标模型编码，如 {@code SUB_USER}
     * @param keyword         关键词过滤（可为空）
     * @param pageNo          页码（从 1 开始）
     * @param pageSize        每页条数
     * @return 目标类型的分页结果
     */
    public PageResult<DataItem> pageAssociatedSubjectItems(
            String tenantId, String appCode,
            String sourceModelCode, String sourceId,
            String targetModelCode, String keyword,
            int pageNo, int pageSize) {
        ModelCode targetModel = ModelCode.fromCode(targetModelCode);
        List<AuthSubjectRelation> allRelations = subjectRepository
            .pageSubjectRelations(tenantId, appCode, null, 1, Integer.MAX_VALUE)
            .getRecords();
        LinkedHashMap<String, DataItem> matchedItems = new LinkedHashMap<>();
        ShadowAdapterContext targetShadowContext = tryResolveShadow(tenantId, appCode, targetModelCode);
        for (AuthSubjectRelation relation : allRelations) {
            String targetId = resolveAssociatedTargetId(sourceModelCode, sourceId, targetModelCode, relation);
            if (!StringUtils.hasText(targetId)) {
                continue;
            }
            DataItem targetItem = resolveAssociatedTargetItem(
                tenantId,
                appCode,
                targetModelCode,
                targetModel,
                targetId,
                targetShadowContext
            );
            if (targetItem != null && matchesAssociatedItemKeyword(targetItem, keyword)) {
                String dedupeKey = StringUtils.hasText(targetItem.getId())
                    ? targetItem.getId()
                    : targetModelCode + ":" + targetId;
                matchedItems.putIfAbsent(dedupeKey, targetItem);
            }
        }
        return buildDataItemPage(new ArrayList<>(matchedItems.values()), pageNo, pageSize);
    }

    // ────────── 主体关系 CRUD ──────────

    /**
     * 创建主体关系。
     */
    public AuthSubjectRelation createSubjectRelation(AuthSubjectRelation authSubjectRelation) {
        validateSubjectRelation(authSubjectRelation);
        return subjectRepository.saveSubjectRelation(authSubjectRelation);
    }

    /**
     * 分页查询主体关系。
     */
    public PageResult<AuthSubjectRelation> pageSubjectRelations(String tenantId, String appCode,
                                                                           String keyword, int pageNo, int pageSize) {
        return subjectRepository.pageSubjectRelations(tenantId, appCode, keyword, pageNo, pageSize);
    }

    /**
     * 查询主体关系详情。
     */
    public AuthSubjectRelation getSubjectRelation(String tenantId, String appCode, Long relationId) {
        AuthSubjectRelation authSubjectRelation = subjectRepository.findSubjectRelation(tenantId, appCode, relationId);
        if (authSubjectRelation == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "主体关系不存在");
        }
        return authSubjectRelation;
    }

    /**
     * 更新主体关系。
     */
    public void updateSubjectRelation(String tenantId, String appCode, Long relationId,
                                      AuthSubjectRelation authSubjectRelation) {
        validateSubjectRelation(authSubjectRelation);
        AuthSubjectRelation existing = getSubjectRelation(tenantId, appCode, relationId);
        if (requiresRelationRecreate(existing, authSubjectRelation)) {
            throw new BusinessException(ErrorCode.RELATION_RECREATE_REQUIRED, "主体关系身份字段变更必须删除后重建");
        }
        authSubjectRelation.setId(existing.getId());
        authSubjectRelation.setTenantId(tenantId);
        authSubjectRelation.setAppCode(appCode);
        subjectRepository.saveSubjectRelation(authSubjectRelation);
    }

    /**
     * 删除主体关系。
     */
    public void deleteSubjectRelation(String tenantId, String appCode, Long relationId) {
        getSubjectRelation(tenantId, appCode, relationId);
        subjectRepository.deleteSubjectRelation(tenantId, appCode, relationId);
    }

    // ────────── 保留方法 ──────────

    /**
     * 保存或更新用户目录（无 Shadow 路由，供内部调用）。
     *
     * @param userAccount 用户目录定义
     * @return 已保存结果
     */
    public SysUserAccount upsertUser(SysUserAccount userAccount) {
        userAccount = normalizeUserAccount(userAccount, null);
        userAccount.setStatus(normalizeStatus(userAccount.getStatus()));
        return subjectRepository.saveUser(userAccount);
    }

    /**
     * 查询用户目录（全量列表）。
     *
     * @param tenantId 租户标识
     * @param appCode  应用标识
     * @return 用户目录列表
     */
    public List<SysUserAccount> listUsers(String tenantId, String appCode) {
        return subjectRepository.listUsers(tenantId, appCode);
    }

    private DataItem resolveAssociatedTargetItem(String tenantId,
                                                 String appCode,
                                                 String targetModelCode,
                                                 ModelCode targetModel,
                                                 String targetId,
                                                 ShadowAdapterContext targetShadowContext) {
        if (!StringUtils.hasText(targetId)) {
            return null;
        }
        if (targetShadowContext != null) {
            return targetShadowContext.adapter.getItem(targetModel, targetId);
        }
        return resolveNativeAssociatedItem(tenantId, appCode, targetModelCode, targetId);
    }

    private DataItem resolveNativeAssociatedItem(String tenantId,
                                                 String appCode,
                                                 String targetModelCode,
                                                 String targetId) {
        if ("SUB_USER".equals(targetModelCode)) {
            for (SysUserAccount user : subjectRepository.listUsers(tenantId, appCode)) {
                if (user != null && user.getId() != null && targetId.equals(String.valueOf(user.getId()))) {
                    return toDataItem(user);
                }
            }
            return null;
        }
        if ("SUB_ORG".equals(targetModelCode)) {
            for (SysOrgNode org : subjectRepository.pageOrgs(tenantId, appCode, null, 1, Integer.MAX_VALUE).getRecords()) {
                if (org != null && org.getId() != null && targetId.equals(String.valueOf(org.getId()))) {
                    return toDataItem(org);
                }
            }
            return null;
        }
        if ("SUB_ROLE".equals(targetModelCode)) {
            for (AuthRole role : subjectRepository.pageRoles(tenantId, appCode, null, 1, Integer.MAX_VALUE).getRecords()) {
                if (role != null && role.getId() != null && targetId.equals(String.valueOf(role.getId()))) {
                    return toDataItem(role);
                }
            }
            return null;
        }
        if ("SUB_POSITION".equals(targetModelCode)) {
            for (SysPosition position : subjectRepository.pagePositions(tenantId, appCode, null, 1, Integer.MAX_VALUE).getRecords()) {
                if (position != null && position.getId() != null && targetId.equals(String.valueOf(position.getId()))) {
                    return toDataItem(position);
                }
            }
            return null;
        }
        if ("SUB_GROUP".equals(targetModelCode)) {
            for (SysUserGroup group : subjectRepository.pageUserGroups(tenantId, appCode, null, 1, Integer.MAX_VALUE).getRecords()) {
                if (group != null && group.getId() != null && targetId.equals(String.valueOf(group.getId()))) {
                    return toDataItem(group);
                }
            }
        }
        return null;
    }

    private String resolveAssociatedTargetId(String sourceModelCode,
                                             String sourceId,
                                             String targetModelCode,
                                             AuthSubjectRelation relation) {
        if (relation == null) {
            return null;
        }
        if (safeEquals(sourceModelCode, relation.getSubjectModel())
            && safeEquals(sourceId, relation.getSubjectId())
            && safeEquals(targetModelCode, relation.getRelatedSubjectModel())) {
            return relation.getRelatedSubjectId();
        }
        if (safeEquals(sourceModelCode, relation.getRelatedSubjectModel())
            && safeEquals(sourceId, relation.getRelatedSubjectId())
            && safeEquals(targetModelCode, relation.getSubjectModel())) {
            return relation.getSubjectId();
        }
        return null;
    }

    private PageResult<DataItem> buildDataItemPage(List<DataItem> records, int pageNo, int pageSize) {
        List<DataItem> safeRecords = records == null ? Collections.<DataItem>emptyList() : records;
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, safeRecords.size());
        int toIndex = Math.min(fromIndex + safePageSize, safeRecords.size());
        return PageResult.<DataItem>builder()
            .pageNo(safePageNo)
            .pageSize(safePageSize)
            .total(safeRecords.size())
            .records(new ArrayList<>(safeRecords.subList(fromIndex, toIndex)))
            .build();
    }

    private boolean matchesAssociatedItemKeyword(DataItem item, String keyword) {
        if (item == null || !StringUtils.hasText(keyword)) {
            return item != null;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return containsKeyword(item.getId(), normalizedKeyword)
            || containsKeyword(item.getCode(), normalizedKeyword)
            || containsKeyword(item.getName(), normalizedKeyword)
            || containsKeyword(item.getStatus(), normalizedKeyword);
    }

    private boolean containsKeyword(String value, String normalizedKeyword) {
        return StringUtils.hasText(value)
            && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private DataItem toDataItem(SysUserAccount user) {
        return DataItem.builder()
            .id(user.getId() == null ? null : String.valueOf(user.getId()))
            .code(firstNonBlank(user.getStaffNo(), user.getUserId()))
            .name(firstNonBlank(user.getStaffName(), user.getUserId()))
            .status(user.getStatus())
            .attributes(user.getAttributes())
            .build();
    }

    private SysUserAccount normalizeUserAccount(SysUserAccount userAccount, SysUserAccount existing) {
        if (userAccount == null) {
            return null;
        }
        userAccount.setStaffNo(firstNonBlank(userAccount.getStaffNo(), existing == null ? null : existing.getStaffNo()));
        userAccount.setUserId(firstNonBlank(userAccount.getUserId(), existing == null ? null : existing.getUserId()));
        userAccount.setStaffCompanyName(firstNonBlank(userAccount.getStaffCompanyName(), existing == null ? null : existing.getStaffCompanyName()));
        userAccount.setStaffName(firstNonBlank(userAccount.getStaffName(), existing == null ? null : existing.getStaffName()));
        userAccount.setDepartmentCode(firstNonBlank(userAccount.getDepartmentCode(), userAccount.getOrgCode(), existing == null ? null : existing.getDepartmentCode(), existing == null ? null : existing.getOrgCode()));
        userAccount.setDepartmentName(firstNonBlank(userAccount.getDepartmentName(), existing == null ? null : existing.getDepartmentName()));
        userAccount.setStaffEmail(firstNonBlank(userAccount.getStaffEmail(), existing == null ? null : existing.getStaffEmail()));
        userAccount.setPersonalMobile(firstNonBlank(userAccount.getPersonalMobile(), existing == null ? null : existing.getPersonalMobile()));
        userAccount.setHcmPayloadJson(firstNonBlank(userAccount.getHcmPayloadJson(), existing == null ? null : existing.getHcmPayloadJson()));
        userAccount.setOrgCode(firstNonBlank(userAccount.getOrgCode(), userAccount.getDepartmentCode(), existing == null ? null : existing.getOrgCode()));
        return userAccount;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private DataItem toDataItem(SysOrgNode org) {
        return DataItem.builder()
            .id(org.getId() == null ? null : String.valueOf(org.getId()))
            .code(org.getDepartmentCode())
            .name(org.getDepartmentName())
            .status(org.getStatus())
            .attributes(org.getAttributes())
            .build();
    }

    private DataItem toDataItem(AuthRole role) {
        return DataItem.builder()
            .id(role.getId() == null ? null : String.valueOf(role.getId()))
            .code(role.getRoleCode())
            .name(role.getRoleName())
            .status(role.getStatus())
            .attributes(role.getAttributes())
            .build();
    }

    private DataItem toDataItem(SysPosition position) {
        return DataItem.builder()
            .id(position.getId() == null ? null : String.valueOf(position.getId()))
            .code(position.getPositionCode())
            .name(position.getPositionName())
            .status(position.getStatus())
            .attributes(position.getAttributes())
            .build();
    }

    private DataItem toDataItem(SysUserGroup group) {
        return DataItem.builder()
            .id(group.getId() == null ? null : String.valueOf(group.getId()))
            .code(group.getGroupCode())
            .name(group.getGroupName())
            .status(group.getStatus())
            .attributes(group.getAttributes())
            .build();
    }

    // ────────── 私有辅助方法 ──────────

    /**
     * 尝试解析 Shadow 上下文。若为 Shadow Mode，返回 {@link ShadowAdapterContext}；否则返回 null。
     */
    private ShadowAdapterContext tryResolveShadow(String tenantId, String appCode, String modelCode) {
        AuthMetaModelDefinition metaDef = metaRepository.findAuthMetaModel(tenantId, appCode, modelCode);
        if (metaDef == null) {
            return null;
        }
        String resolver = metaDef.getResolver();
        if (!StringUtils.hasText(resolver) || "noopHook".equalsIgnoreCase(resolver.trim())) {
            return null;
        }
        AuthMetaModelAdapter adapter = authMetaResolverRouter.resolve(metaDef.getAdapterType(), resolver);
        if (adapter == null) {
            log.warn("[ShadowSubject] 未找到有效适配器 Bean. tenantId={} appCode={} modelCode={} resolver={}",
                tenantId, appCode, modelCode, resolver);
            return null;
        }
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema(modelCode, metaDef.getSchemaView());
        return new ShadowAdapterContext(adapter, schema);
    }

    private Map<String, String> buildKeywordParams(String keyword) {
        Map<String, String> params = new HashMap<>(2);
        if (StringUtils.hasText(keyword)) {
            params.put("keyword", keyword);
        }
        return params;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status : "ENABLED";
    }

    private boolean requiresRelationRecreate(AuthSubjectRelation existing, AuthSubjectRelation candidate) {
        return !safeEquals(existing.getSubjectModel(), candidate.getSubjectModel())
            || !safeEquals(existing.getSubjectId(), candidate.getSubjectId())
            || !safeEquals(existing.getRelatedSubjectModel(), candidate.getRelatedSubjectModel())
            || !safeEquals(existing.getRelatedSubjectId(), candidate.getRelatedSubjectId())
            || !safeEquals(normalizeRelationType(existing), normalizeRelationType(candidate));
    }

    private void validateSubjectRelation(AuthSubjectRelation relation) {
        if (relation == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "主体关系不能为空");
        }
        if (USER_SUBJECT_MODEL.equals(relation.getRelatedSubjectModel())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                "主体关系的关联主体模型不支持 SUB_USER，用户间授权请走委托模型");
        }
        if (USER_SUBJECT_MODEL.equals(relation.getSubjectModel())
            && !SUPPORTED_RELATED_SUBJECT_MODELS.contains(relation.getRelatedSubjectModel())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                "SUB_USER 仅支持关联 SUB_ROLE/SUB_ORG/SUB_POSITION/SUB_GROUP");
        }
    }

    private String normalizeRelationType(AuthSubjectRelation relation) {
        if (relation == null) {
            return null;
        }
        return SubjectRelationTypeNormalizer.normalize(
            relation.getRelatedSubjectModel(),
            relation.getRelationType());
    }

    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    /**
     * Shadow 适配器上下文，合并 adapter + schema，避免两次查库。
     */
    private static final class ShadowAdapterContext {
        final AuthMetaModelAdapter adapter;
        final ModelFieldSchema schema;

        ShadowAdapterContext(AuthMetaModelAdapter adapter, ModelFieldSchema schema) {
            this.adapter = adapter;
            this.schema = schema;
        }
    }
}