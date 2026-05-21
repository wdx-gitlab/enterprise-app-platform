package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.common.SubjectModelCode;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.subject.AuthRole;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SubjectRelationTypeNormalizer;
import com.ruijie.authzengine.domain.model.governance.subject.SysOrgNode;
import com.ruijie.authzengine.domain.model.governance.subject.SysPosition;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserGroup;
import com.ruijie.authzengine.domain.repository.SubjectRepository;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthRoleEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthSubjectRelationEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAssignmentDelegateEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthAssignmentEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysOrgEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysPositionEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysUserEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysUserGroupEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthRolePersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthSubjectRelationPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAssignmentDelegatePersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthAssignmentPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysOrgPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysPositionPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysUserGroupPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysUserPersistenceService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 治理主体目录仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class DatabaseSubjectRepository implements SubjectRepository {

    private final SysOrgPersistenceService sysOrgPersistenceService;

    private final SysUserPersistenceService sysUserPersistenceService;

    private final SysPositionPersistenceService sysPositionPersistenceService;

    private final SysUserGroupPersistenceService sysUserGroupPersistenceService;

    private final AuthRolePersistenceService authRolePersistenceService;

    private final AuthSubjectRelationPersistenceService authSubjectRelationPersistenceService;

    private final SysAuthAssignmentPersistenceService sysAuthAssignmentPersistenceService;

    private final SysAssignmentDelegatePersistenceService sysAssignmentDelegatePersistenceService;

    @Override
    public SysOrgNode saveOrg(SysOrgNode sysOrgNode) {
        SysOrgEntity existing = findOrgEntity(sysOrgNode.getTenantId(), sysOrgNode.getAppCode(), sysOrgNode.getDepartmentCode());
        SysOrgEntity entity = toEntity(sysOrgNode);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        sysOrgPersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<SysOrgNode> pageOrgs(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<SysOrgNode> records = sysOrgPersistenceService.lambdaQuery()
            .eq(SysOrgEntity::getTenantId, tenantId)
            .eq(SysOrgEntity::getAppCode, appCode)
            .orderByAsc(SysOrgEntity::getDepartmentCode)
            .list()
            .stream()
            .filter(entity -> matchesKeyword(keyword, entity.getDepartmentCode(), entity.getDepartmentName(), entity.getOrgPath()))
            .map(this::toDefinition)
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public SysOrgNode findOrg(String tenantId, String appCode, String orgCode) {
        return toDefinition(findOrgEntity(tenantId, appCode, orgCode));
    }

    @Override
    public void deleteOrg(String tenantId, String appCode, String orgCode) {
        SysOrgEntity entity = findOrgEntity(tenantId, appCode, orgCode);
        if (entity != null) {
            sysOrgPersistenceService.removeById(entity.getId());
        }
    }

    @Override
    public boolean hasOrgReference(String tenantId, String appCode, String orgCode) {
        SysOrgEntity entity = findOrgEntity(tenantId, appCode, orgCode);
        if (entity == null) {
            return false;
        }
        String subjectId = String.valueOf(entity.getId());
        if (sysUserPersistenceService.lambdaQuery()
            .eq(SysUserEntity::getTenantId, tenantId)
            .eq(SysUserEntity::getAppCode, appCode)
            .eq(SysUserEntity::getOrgId, entity.getId())
            .count() > 0) {
            return true;
        }
        if (sysPositionPersistenceService.lambdaQuery()
            .eq(SysPositionEntity::getTenantId, tenantId)
            .eq(SysPositionEntity::getAppCode, appCode)
            .eq(SysPositionEntity::getOrgId, entity.getId())
            .count() > 0) {
            return true;
        }
        return hasSubjectRelationReference(tenantId, appCode, SubjectModelCode.SUB_ORG.name(), subjectId)
            || hasAssignmentReference(tenantId, appCode, SubjectModelCode.SUB_ORG.name(), subjectId)
            || hasDelegationReference(tenantId, appCode, SubjectModelCode.SUB_ORG.name(), subjectId);
    }

    @Override
    public SysUserAccount saveUser(SysUserAccount userAccount) {
        SysUserAccount normalizedUser = normalizeUserAccount(userAccount);
        SysUserEntity existing = findUserEntity(
            normalizedUser.getTenantId(),
            normalizedUser.getAppCode(),
            firstNonBlank(normalizedUser.getStaffNo(), normalizedUser.getUserId())
        );
        SysUserEntity entity = toEntity(normalizedUser);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        sysUserPersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<SysUserAccount> pageUsers(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<SysUserAccount> records = listUsers(tenantId, appCode).stream()
            .filter(item -> matchesKeyword(keyword,
                item.getStaffNo(),
                item.getUserId(),
                item.getStaffName(),
                item.getDepartmentCode(),
                item.getDepartmentName(),
                item.getOrgCode()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public SysUserAccount findUser(String tenantId, String appCode, String subjectKey) {
        return toDefinition(findUserEntity(tenantId, appCode, subjectKey));
    }

    @Override
    public void deleteUser(String tenantId, String appCode, String subjectKey) {
        SysUserEntity entity = findUserEntity(tenantId, appCode, subjectKey);
        if (entity != null) {
            sysUserPersistenceService.removeById(entity.getId());
        }
    }

    @Override
    public boolean hasUserReference(String tenantId, String appCode, String subjectKey) {
        SysUserEntity entity = findUserEntity(tenantId, appCode, subjectKey);
        if (entity == null || entity.getId() == null) {
            return false;
        }
        String subjectId = String.valueOf(entity.getId());
        return hasSubjectRelationReference(tenantId, appCode, SubjectModelCode.SUB_USER.name(), subjectId)
            || hasAssignmentReference(tenantId, appCode, SubjectModelCode.SUB_USER.name(), subjectId)
            || hasDelegationReference(tenantId, appCode, SubjectModelCode.SUB_USER.name(), subjectId);
    }

    @Override
    public SysPosition savePosition(SysPosition sysPosition) {
        SysPositionEntity existing = findPositionEntity(sysPosition.getTenantId(), sysPosition.getAppCode(), sysPosition.getPositionCode());
        SysPositionEntity entity = toEntity(sysPosition);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        sysPositionPersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<SysPosition> pagePositions(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<SysPosition> records = sysPositionPersistenceService.lambdaQuery()
            .eq(SysPositionEntity::getTenantId, tenantId)
            .eq(SysPositionEntity::getAppCode, appCode)
            .orderByAsc(SysPositionEntity::getPositionCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .filter(item -> matchesKeyword(keyword, item.getPositionCode(), item.getPositionName(), item.getOrgCode()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public SysPosition findPosition(String tenantId, String appCode, String positionCode) {
        return toDefinition(findPositionEntity(tenantId, appCode, positionCode));
    }

    @Override
    public void deletePosition(String tenantId, String appCode, String positionCode) {
        SysPositionEntity entity = findPositionEntity(tenantId, appCode, positionCode);
        if (entity != null) {
            sysPositionPersistenceService.removeById(entity.getId());
        }
    }

    @Override
    public boolean hasPositionReference(String tenantId, String appCode, String positionCode) {
        SysPositionEntity entity = findPositionEntity(tenantId, appCode, positionCode);
        if (entity == null || entity.getId() == null) {
            return false;
        }
        String subjectId = String.valueOf(entity.getId());
        return hasSubjectRelationReference(tenantId, appCode, SubjectModelCode.SUB_POSITION.name(), subjectId)
            || hasAssignmentReference(tenantId, appCode, SubjectModelCode.SUB_POSITION.name(), subjectId)
            || hasDelegationReference(tenantId, appCode, SubjectModelCode.SUB_POSITION.name(), subjectId);
    }

    @Override
    public SysUserGroup saveUserGroup(SysUserGroup sysUserGroup) {
        SysUserGroupEntity existing = findUserGroupEntity(sysUserGroup.getTenantId(), sysUserGroup.getAppCode(), sysUserGroup.getGroupCode());
        SysUserGroupEntity entity = toEntity(sysUserGroup);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        sysUserGroupPersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<SysUserGroup> pageUserGroups(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<SysUserGroup> records = sysUserGroupPersistenceService.lambdaQuery()
            .eq(SysUserGroupEntity::getTenantId, tenantId)
            .eq(SysUserGroupEntity::getAppCode, appCode)
            .orderByAsc(SysUserGroupEntity::getGroupCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .filter(item -> matchesKeyword(keyword, item.getGroupCode(), item.getGroupName()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public SysUserGroup findUserGroup(String tenantId, String appCode, String groupCode) {
        return toDefinition(findUserGroupEntity(tenantId, appCode, groupCode));
    }

    @Override
    public void deleteUserGroup(String tenantId, String appCode, String groupCode) {
        SysUserGroupEntity entity = findUserGroupEntity(tenantId, appCode, groupCode);
        if (entity != null) {
            sysUserGroupPersistenceService.removeById(entity.getId());
        }
    }

    @Override
    public boolean hasUserGroupReference(String tenantId, String appCode, String groupCode) {
        SysUserGroupEntity entity = findUserGroupEntity(tenantId, appCode, groupCode);
        if (entity == null || entity.getId() == null) {
            return false;
        }
        String subjectId = String.valueOf(entity.getId());
        return hasSubjectRelationReference(tenantId, appCode, SubjectModelCode.SUB_GROUP.name(), subjectId)
            || hasAssignmentReference(tenantId, appCode, SubjectModelCode.SUB_GROUP.name(), subjectId)
            || hasDelegationReference(tenantId, appCode, SubjectModelCode.SUB_GROUP.name(), subjectId);
    }

    @Override
    public AuthRole saveRole(AuthRole authRole) {
        AuthRoleEntity existing = findRoleEntity(authRole.getTenantId(), authRole.getAppCode(), authRole.getRoleCode());
        AuthRoleEntity entity = toEntity(authRole);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        authRolePersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<AuthRole> pageRoles(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<AuthRole> records = authRolePersistenceService.lambdaQuery()
            .eq(AuthRoleEntity::getTenantId, tenantId)
            .eq(AuthRoleEntity::getAppCode, appCode)
            .orderByAsc(AuthRoleEntity::getRoleCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .filter(item -> matchesKeyword(keyword, item.getRoleCode(), item.getRoleName(), item.getRoleScope()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public AuthRole findRole(String tenantId, String appCode, String roleCode) {
        return toDefinition(findRoleEntity(tenantId, appCode, roleCode));
    }

    @Override
    public void deleteRole(String tenantId, String appCode, String roleCode) {
        AuthRoleEntity entity = findRoleEntity(tenantId, appCode, roleCode);
        if (entity != null) {
            authRolePersistenceService.removeById(entity.getId());
        }
    }

    @Override
    public boolean hasRoleReference(String tenantId, String appCode, String roleCode) {
        AuthRoleEntity entity = findRoleEntity(tenantId, appCode, roleCode);
        if (entity == null || entity.getId() == null) {
            return false;
        }
        String subjectId = String.valueOf(entity.getId());
        return hasSubjectRelationReference(tenantId, appCode, SubjectModelCode.SUB_ROLE.name(), subjectId)
            || hasAssignmentReference(tenantId, appCode, SubjectModelCode.SUB_ROLE.name(), subjectId)
            || hasDelegationReference(tenantId, appCode, SubjectModelCode.SUB_ROLE.name(), subjectId);
    }

    @Override
    public AuthSubjectRelation saveSubjectRelation(AuthSubjectRelation authSubjectRelation) {
        normalizeSubjectRelation(authSubjectRelation);
        AuthSubjectRelationEntity existing = findSubjectRelationEntity(
            authSubjectRelation.getTenantId(),
            authSubjectRelation.getAppCode(),
            authSubjectRelation.getId(),
            authSubjectRelation
        );
        AuthSubjectRelationEntity entity = toEntity(authSubjectRelation);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        authSubjectRelationPersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<AuthSubjectRelation> pageSubjectRelations(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<AuthSubjectRelation> records = authSubjectRelationPersistenceService.lambdaQuery()
            .eq(AuthSubjectRelationEntity::getTenantId, tenantId)
            .eq(AuthSubjectRelationEntity::getAppCode, appCode)
            .orderByAsc(AuthSubjectRelationEntity::getId)
            .list()
            .stream()
            .map(this::toDefinition)
            .filter(item -> matchesKeyword(
                keyword,
                item.getSubjectModel(),
                item.getSubjectId(),
                item.getRelatedSubjectModel(),
                item.getRelatedSubjectId(),
                item.getRelationType()
            ))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public AuthSubjectRelation findSubjectRelation(String tenantId, String appCode, Long relationId) {
        if (relationId == null) {
            return null;
        }
        AuthSubjectRelationEntity entity = authSubjectRelationPersistenceService.lambdaQuery()
            .eq(AuthSubjectRelationEntity::getTenantId, tenantId)
            .eq(AuthSubjectRelationEntity::getAppCode, appCode)
            .eq(AuthSubjectRelationEntity::getId, relationId)
            .one();
        return toDefinition(entity);
    }

    /**
     * 查询指定用户的所有主体关联关系（角色/岗位/组织/用户组）。
     * 权限查询接口（Q1-Q4）主体展开阶段调用此方法，获取用户的全部间接身份。
     */
    @Override
    public List<AuthSubjectRelation> findRelationsByUserId(String tenantId, String appCode, String subjectId) {
        List<String> subjectIdentifiers = resolveSubjectIdentifiers(
            tenantId,
            appCode,
            SubjectModelCode.SUB_USER.name(),
            subjectId);
        if (subjectIdentifiers.isEmpty()) {
            return Collections.emptyList();
        }
        return authSubjectRelationPersistenceService.lambdaQuery()
            .eq(AuthSubjectRelationEntity::getTenantId, tenantId)
            .eq(AuthSubjectRelationEntity::getAppCode, appCode)
            .eq(AuthSubjectRelationEntity::getSubjectModel, SubjectModelCode.SUB_USER.name())
            .and(wrapper -> {
                if (subjectIdentifiers.size() == 1) {
                    wrapper.eq(AuthSubjectRelationEntity::getSubjectId, subjectIdentifiers.get(0));
                } else {
                    wrapper.in(AuthSubjectRelationEntity::getSubjectId, subjectIdentifiers);
                }
            })
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteSubjectRelation(String tenantId, String appCode, Long relationId) {
        AuthSubjectRelationEntity entity = authSubjectRelationPersistenceService.lambdaQuery()
            .eq(AuthSubjectRelationEntity::getTenantId, tenantId)
            .eq(AuthSubjectRelationEntity::getAppCode, appCode)
            .eq(AuthSubjectRelationEntity::getId, relationId)
            .one();
        if (entity != null) {
            authSubjectRelationPersistenceService.removeById(entity.getId());
        }
    }

    @Override
    public List<SysUserAccount> listUsers(String tenantId, String appCode) {
        return sysUserPersistenceService.lambdaQuery()
            .eq(SysUserEntity::getTenantId, tenantId)
            .eq(SysUserEntity::getAppCode, appCode)
            .orderByAsc(SysUserEntity::getStaffNo)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    private SysOrgEntity findOrgEntity(String tenantId, String appCode, String orgCode) {
        return sysOrgPersistenceService.lambdaQuery()
            .eq(SysOrgEntity::getTenantId, tenantId)
            .eq(SysOrgEntity::getAppCode, appCode)
            .eq(SysOrgEntity::getDepartmentCode, orgCode)
            .one();
    }

    private SysUserEntity findUserEntity(String tenantId, String appCode, String subjectKey) {
        if (!StringUtils.hasText(subjectKey)) {
            return null;
        }
        return sysUserPersistenceService.lambdaQuery()
            .eq(SysUserEntity::getTenantId, tenantId)
            .eq(SysUserEntity::getAppCode, appCode)
            .and(wrapper -> wrapper.eq(SysUserEntity::getStaffNo, subjectKey)
                .or()
                .eq(SysUserEntity::getUserId, subjectKey))
            .one();
    }

    private SysPositionEntity findPositionEntity(String tenantId, String appCode, String positionCode) {
        return sysPositionPersistenceService.lambdaQuery()
            .eq(SysPositionEntity::getTenantId, tenantId)
            .eq(SysPositionEntity::getAppCode, appCode)
            .eq(SysPositionEntity::getPositionCode, positionCode)
            .one();
    }

    private SysUserGroupEntity findUserGroupEntity(String tenantId, String appCode, String groupCode) {
        return sysUserGroupPersistenceService.lambdaQuery()
            .eq(SysUserGroupEntity::getTenantId, tenantId)
            .eq(SysUserGroupEntity::getAppCode, appCode)
            .eq(SysUserGroupEntity::getGroupCode, groupCode)
            .one();
    }

    private AuthRoleEntity findRoleEntity(String tenantId, String appCode, String roleCode) {
        return authRolePersistenceService.lambdaQuery()
            .eq(AuthRoleEntity::getTenantId, tenantId)
            .eq(AuthRoleEntity::getAppCode, appCode)
            .eq(AuthRoleEntity::getRoleCode, roleCode)
            .one();
    }

    private AuthSubjectRelationEntity findSubjectRelationEntity(
        String tenantId,
        String appCode,
        Long relationId,
        AuthSubjectRelation authSubjectRelation
    ) {
        if (relationId != null) {
            return authSubjectRelationPersistenceService.lambdaQuery()
                .eq(AuthSubjectRelationEntity::getTenantId, tenantId)
                .eq(AuthSubjectRelationEntity::getAppCode, appCode)
                .eq(AuthSubjectRelationEntity::getId, relationId)
                .one();
        }
        return authSubjectRelationPersistenceService.lambdaQuery()
            .eq(AuthSubjectRelationEntity::getTenantId, tenantId)
            .eq(AuthSubjectRelationEntity::getAppCode, appCode)
            .eq(AuthSubjectRelationEntity::getSubjectModel, authSubjectRelation.getSubjectModel())
            .eq(AuthSubjectRelationEntity::getSubjectId, authSubjectRelation.getSubjectId())
            .eq(AuthSubjectRelationEntity::getRelatedSubjectModel, authSubjectRelation.getRelatedSubjectModel())
            .eq(AuthSubjectRelationEntity::getRelatedSubjectId, authSubjectRelation.getRelatedSubjectId())
            .list()
            .stream()
            .filter(entity -> safeEquals(normalizeRelationType(entity), authSubjectRelation.getRelationType()))
            .findFirst()
            .orElse(null);
    }

    private SysUserEntity toEntity(SysUserAccount userAccount) {
        SysUserAccount normalizedUser = normalizeUserAccount(userAccount);
        SysUserEntity entity = new SysUserEntity();
        entity.setTenantId(normalizedUser.getTenantId());
        entity.setAppCode(normalizedUser.getAppCode());
        entity.setOrgId(resolveOrgId(normalizedUser.getTenantId(), normalizedUser.getAppCode(), firstNonBlank(normalizedUser.getDepartmentCode(), normalizedUser.getOrgCode())));
        entity.setStaffNo(normalizedUser.getStaffNo());
        entity.setUserId(normalizedUser.getUserId());
        entity.setStaffCompanyName(normalizedUser.getStaffCompanyName());
        entity.setStaffName(normalizedUser.getStaffName());
        entity.setStaffEnName(normalizedUser.getStaffEnName());
        entity.setStaffStatus(normalizedUser.getStaffStatus());
        entity.setCardNum(normalizedUser.getCardNum());
        entity.setCardTypeCode(normalizedUser.getCardTypeCode());
        entity.setCardType(normalizedUser.getCardType());
        entity.setStaffEmail(normalizedUser.getStaffEmail());
        entity.setStaffTypeCode(normalizedUser.getStaffTypeCode());
        entity.setStaffType(normalizedUser.getStaffType());
        entity.setStaffPhoto(normalizedUser.getStaffPhoto());
        entity.setPersonalEmail(normalizedUser.getPersonalEmail());
        entity.setPersonalMobile(normalizedUser.getPersonalMobile());
        entity.setJoinDate(normalizedUser.getJoinDate());
        entity.setJoinJobDate(normalizedUser.getJoinJobDate());
        entity.setNationCode(normalizedUser.getNationCode());
        entity.setNation(normalizedUser.getNation());
        entity.setCountryAndAreaCode(normalizedUser.getCountryAndAreaCode());
        entity.setCountryAndArea(normalizedUser.getCountryAndArea());
        entity.setWorkPlaceCode(normalizedUser.getWorkPlaceCode());
        entity.setWorkPlace(normalizedUser.getWorkPlace());
        entity.setOfficePlaceCode(normalizedUser.getOfficePlaceCode());
        entity.setOfficePlace(normalizedUser.getOfficePlace());
        entity.setBirthday(normalizedUser.getBirthday());
        entity.setEducationalBackgroundCode(normalizedUser.getEducationalBackgroundCode());
        entity.setEducationalBackground(normalizedUser.getEducationalBackground());
        entity.setMarriageCode(normalizedUser.getMarriageCode());
        entity.setMarriage(normalizedUser.getMarriage());
        entity.setNativePlace(normalizedUser.getNativePlace());
        entity.setGenderCode(normalizedUser.getGenderCode());
        entity.setGender(normalizedUser.getGender());
        entity.setActualFormalDate(normalizedUser.getActualFormalDate());
        entity.setLastWorkDate(normalizedUser.getLastWorkDate());
        entity.setManageUserId(normalizedUser.getManageUserId());
        entity.setManageStaffNo(normalizedUser.getManageStaffNo());
        entity.setManageStaffName(normalizedUser.getManageStaffName());
        entity.setTutorUserId(normalizedUser.getTutorUserId());
        entity.setTutorStaffNo(normalizedUser.getTutorStaffNo());
        entity.setTutorStaffName(normalizedUser.getTutorStaffName());
        entity.setPostCode(normalizedUser.getPostCode());
        entity.setPostName(normalizedUser.getPostName());
        entity.setPostTypeCode(normalizedUser.getPostTypeCode());
        entity.setPostType(normalizedUser.getPostType());
        entity.setDepartmentCode(normalizedUser.getDepartmentCode());
        entity.setDepartmentName(normalizedUser.getDepartmentName());
        entity.setOneDepartmentCode(normalizedUser.getOneDepartmentCode());
        entity.setTwoDepartmentCode(normalizedUser.getTwoDepartmentCode());
        entity.setThreeDepartmentCode(normalizedUser.getThreeDepartmentCode());
        entity.setFourDepartmentCode(normalizedUser.getFourDepartmentCode());
        entity.setFiveDepartmentCode(normalizedUser.getFiveDepartmentCode());
        entity.setSixDepartmentCode(normalizedUser.getSixDepartmentCode());
        entity.setSevenDepartmentCode(normalizedUser.getSevenDepartmentCode());
        entity.setEightDepartmentCode(normalizedUser.getEightDepartmentCode());
        entity.setNineDepartmentCode(normalizedUser.getNineDepartmentCode());
        entity.setTenDepartmentCode(normalizedUser.getTenDepartmentCode());
        entity.setOneDepartmentName(normalizedUser.getOneDepartmentName());
        entity.setTwoDepartmentName(normalizedUser.getTwoDepartmentName());
        entity.setThreeDepartmentName(normalizedUser.getThreeDepartmentName());
        entity.setFourDepartmentName(normalizedUser.getFourDepartmentName());
        entity.setFiveDepartmentName(normalizedUser.getFiveDepartmentName());
        entity.setSixDepartmentName(normalizedUser.getSixDepartmentName());
        entity.setSevenDepartmentName(normalizedUser.getSevenDepartmentName());
        entity.setEightDepartmentName(normalizedUser.getEightDepartmentName());
        entity.setNineDepartmentName(normalizedUser.getNineDepartmentName());
        entity.setTenDepartmentName(normalizedUser.getTenDepartmentName());
        entity.setOrganizationTypeCode(normalizedUser.getOrganizationTypeCode());
        entity.setOrganizationType(normalizedUser.getOrganizationType());
        entity.setComputerTypeCode(normalizedUser.getComputerTypeCode());
        entity.setComputerType(normalizedUser.getComputerType());
        entity.setCountryCode(normalizedUser.getCountryCode());
        entity.setPermanentCountryAndArea(normalizedUser.getPermanentCountryAndArea());
        entity.setPermanentCountryAndAreaCode(normalizedUser.getPermanentCountryAndAreaCode());
        entity.setVenderCompany(normalizedUser.getVenderCompany());
        entity.setEmploymentTypeCode(normalizedUser.getEmploymentTypeCode());
        entity.setEmpId(normalizedUser.getEmpId());
        entity.setEnglishCountryAndArea(normalizedUser.getEnglishCountryAndArea());
        entity.setEnglishPermanentCountryAndArea(normalizedUser.getEnglishPermanentCountryAndArea());
        entity.setPaySubjectCode(normalizedUser.getPaySubjectCode());
        entity.setPaySubject(normalizedUser.getPaySubject());
        entity.setIsDeptManage(normalizedUser.getIsDeptManage());
        entity.setIsDeptPortionManage(normalizedUser.getIsDeptPortionManage());
        entity.setStaffManagementJurisdiction(normalizedUser.getStaffManagementJurisdiction());
        entity.setHcmPayloadJson(normalizedUser.getHcmPayloadJson());
        return entity;
    }

    private SysUserAccount toDefinition(SysUserEntity entity) {
        if (entity == null) {
            return null;
        }
        return SysUserAccount.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .staffNo(firstNonBlank(entity.getStaffNo(), entity.getUserId()))
            .userId(firstNonBlank(entity.getUserId(), entity.getStaffNo()))
            .staffCompanyName(entity.getStaffCompanyName())
            .staffName(entity.getStaffName())
            .staffEnName(entity.getStaffEnName())
            .staffStatus(entity.getStaffStatus())
            .cardNum(entity.getCardNum())
            .cardTypeCode(entity.getCardTypeCode())
            .cardType(entity.getCardType())
            .staffEmail(entity.getStaffEmail())
            .staffTypeCode(entity.getStaffTypeCode())
            .staffType(entity.getStaffType())
            .staffPhoto(entity.getStaffPhoto())
            .personalEmail(entity.getPersonalEmail())
            .personalMobile(entity.getPersonalMobile())
            .joinDate(entity.getJoinDate())
            .joinJobDate(entity.getJoinJobDate())
            .nationCode(entity.getNationCode())
            .nation(entity.getNation())
            .countryAndAreaCode(entity.getCountryAndAreaCode())
            .countryAndArea(entity.getCountryAndArea())
            .workPlaceCode(entity.getWorkPlaceCode())
            .workPlace(entity.getWorkPlace())
            .officePlaceCode(entity.getOfficePlaceCode())
            .officePlace(entity.getOfficePlace())
            .birthday(entity.getBirthday())
            .educationalBackgroundCode(entity.getEducationalBackgroundCode())
            .educationalBackground(entity.getEducationalBackground())
            .marriageCode(entity.getMarriageCode())
            .marriage(entity.getMarriage())
            .nativePlace(entity.getNativePlace())
            .genderCode(entity.getGenderCode())
            .gender(entity.getGender())
            .actualFormalDate(entity.getActualFormalDate())
            .lastWorkDate(entity.getLastWorkDate())
            .manageUserId(entity.getManageUserId())
            .manageStaffNo(entity.getManageStaffNo())
            .manageStaffName(entity.getManageStaffName())
            .tutorUserId(entity.getTutorUserId())
            .tutorStaffNo(entity.getTutorStaffNo())
            .tutorStaffName(entity.getTutorStaffName())
            .postCode(entity.getPostCode())
            .postName(entity.getPostName())
            .postTypeCode(entity.getPostTypeCode())
            .postType(entity.getPostType())
            .departmentCode(entity.getDepartmentCode())
            .departmentName(entity.getDepartmentName())
            .oneDepartmentCode(entity.getOneDepartmentCode())
            .twoDepartmentCode(entity.getTwoDepartmentCode())
            .threeDepartmentCode(entity.getThreeDepartmentCode())
            .fourDepartmentCode(entity.getFourDepartmentCode())
            .fiveDepartmentCode(entity.getFiveDepartmentCode())
            .sixDepartmentCode(entity.getSixDepartmentCode())
            .sevenDepartmentCode(entity.getSevenDepartmentCode())
            .eightDepartmentCode(entity.getEightDepartmentCode())
            .nineDepartmentCode(entity.getNineDepartmentCode())
            .tenDepartmentCode(entity.getTenDepartmentCode())
            .oneDepartmentName(entity.getOneDepartmentName())
            .twoDepartmentName(entity.getTwoDepartmentName())
            .threeDepartmentName(entity.getThreeDepartmentName())
            .fourDepartmentName(entity.getFourDepartmentName())
            .fiveDepartmentName(entity.getFiveDepartmentName())
            .sixDepartmentName(entity.getSixDepartmentName())
            .sevenDepartmentName(entity.getSevenDepartmentName())
            .eightDepartmentName(entity.getEightDepartmentName())
            .nineDepartmentName(entity.getNineDepartmentName())
            .tenDepartmentName(entity.getTenDepartmentName())
            .organizationTypeCode(entity.getOrganizationTypeCode())
            .organizationType(entity.getOrganizationType())
            .computerTypeCode(entity.getComputerTypeCode())
            .computerType(entity.getComputerType())
            .countryCode(entity.getCountryCode())
            .permanentCountryAndArea(entity.getPermanentCountryAndArea())
            .permanentCountryAndAreaCode(entity.getPermanentCountryAndAreaCode())
            .venderCompany(entity.getVenderCompany())
            .employmentTypeCode(entity.getEmploymentTypeCode())
            .empId(entity.getEmpId())
            .englishCountryAndArea(entity.getEnglishCountryAndArea())
            .englishPermanentCountryAndArea(entity.getEnglishPermanentCountryAndArea())
            .paySubjectCode(entity.getPaySubjectCode())
            .paySubject(entity.getPaySubject())
            .isDeptManage(entity.getIsDeptManage())
            .isDeptPortionManage(entity.getIsDeptPortionManage())
            .staffManagementJurisdiction(entity.getStaffManagementJurisdiction())
            .hcmPayloadJson(entity.getHcmPayloadJson())
            .orgCode(resolveOrgCode(entity))
            .build();
    }

    private SysOrgEntity toEntity(SysOrgNode sysOrgNode) {
        SysOrgEntity entity = new SysOrgEntity();
        entity.setTenantId(sysOrgNode.getTenantId());
        entity.setAppCode(sysOrgNode.getAppCode());
        entity.setDepartmentCode(sysOrgNode.getDepartmentCode());
        entity.setDepartmentName(sysOrgNode.getDepartmentName());
        entity.setDepartmentEnName(sysOrgNode.getDepartmentEnName());
        entity.setDepartmentLevel(sysOrgNode.getDepartmentLevel());
        entity.setDepartmentTypeCode(sysOrgNode.getDepartmentTypeCode());
        entity.setDepartmentType(sysOrgNode.getDepartmentType());
        entity.setDepartmentCategory(sysOrgNode.getDepartmentCategory());
        entity.setParentOrgId(resolveOrgId(sysOrgNode.getTenantId(), sysOrgNode.getAppCode(), sysOrgNode.getParentDepartmentCode()));
        entity.setParentDepartmentCode(sysOrgNode.getParentDepartmentCode());
        entity.setParentDepartmentName(sysOrgNode.getParentDepartmentName());
        entity.setOrgPath(sysOrgNode.getOrgPath());
        entity.setManageUserId(sysOrgNode.getManageUserId());
        entity.setManageStaffNo(sysOrgNode.getManageStaffNo());
        entity.setManageName(sysOrgNode.getManageName());
        entity.setPortionManageUserId(sysOrgNode.getPortionManageUserId());
        entity.setPortionManageStaffNo(sysOrgNode.getPortionManageStaffNo());
        entity.setPortionManageName(sysOrgNode.getPortionManageName());
        entity.setIsEnable(sysOrgNode.getIsEnable());
        entity.setCreateTime(sysOrgNode.getCreateTime());
        entity.setDepartmentHrbpList(sysOrgNode.getDepartmentHrbpList());
        entity.setHcmPayloadJson(sysOrgNode.getHcmPayloadJson());
        return entity;
    }

    private SysOrgNode toDefinition(SysOrgEntity entity) {
        if (entity == null) {
            return null;
        }
        return SysOrgNode.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .departmentCode(entity.getDepartmentCode())
            .departmentName(entity.getDepartmentName())
            .departmentEnName(entity.getDepartmentEnName())
            .departmentLevel(entity.getDepartmentLevel())
            .departmentTypeCode(entity.getDepartmentTypeCode())
            .departmentType(entity.getDepartmentType())
            .departmentCategory(entity.getDepartmentCategory())
            .parentDepartmentCode(resolveOrgCode(entity.getTenantId(), entity.getAppCode(), entity.getParentOrgId()))
            .parentDepartmentName(entity.getParentDepartmentName())
            .orgPath(entity.getOrgPath())
            .manageUserId(entity.getManageUserId())
            .manageStaffNo(entity.getManageStaffNo())
            .manageName(entity.getManageName())
            .portionManageUserId(entity.getPortionManageUserId())
            .portionManageStaffNo(entity.getPortionManageStaffNo())
            .portionManageName(entity.getPortionManageName())
            .isEnable(entity.getIsEnable())
            .createTime(entity.getCreateTime())
            .departmentHrbpList(entity.getDepartmentHrbpList())
            .hcmPayloadJson(entity.getHcmPayloadJson())
            .build();
    }

    private SysPositionEntity toEntity(SysPosition sysPosition) {
        SysPositionEntity entity = new SysPositionEntity();
        entity.setTenantId(sysPosition.getTenantId());
        entity.setAppCode(sysPosition.getAppCode());
        entity.setPositionCode(sysPosition.getPositionCode());
        entity.setPositionName(sysPosition.getPositionName());
        entity.setOrgId(resolveOrgId(sysPosition.getTenantId(), sysPosition.getAppCode(), sysPosition.getOrgCode()));
        entity.setStatus(sysPosition.getStatus());
        return entity;
    }

    private SysPosition toDefinition(SysPositionEntity entity) {
        if (entity == null) {
            return null;
        }
        return SysPosition.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .positionCode(entity.getPositionCode())
            .positionName(entity.getPositionName())
            .orgCode(resolveOrgCode(entity.getTenantId(), entity.getAppCode(), entity.getOrgId()))
            .status(entity.getStatus())
            .build();
    }

    private SysUserGroupEntity toEntity(SysUserGroup sysUserGroup) {
        SysUserGroupEntity entity = new SysUserGroupEntity();
        entity.setTenantId(sysUserGroup.getTenantId());
        entity.setAppCode(sysUserGroup.getAppCode());
        entity.setGroupCode(sysUserGroup.getGroupCode());
        entity.setGroupName(sysUserGroup.getGroupName());
        entity.setStatus(sysUserGroup.getStatus());
        return entity;
    }

    private SysUserGroup toDefinition(SysUserGroupEntity entity) {
        if (entity == null) {
            return null;
        }
        return SysUserGroup.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .groupCode(entity.getGroupCode())
            .groupName(entity.getGroupName())
            .status(entity.getStatus())
            .build();
    }

    private AuthRoleEntity toEntity(AuthRole authRole) {
        AuthRoleEntity entity = new AuthRoleEntity();
        entity.setTenantId(authRole.getTenantId());
        entity.setAppCode(authRole.getAppCode());
        entity.setRoleCode(authRole.getRoleCode());
        entity.setRoleName(authRole.getRoleName());
        entity.setRoleScope(authRole.getRoleScope());
        entity.setStatus(authRole.getStatus());
        return entity;
    }

    private AuthRole toDefinition(AuthRoleEntity entity) {
        if (entity == null) {
            return null;
        }
        return AuthRole.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .roleCode(entity.getRoleCode())
            .roleName(entity.getRoleName())
            .roleScope(entity.getRoleScope())
            .status(entity.getStatus())
            .build();
    }

    private AuthSubjectRelationEntity toEntity(AuthSubjectRelation authSubjectRelation) {
        AuthSubjectRelationEntity entity = new AuthSubjectRelationEntity();
        entity.setTenantId(authSubjectRelation.getTenantId());
        entity.setAppCode(authSubjectRelation.getAppCode());
        entity.setSubjectModel(authSubjectRelation.getSubjectModel());
        entity.setSubjectId(authSubjectRelation.getSubjectId());
        entity.setRelatedSubjectModel(authSubjectRelation.getRelatedSubjectModel());
        entity.setRelatedSubjectId(authSubjectRelation.getRelatedSubjectId());
        entity.setRelationType(normalizeRelationType(authSubjectRelation));
        return entity;
    }

    private AuthSubjectRelation toDefinition(AuthSubjectRelationEntity entity) {
        if (entity == null) {
            return null;
        }
        return AuthSubjectRelation.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .subjectModel(entity.getSubjectModel())
            .subjectId(entity.getSubjectId())
            .relatedSubjectModel(entity.getRelatedSubjectModel())
            .relatedSubjectId(entity.getRelatedSubjectId())
            .relationType(normalizeRelationType(entity))
            .build();
    }

    private List<String> resolveSubjectIdentifiers(String tenantId, String appCode, String subjectModel, String subjectId) {
        LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        if (StringUtils.hasText(subjectId)) {
            identifiers.add(subjectId);
        }
        if (!SubjectModelCode.SUB_USER.name().equals(subjectModel) || !StringUtils.hasText(subjectId)) {
            return new ArrayList<>(identifiers);
        }
        SysUserEntity userEntity = sysUserPersistenceService.lambdaQuery()
            .eq(SysUserEntity::getTenantId, tenantId)
            .eq(SysUserEntity::getAppCode, appCode)
            .and(wrapper -> wrapper.eq(SysUserEntity::getStaffNo, subjectId)
                .or()
                .eq(SysUserEntity::getUserId, subjectId))
            .one();
        if (userEntity != null && userEntity.getId() != null) {
            identifiers.add(String.valueOf(userEntity.getId()));
        }
        return new ArrayList<>(identifiers);
    }

    private void normalizeSubjectRelation(AuthSubjectRelation authSubjectRelation) {
        if (authSubjectRelation == null) {
            return;
        }
        authSubjectRelation.setRelationType(normalizeRelationType(authSubjectRelation));
    }

    private String normalizeRelationType(AuthSubjectRelation authSubjectRelation) {
        if (authSubjectRelation == null) {
            return null;
        }
        return SubjectRelationTypeNormalizer.normalize(
            authSubjectRelation.getRelatedSubjectModel(),
            authSubjectRelation.getRelationType());
    }

    private String normalizeRelationType(AuthSubjectRelationEntity entity) {
        if (entity == null) {
            return null;
        }
        return SubjectRelationTypeNormalizer.normalize(entity.getRelatedSubjectModel(), entity.getRelationType());
    }

    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private Long resolveOrgId(String tenantId, String appCode, String orgCode) {
        if (!StringUtils.hasText(orgCode)) {
            return null;
        }
        SysOrgEntity orgEntity = sysOrgPersistenceService.lambdaQuery()
            .eq(SysOrgEntity::getTenantId, tenantId)
            .eq(SysOrgEntity::getAppCode, appCode)
            .eq(SysOrgEntity::getDepartmentCode, orgCode)
            .one();
        return orgEntity == null ? null : orgEntity.getId();
    }

    private String resolveOrgCode(SysUserEntity entity) {
        String orgCode = resolveOrgCode(entity.getTenantId(), entity.getAppCode(), entity.getOrgId());
        return StringUtils.hasText(orgCode) ? orgCode : entity.getDepartmentCode();
    }

    private SysUserAccount normalizeUserAccount(SysUserAccount userAccount) {
        if (userAccount == null) {
            return null;
        }
        userAccount.setStaffName(firstNonBlank(userAccount.getStaffName(), userAccount.getStaffCompanyName()));
        userAccount.setDepartmentCode(firstNonBlank(userAccount.getDepartmentCode(), userAccount.getOrgCode()));
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

    private String resolveOrgCode(String tenantId, String appCode, Long orgId) {
        if (orgId == null) {
            return null;
        }
        SysOrgEntity orgEntity = sysOrgPersistenceService.lambdaQuery()
            .eq(SysOrgEntity::getTenantId, tenantId)
            .eq(SysOrgEntity::getAppCode, appCode)
            .eq(SysOrgEntity::getId, orgId)
            .one();
        return orgEntity == null ? null : orgEntity.getDepartmentCode();
    }

    private boolean hasSubjectRelationReference(String tenantId, String appCode, String subjectModel, String subjectId) {
        return authSubjectRelationPersistenceService.lambdaQuery()
            .eq(AuthSubjectRelationEntity::getTenantId, tenantId)
            .eq(AuthSubjectRelationEntity::getAppCode, appCode)
            .and(wrapper -> wrapper
                .eq(AuthSubjectRelationEntity::getSubjectModel, subjectModel)
                .eq(AuthSubjectRelationEntity::getSubjectId, subjectId)
                .or()
                .eq(AuthSubjectRelationEntity::getRelatedSubjectModel, subjectModel)
                .eq(AuthSubjectRelationEntity::getRelatedSubjectId, subjectId))
            .count() > 0;
    }

    private boolean hasAssignmentReference(String tenantId, String appCode, String subjectModel, String subjectId) {
        return sysAuthAssignmentPersistenceService.lambdaQuery()
            .eq(SysAuthAssignmentEntity::getTenantId, tenantId)
            .eq(SysAuthAssignmentEntity::getAppCode, appCode)
            .eq(SysAuthAssignmentEntity::getSubjectModel, subjectModel)
            .eq(SysAuthAssignmentEntity::getSubjectId, subjectId)
            .count() > 0;
    }

    private boolean hasDelegationReference(String tenantId, String appCode, String subjectModel, String subjectId) {
        return sysAssignmentDelegatePersistenceService.lambdaQuery()
            .eq(SysAssignmentDelegateEntity::getTenantId, tenantId)
            .eq(SysAssignmentDelegateEntity::getAppCode, appCode)
            .and(wrapper -> wrapper
                .eq(SysAssignmentDelegateEntity::getGrantorSubjectModel, subjectModel)
                .eq(SysAssignmentDelegateEntity::getGrantorSubjectId, subjectId)
                .or()
                .eq(SysAssignmentDelegateEntity::getDelegateSubjectModel, subjectModel)
                .eq(SysAssignmentDelegateEntity::getDelegateSubjectId, subjectId))
            .count() > 0;
    }

    private boolean matchesKeyword(String keyword, String... values) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    private <T> PageResult<T> buildPage(List<T> records, int pageNo, int pageSize) {
        List<T> safeRecords = records == null ? Collections.<T>emptyList() : records;
        int safePageNo = pageNo > 0 ? pageNo : 1;
        int safePageSize = pageSize > 0 ? pageSize : 20;
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, safeRecords.size());
        int toIndex = Math.min(fromIndex + safePageSize, safeRecords.size());
        return PageResult.<T>builder()
            .pageNo(safePageNo)
            .pageSize(safePageSize)
            .total(safeRecords.size())
            .records(safeRecords.subList(fromIndex, toIndex))
            .build();
    }
}