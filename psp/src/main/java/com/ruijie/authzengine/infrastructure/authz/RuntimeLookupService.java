package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.domain.model.common.DelegationStatus;
import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthMetaModelEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthSubjectRelationEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.StandardActionEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAssignmentDelegateEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysOrgEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysUserEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthMetaModelPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthRolePersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthSubjectRelationPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.BoMetaModelPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.StandardActionPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAssignmentDelegatePersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysOrgPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysPositionPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResApiPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResComponentPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResMenuPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysResPagePersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysUserGroupPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysUserPersistenceService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 运行时治理查询服务，负责为 PIP 提供主体展开、资源识别、动作标准化与委托命中能力。
 */
@Component
public class RuntimeLookupService {

    private final AuthMetaModelPersistenceService authMetaModelPersistenceService;

    private final AuthSubjectRelationPersistenceService authSubjectRelationPersistenceService;

    private final SysUserPersistenceService sysUserPersistenceService;

    private final SysOrgPersistenceService sysOrgPersistenceService;

    private final SysPositionPersistenceService sysPositionPersistenceService;

    private final SysUserGroupPersistenceService sysUserGroupPersistenceService;

    private final AuthRolePersistenceService authRolePersistenceService;

    private final SysResMenuPersistenceService sysResMenuPersistenceService;

    private final SysResPagePersistenceService sysResPagePersistenceService;

    private final SysResComponentPersistenceService sysResComponentPersistenceService;

    private final SysResApiPersistenceService sysResApiPersistenceService;

    private final BoMetaModelPersistenceService boMetaModelPersistenceService;

    private final StandardActionPersistenceService standardActionPersistenceService;

    private final SysAssignmentDelegatePersistenceService sysAssignmentDelegatePersistenceService;

    @Autowired
    public RuntimeLookupService(
        AuthMetaModelPersistenceService authMetaModelPersistenceService,
        AuthSubjectRelationPersistenceService authSubjectRelationPersistenceService,
        SysUserPersistenceService sysUserPersistenceService,
        SysOrgPersistenceService sysOrgPersistenceService,
        SysPositionPersistenceService sysPositionPersistenceService,
        SysUserGroupPersistenceService sysUserGroupPersistenceService,
        AuthRolePersistenceService authRolePersistenceService,
        SysResMenuPersistenceService sysResMenuPersistenceService,
        SysResPagePersistenceService sysResPagePersistenceService,
        SysResComponentPersistenceService sysResComponentPersistenceService,
        SysResApiPersistenceService sysResApiPersistenceService,
        BoMetaModelPersistenceService boMetaModelPersistenceService,
        StandardActionPersistenceService standardActionPersistenceService,
        SysAssignmentDelegatePersistenceService sysAssignmentDelegatePersistenceService
    ) {
        this.authMetaModelPersistenceService = authMetaModelPersistenceService;
        this.authSubjectRelationPersistenceService = authSubjectRelationPersistenceService;
        this.sysUserPersistenceService = sysUserPersistenceService;
        this.sysOrgPersistenceService = sysOrgPersistenceService;
        this.sysPositionPersistenceService = sysPositionPersistenceService;
        this.sysUserGroupPersistenceService = sysUserGroupPersistenceService;
        this.authRolePersistenceService = authRolePersistenceService;
        this.sysResMenuPersistenceService = sysResMenuPersistenceService;
        this.sysResPagePersistenceService = sysResPagePersistenceService;
        this.sysResComponentPersistenceService = sysResComponentPersistenceService;
        this.sysResApiPersistenceService = sysResApiPersistenceService;
        this.boMetaModelPersistenceService = boMetaModelPersistenceService;
        this.standardActionPersistenceService = standardActionPersistenceService;
        this.sysAssignmentDelegatePersistenceService = sysAssignmentDelegatePersistenceService;
    }

    private RuntimeLookupService() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static RuntimeLookupService noop() {
        return new RuntimeLookupService();
    }

    /**
     * 加载治理识别结果。
     *
     * @param request 鉴权请求
     * @return 治理属性
     */
    public Map<String, Object> loadGovernanceAttributes(AuthzRequest request) {
        if (isNoop()) {
            return Collections.emptyMap();
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        StandardActionEntity standardAction = findStandardAction(request.getTenantId(), request.getAction());
        String normalizedActionCode = standardAction == null
            ? normalizeRawActionCode(request.getAction())
            : standardAction.getActCode();
        attributes.put("resourceType", request.getResource().getResourceType());
        attributes.put("actionCode", request.getAction());
        attributes.put("normalizedActionCode", normalizedActionCode);
        attributes.put("subjectRegistered", isSubjectRegistered(request));
        attributes.put("resourceRegistered", isResourceRegistered(request));
        attributes.put("actionRegistered", standardAction != null);
        return attributes;
    }

    /**
     * 把主体关系表中的角色、组织、岗位和用户组补入上下文属性。
     *
     * @param request 鉴权请求
     * @param attributes 上下文属性
     */
    public void mergeRelatedSubjects(AuthzRequest request, Map<String, Object> attributes) {
        if (authSubjectRelationPersistenceService == null) {
            return;
        }
        SysUserEntity userEntity = findUserEntity(request);
        List<String> subjectIdentifiers = resolveRelationSubjectIds(request, userEntity);
        List<AuthSubjectRelationEntity> relations = authSubjectRelationPersistenceService.lambdaQuery()
            .eq(AuthSubjectRelationEntity::getTenantId, request.getTenantId())
            .eq(AuthSubjectRelationEntity::getAppCode, request.getAppCode())
            .eq(AuthSubjectRelationEntity::getSubjectModel, request.getSubject().getType())
            .and(wrapper -> {
                if (subjectIdentifiers.size() == 1) {
                    wrapper.eq(AuthSubjectRelationEntity::getSubjectId, subjectIdentifiers.get(0));
                } else {
                    wrapper.in(AuthSubjectRelationEntity::getSubjectId, subjectIdentifiers);
                }
            })
            .orderByAsc(AuthSubjectRelationEntity::getId)
            .list();
        List<String> directOrgCodes = new ArrayList<>(extractRelatedSubjectIds(relations, "SUB_ORG"));
        directOrgCodes.addAll(loadUserPrimaryOrgCodes(userEntity));
        mergeAttributeValues(attributes, "roles", extractRelatedSubjectIds(relations, "SUB_ROLE"));
        mergeAttributeValues(attributes, "orgs", loadOrgChainCodes(request, directOrgCodes));
        mergeAttributeValues(attributes, "positions", extractRelatedSubjectIds(relations, "SUB_POSITION"));
        mergeAttributeValues(attributes, "groups", extractRelatedSubjectIds(relations, "SUB_GROUP"));
    }

    /**
     * 加载当前生效的委托记录标识。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param subjectKeys 主体集合
     * @return 委托标识集合
     */
    public Set<String> loadDelegationIds(String tenantId, String appCode, Set<SubjectKey> subjectKeys) {
        if (sysAssignmentDelegatePersistenceService == null || subjectKeys == null || subjectKeys.isEmpty()) {
            return Collections.emptySet();
        }
        LocalDateTime now = LocalDateTime.now();
        return sysAssignmentDelegatePersistenceService.lambdaQuery()
            .eq(SysAssignmentDelegateEntity::getTenantId, tenantId)
            .eq(SysAssignmentDelegateEntity::getAppCode, appCode)
            .eq(SysAssignmentDelegateEntity::getStatus, DelegationStatus.ACTIVE.name())
            .le(SysAssignmentDelegateEntity::getStartTime, now)
            .ge(SysAssignmentDelegateEntity::getEndTime, now)
            .list()
            .stream()
            .filter(entity -> subjectKeys.stream().anyMatch(subjectKey ->
                subjectKey.getSubjectType().equals(entity.getDelegateSubjectModel())
                    && subjectKey.getSubjectId().equals(entity.getDelegateSubjectId())))
            .map(SysAssignmentDelegateEntity::getId)
            .map(String::valueOf)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> extractRelatedSubjectIds(List<AuthSubjectRelationEntity> relations, String subjectModel) {
        return relations.stream()
            .filter(entity -> subjectModel.equals(entity.getRelatedSubjectModel()))
            .map(AuthSubjectRelationEntity::getRelatedSubjectId)
            .collect(Collectors.toList());
    }

    private void mergeAttributeValues(Map<String, Object> attributes, String key, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>(toStringSet(attributes.get(key)));
        merged.addAll(values.stream().filter(StringUtils::hasText).collect(Collectors.toList()));
        attributes.put(key, new ArrayList<>(merged));
    }

    private Set<String> toStringSet(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptySet();
        }
        if (rawValue instanceof Collection) {
            return ((Collection<?>) rawValue).stream()
                .map(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Collections.singleton(String.valueOf(rawValue));
    }

    private List<String> loadUserPrimaryOrgCodes(SysUserEntity userEntity) {
        if (userEntity == null || sysOrgPersistenceService == null) {
            return Collections.emptyList();
        }
        if (userEntity == null || userEntity.getOrgId() == null) {
            return Collections.emptyList();
        }
        SysOrgEntity orgEntity = sysOrgPersistenceService.getById(userEntity.getOrgId());
        if (orgEntity == null || !StringUtils.hasText(orgEntity.getDepartmentCode())) {
            return Collections.emptyList();
        }
        return Collections.singletonList(orgEntity.getDepartmentCode());
    }

    private List<String> resolveRelationSubjectIds(AuthzRequest request, SysUserEntity userEntity) {
        LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        if (StringUtils.hasText(request.getSubject().getId())) {
            identifiers.add(request.getSubject().getId());
        }
        if (userEntity != null && userEntity.getId() != null) {
            identifiers.add(String.valueOf(userEntity.getId()));
        }
        return new ArrayList<>(identifiers);
    }

    private SysUserEntity findUserEntity(AuthzRequest request) {
        if (sysUserPersistenceService == null || !"SUB_USER".equals(request.getSubject().getType())) {
            return null;
        }
        return sysUserPersistenceService.lambdaQuery()
            .eq(SysUserEntity::getTenantId, request.getTenantId())
            .eq(SysUserEntity::getAppCode, request.getAppCode())
            .and(wrapper -> wrapper.eq(SysUserEntity::getStaffNo, request.getSubject().getId())
                .or()
                .eq(SysUserEntity::getUserId, request.getSubject().getId()))
            .one();
    }

    private List<String> loadOrgChainCodes(AuthzRequest request, Collection<String> directOrgCodes) {
        if (sysOrgPersistenceService == null || directOrgCodes == null || directOrgCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysOrgEntity> orgEntities = sysOrgPersistenceService.lambdaQuery()
            .eq(SysOrgEntity::getTenantId, request.getTenantId())
            .eq(SysOrgEntity::getAppCode, request.getAppCode())
            .list();
        if (orgEntities.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, SysOrgEntity> orgByCode = orgEntities.stream()
            .filter(entity -> StringUtils.hasText(entity.getDepartmentCode()))
            .collect(Collectors.toMap(SysOrgEntity::getDepartmentCode, entity -> entity, (left, right) -> left, LinkedHashMap::new));
        Map<Long, SysOrgEntity> orgById = orgEntities.stream()
            .filter(entity -> entity.getId() != null)
            .collect(Collectors.toMap(SysOrgEntity::getId, entity -> entity, (left, right) -> left, LinkedHashMap::new));
        LinkedHashSet<String> orgChainCodes = new LinkedHashSet<>();
        for (String directOrgCode : directOrgCodes) {
            if (!StringUtils.hasText(directOrgCode)) {
                continue;
            }
            SysOrgEntity current = orgByCode.get(directOrgCode);
            while (current != null && StringUtils.hasText(current.getDepartmentCode()) && orgChainCodes.add(current.getDepartmentCode())) {
                Long parentOrgId = current.getParentOrgId();
                current = parentOrgId == null ? null : orgById.get(parentOrgId);
            }
        }
        return new ArrayList<>(orgChainCodes);
    }

    private boolean isNoop() {
        return authMetaModelPersistenceService == null
            && authSubjectRelationPersistenceService == null
            && sysUserPersistenceService == null
            && sysOrgPersistenceService == null
            && sysPositionPersistenceService == null
            && sysUserGroupPersistenceService == null
            && authRolePersistenceService == null
            && sysResMenuPersistenceService == null
            && sysResPagePersistenceService == null
            && sysResComponentPersistenceService == null
            && sysResApiPersistenceService == null
            && boMetaModelPersistenceService == null
            && standardActionPersistenceService == null
            && sysAssignmentDelegatePersistenceService == null;
    }

    private boolean isSubjectRegistered(AuthzRequest request) {
        String subjectType = request.getSubject().getType();
        String subjectId = request.getSubject().getId();
        if (!StringUtils.hasText(subjectType) || !StringUtils.hasText(subjectId)) {
            return false;
        }
        // C2 修复：如果主体类型在 authz_meta_model 中配置了有效 resolver，视为已注册（由宿主系统管理）
        if (hasSubjectResolver(request.getTenantId(), request.getAppCode(), subjectType)) {
            return true;
        }
        // 未配置 resolver，从权限引擎库查询注册状态
        switch (subjectType) {
            case "SUB_USER":
                return sysUserPersistenceService != null && sysUserPersistenceService.lambdaQuery()
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysUserEntity::getTenantId, request.getTenantId())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysUserEntity::getAppCode, request.getAppCode())
                    .and(wrapper -> wrapper.eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysUserEntity::getStaffNo, subjectId)
                        .or()
                        .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysUserEntity::getUserId, subjectId))
                    .count() > 0;
            case "SUB_ORG":
                return sysOrgPersistenceService != null && sysOrgPersistenceService.lambdaQuery()
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysOrgEntity::getTenantId, request.getTenantId())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysOrgEntity::getAppCode, request.getAppCode())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysOrgEntity::getDepartmentCode, subjectId)
                    .count() > 0;
            case "SUB_POSITION":
                return sysPositionPersistenceService != null && sysPositionPersistenceService.lambdaQuery()
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysPositionEntity::getTenantId, request.getTenantId())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysPositionEntity::getAppCode, request.getAppCode())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysPositionEntity::getPositionCode, subjectId)
                    .count() > 0;
            case "SUB_GROUP":
                return sysUserGroupPersistenceService != null && sysUserGroupPersistenceService.lambdaQuery()
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysUserGroupEntity::getTenantId, request.getTenantId())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysUserGroupEntity::getAppCode, request.getAppCode())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysUserGroupEntity::getGroupCode, subjectId)
                    .count() > 0;
            case "SUB_ROLE":
                return authRolePersistenceService != null && authRolePersistenceService.lambdaQuery()
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.AuthRoleEntity::getTenantId, request.getTenantId())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.AuthRoleEntity::getAppCode, request.getAppCode())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.AuthRoleEntity::getRoleCode, subjectId)
                    .count() > 0;
            default:
                return false;
        }
    }

    /**
     * 判断指定主体类型是否在 authz_meta_model 中配置了有效的 resolver。
     * <p>
     * 有效 resolver：非空且非 "noopHook"，表示该主体由宿主系统管理。
     * </p>
     */
    private boolean hasSubjectResolver(String tenantId, String appCode, String subjectType) {
        if (authMetaModelPersistenceService == null) {
            return false;
        }
        AuthMetaModelEntity entity = authMetaModelPersistenceService.lambdaQuery()
            .eq(AuthMetaModelEntity::getTenantId, tenantId)
            .eq(AuthMetaModelEntity::getAppCode, appCode)
            .eq(AuthMetaModelEntity::getModelCode, subjectType)
            .one();
        if (entity == null) {
            return false;
        }
        String resolver = entity.getResolver();
        return StringUtils.hasText(resolver) && !"noopHook".equalsIgnoreCase(resolver.trim());
    }

    /**
     * 判断指定资源类型是否在 authz_meta_model 中配置了有效的 resolver。
     * <p>
     * 与 {@link #hasSubjectResolver} 对齐：有效 resolver 非空且非 "noopHook"，
     * 表示该资源由宿主系统管理（Shadow Mode），不依赖引擎库表存储资源实例。
     * </p>
     */
    private boolean hasResourceResolver(String tenantId, String appCode, String resourceType) {
        if (authMetaModelPersistenceService == null) {
            return false;
        }
        AuthMetaModelEntity entity = authMetaModelPersistenceService.lambdaQuery()
            .eq(AuthMetaModelEntity::getTenantId, tenantId)
            .eq(AuthMetaModelEntity::getAppCode, appCode)
            .eq(AuthMetaModelEntity::getModelCode, resourceType)
            .one();
        if (entity == null) {
            return false;
        }
        String resolver = entity.getResolver();
        return StringUtils.hasText(resolver) && !"noopHook".equalsIgnoreCase(resolver.trim());
    }

    /**
     * 判断鉴权请求中的资源是否在治理目录中注册。
     * <p>
     * 与 {@link #isSubjectRegistered} 对齐：如果资源模型在 authz_meta_model 中配置了有效 resolver，
     * 说明资源由宿主系统管理（Shadow Mode），直接视为已注册，跳过引擎库表查询。
     * 这样 permissionKey（如 "sys:audit:view"）等非数字标识也能正常通过注册校验。
     * </p>
     * <p>
     * 未配置 resolver 的模型仍走原有逻辑：通过 resId（资源表主键 Long）查表判断。
     * 模型级权限（resId 为空）不绑定具体实例，直接视为已注册。
     * </p>
     */
    private boolean isResourceRegistered(AuthzRequest request) {
        String resourceType = request.getResource().getResourceType();
        if (!StringUtils.hasText(resourceType)) {
            return false;
        }

        // Shadow Mode 旁路：资源模型配置了有效 resolver，由宿主系统管理，直接视为已注册
        if (hasResourceResolver(request.getTenantId(), request.getAppCode(), resourceType)) {
            return true;
        }

        // 模型级权限不绑定具体资源实例，只要资源类型合法即可视为已注册
        String resourceIdStr = resolveRequestResourceId(request);
        if (!StringUtils.hasText(resourceIdStr)) {
            return true;
        }
        Long resourceId;
        try {
            resourceId = Long.parseLong(resourceIdStr);
        } catch (NumberFormatException e) {
            // 非数字且未配置 resolver，视为未注册
            return false;
        }

        switch (resourceType) {
            case "RES_DATA_BO":
                return boMetaModelPersistenceService != null && boMetaModelPersistenceService.lambdaQuery()
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.BoMetaModelEntity::getTenantId, request.getTenantId())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.BoMetaModelEntity::getAppCode, request.getAppCode())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.BoMetaModelEntity::getId, resourceId)
                    .count() > 0;
            case "RES_API":
                return sysResApiPersistenceService != null && sysResApiPersistenceService.lambdaQuery()
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResApiEntity::getTenantId, request.getTenantId())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResApiEntity::getAppCode, request.getAppCode())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResApiEntity::getId, resourceId)
                    .count() > 0;
            case "RES_UI_MENU":
                return sysResMenuPersistenceService != null && sysResMenuPersistenceService.lambdaQuery()
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResMenuEntity::getTenantId, request.getTenantId())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResMenuEntity::getAppCode, request.getAppCode())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResMenuEntity::getId, resourceId)
                    .count() > 0;
            case "RES_UI_PAGE":
                return sysResPagePersistenceService != null && sysResPagePersistenceService.lambdaQuery()
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResPageEntity::getTenantId, request.getTenantId())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResPageEntity::getAppCode, request.getAppCode())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResPageEntity::getId, resourceId)
                    .count() > 0;
            case "RES_UI_COMPONENT":
                return sysResComponentPersistenceService != null && sysResComponentPersistenceService.lambdaQuery()
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResComponentEntity::getTenantId, request.getTenantId())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResComponentEntity::getAppCode, request.getAppCode())
                    .eq(com.ruijie.authzengine.infrastructure.persistence.entity.SysResComponentEntity::getId, resourceId)
                    .count() > 0;
            default:
                return false;
        }
    }

    private String resolveRequestResourceId(AuthzRequest request) {
        if (request == null || request.getResource() == null) {
            return null;
        }
        if (StringUtils.hasText(request.getResource().getResId())) {
            return request.getResource().getResId().trim();
        }
        return null;
    }

    private StandardActionEntity findStandardAction(String tenantId, String actionCode) {
        if (!StringUtils.hasText(actionCode) || standardActionPersistenceService == null) {
            return null;
        }
        String normalizedActionCode = normalizeRawActionCode(actionCode);
        return standardActionPersistenceService.lambdaQuery()
            .in(StandardActionEntity::getTenantId, Arrays.asList("__GLOBAL__", tenantId))
            .list()
            .stream()
            .filter(entity -> matchesActionCode(entity, normalizedActionCode))
            .sorted((left, right) -> Boolean.compare(!tenantId.equals(left.getTenantId()), !tenantId.equals(right.getTenantId())))
            .findFirst()
            .orElse(null);
    }

    private boolean matchesActionCode(StandardActionEntity entity, String normalizedActionCode) {
        if (entity == null || !StringUtils.hasText(normalizedActionCode)) {
            return false;
        }
        if (normalizedActionCode.equals(normalizeRawActionCode(entity.getActCode()))) {
            return true;
        }
        return parseActionAliases(entity.getActAliases()).contains(normalizedActionCode);
    }

    private Set<String> parseActionAliases(String actAliases) {
        if (!StringUtils.hasText(actAliases)) {
            return Collections.emptySet();
        }
        return Arrays.stream(actAliases.split("[,;\\s]+"))
            .map(this::normalizeRawActionCode)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeRawActionCode(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            return actionCode;
        }
        return actionCode.trim().toUpperCase();
    }
}