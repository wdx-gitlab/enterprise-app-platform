package com.ruijie.authzengine.api.assembler;

import com.ruijie.authzengine.api.dto.request.PositionRequest;
import com.ruijie.authzengine.api.dto.request.RoleRequest;
import com.ruijie.authzengine.api.dto.request.SubjectRelationRequest;
import com.ruijie.authzengine.api.dto.request.SysOrgRequest;
import com.ruijie.authzengine.api.dto.request.UserGroupRequest;
import com.ruijie.authzengine.api.dto.request.UserRequest;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.api.dto.response.PositionResponse;
import com.ruijie.authzengine.api.dto.response.RoleResponse;
import com.ruijie.authzengine.api.dto.response.SubjectRelationResponse;
import com.ruijie.authzengine.api.dto.response.SysOrgResponse;
import com.ruijie.authzengine.api.dto.response.UserGroupResponse;
import com.ruijie.authzengine.api.dto.response.UserResponse;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.subject.AuthRole;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SysOrgNode;
import com.ruijie.authzengine.domain.model.governance.subject.SysPosition;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserGroup;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 主体目录装配器。
 */
@Component
public class SubjectAssembler {

    public SysOrgNode toDefinition(SysOrgRequest request) {
        return SysOrgNode.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .departmentCode(request.getDepartmentCode())
            .departmentName(request.getDepartmentName())
            .departmentEnName(request.getDepartmentEnName())
            .departmentLevel(request.getDepartmentLevel())
            .departmentTypeCode(request.getDepartmentTypeCode())
            .departmentType(request.getDepartmentType())
            .departmentCategory(request.getDepartmentCategory())
            .parentDepartmentCode(request.getParentDepartmentCode())
            .parentDepartmentName(request.getParentDepartmentName())
            .orgPath(request.getOrgPath())
            .manageUserId(request.getManageUserId())
            .manageStaffNo(request.getManageStaffNo())
            .manageName(request.getManageName())
            .portionManageUserId(request.getPortionManageUserId())
            .portionManageStaffNo(request.getPortionManageStaffNo())
            .portionManageName(request.getPortionManageName())
            .isEnable(request.getIsEnable())
            .createTime(request.getCreateTime())
            .departmentHrbpList(request.getDepartmentHrbpList())
            .hcmPayloadJson(request.getHcmPayloadJson())
            .status(request.getStatus())
            .build();
    }

    public SysUserAccount toDefinition(UserRequest request) {
        return SysUserAccount.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .staffNo(request.getStaffNo())
            .userId(request.getUserId())
            .staffCompanyName(request.getStaffCompanyName())
            .staffName(request.getStaffName())
            .staffEnName(request.getStaffEnName())
            .staffStatus(request.getStaffStatus())
            .cardNum(request.getCardNum())
            .cardTypeCode(request.getCardTypeCode())
            .cardType(request.getCardType())
            .staffEmail(request.getStaffEmail())
            .staffTypeCode(request.getStaffTypeCode())
            .staffType(request.getStaffType())
            .staffPhoto(request.getStaffPhoto())
            .personalEmail(request.getPersonalEmail())
            .personalMobile(request.getPersonalMobile())
            .joinDate(request.getJoinDate())
            .joinJobDate(request.getJoinJobDate())
            .nationCode(request.getNationCode())
            .nation(request.getNation())
            .countryAndAreaCode(request.getCountryAndAreaCode())
            .countryAndArea(request.getCountryAndArea())
            .workPlaceCode(request.getWorkPlaceCode())
            .workPlace(request.getWorkPlace())
            .officePlaceCode(request.getOfficePlaceCode())
            .officePlace(request.getOfficePlace())
            .birthday(request.getBirthday())
            .educationalBackgroundCode(request.getEducationalBackgroundCode())
            .educationalBackground(request.getEducationalBackground())
            .marriageCode(request.getMarriageCode())
            .marriage(request.getMarriage())
            .nativePlace(request.getNativePlace())
            .genderCode(request.getGenderCode())
            .gender(request.getGender())
            .actualFormalDate(request.getActualFormalDate())
            .lastWorkDate(request.getLastWorkDate())
            .manageUserId(request.getManageUserId())
            .manageStaffNo(request.getManageStaffNo())
            .manageStaffName(request.getManageStaffName())
            .tutorUserId(request.getTutorUserId())
            .tutorStaffNo(request.getTutorStaffNo())
            .tutorStaffName(request.getTutorStaffName())
            .postCode(request.getPostCode())
            .postName(request.getPostName())
            .postTypeCode(request.getPostTypeCode())
            .postType(request.getPostType())
            .departmentCode(request.getDepartmentCode())
            .departmentName(request.getDepartmentName())
            .oneDepartmentCode(request.getOneDepartmentCode())
            .twoDepartmentCode(request.getTwoDepartmentCode())
            .threeDepartmentCode(request.getThreeDepartmentCode())
            .fourDepartmentCode(request.getFourDepartmentCode())
            .fiveDepartmentCode(request.getFiveDepartmentCode())
            .sixDepartmentCode(request.getSixDepartmentCode())
            .sevenDepartmentCode(request.getSevenDepartmentCode())
            .eightDepartmentCode(request.getEightDepartmentCode())
            .nineDepartmentCode(request.getNineDepartmentCode())
            .tenDepartmentCode(request.getTenDepartmentCode())
            .oneDepartmentName(request.getOneDepartmentName())
            .twoDepartmentName(request.getTwoDepartmentName())
            .threeDepartmentName(request.getThreeDepartmentName())
            .fourDepartmentName(request.getFourDepartmentName())
            .fiveDepartmentName(request.getFiveDepartmentName())
            .sixDepartmentName(request.getSixDepartmentName())
            .sevenDepartmentName(request.getSevenDepartmentName())
            .eightDepartmentName(request.getEightDepartmentName())
            .nineDepartmentName(request.getNineDepartmentName())
            .tenDepartmentName(request.getTenDepartmentName())
            .organizationTypeCode(request.getOrganizationTypeCode())
            .organizationType(request.getOrganizationType())
            .computerTypeCode(request.getComputerTypeCode())
            .computerType(request.getComputerType())
            .countryCode(request.getCountryCode())
            .permanentCountryAndArea(request.getPermanentCountryAndArea())
            .permanentCountryAndAreaCode(request.getPermanentCountryAndAreaCode())
            .venderCompany(request.getVenderCompany())
            .employmentTypeCode(request.getEmploymentTypeCode())
            .empId(request.getEmpId())
            .englishCountryAndArea(request.getEnglishCountryAndArea())
            .englishPermanentCountryAndArea(request.getEnglishPermanentCountryAndArea())
            .paySubjectCode(request.getPaySubjectCode())
            .paySubject(request.getPaySubject())
            .isDeptManage(request.getIsDeptManage())
            .isDeptPortionManage(request.getIsDeptPortionManage())
            .staffManagementJurisdiction(request.getStaffManagementJurisdiction())
            .hcmPayloadJson(request.getHcmPayloadJson())
            .orgCode(request.getOrgCode())
            .status(request.getStatus())
            .build();
    }

    public SysPosition toDefinition(PositionRequest request) {
        return SysPosition.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .positionCode(request.getPositionCode())
            .positionName(request.getPositionName())
            .orgCode(request.getOrgCode())
            .status(request.getStatus())
            .build();
    }

    public SysUserGroup toDefinition(UserGroupRequest request) {
        return SysUserGroup.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .groupCode(request.getGroupCode())
            .groupName(request.getGroupName())
            .status(request.getStatus())
            .build();
    }

    public AuthRole toDefinition(RoleRequest request) {
        return AuthRole.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .roleCode(request.getRoleCode())
            .roleName(request.getRoleName())
            .roleScope(request.getRoleScope())
            .status(request.getStatus())
            .build();
    }

    public AuthSubjectRelation toDefinition(SubjectRelationRequest request) {
        return AuthSubjectRelation.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .subjectModel(request.getSubjectModel())
            .subjectId(request.getSubjectId())
            .relatedSubjectModel(request.getRelatedSubjectModel())
            .relatedSubjectId(request.getRelatedSubjectId())
            .relationType(request.getRelationType())
            .build();
    }

    public SysOrgResponse toResponse(SysOrgNode definition) {
        return SysOrgResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .departmentCode(definition.getDepartmentCode())
            .departmentName(definition.getDepartmentName())
            .departmentEnName(definition.getDepartmentEnName())
            .departmentLevel(definition.getDepartmentLevel())
            .departmentTypeCode(definition.getDepartmentTypeCode())
            .departmentType(definition.getDepartmentType())
            .departmentCategory(definition.getDepartmentCategory())
            .parentDepartmentCode(definition.getParentDepartmentCode())
            .parentDepartmentName(definition.getParentDepartmentName())
            .orgPath(definition.getOrgPath())
            .manageUserId(definition.getManageUserId())
            .manageStaffNo(definition.getManageStaffNo())
            .manageName(definition.getManageName())
            .portionManageUserId(definition.getPortionManageUserId())
            .portionManageStaffNo(definition.getPortionManageStaffNo())
            .portionManageName(definition.getPortionManageName())
            .isEnable(definition.getIsEnable())
            .createTime(definition.getCreateTime())
            .departmentHrbpList(definition.getDepartmentHrbpList())
            .hcmPayloadJson(definition.getHcmPayloadJson())
            .status(definition.getStatus())
            .attributes(definition.getAttributes())
            .build();
    }

    public UserResponse toResponse(SysUserAccount definition) {
        return UserResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .staffNo(definition.getStaffNo())
            .userId(definition.getUserId())
            .staffCompanyName(definition.getStaffCompanyName())
            .staffName(definition.getStaffName())
            .staffEnName(definition.getStaffEnName())
            .staffStatus(definition.getStaffStatus())
            .cardNum(definition.getCardNum())
            .cardTypeCode(definition.getCardTypeCode())
            .cardType(definition.getCardType())
            .staffEmail(definition.getStaffEmail())
            .staffTypeCode(definition.getStaffTypeCode())
            .staffType(definition.getStaffType())
            .staffPhoto(definition.getStaffPhoto())
            .personalEmail(definition.getPersonalEmail())
            .personalMobile(definition.getPersonalMobile())
            .joinDate(definition.getJoinDate())
            .joinJobDate(definition.getJoinJobDate())
            .nationCode(definition.getNationCode())
            .nation(definition.getNation())
            .countryAndAreaCode(definition.getCountryAndAreaCode())
            .countryAndArea(definition.getCountryAndArea())
            .workPlaceCode(definition.getWorkPlaceCode())
            .workPlace(definition.getWorkPlace())
            .officePlaceCode(definition.getOfficePlaceCode())
            .officePlace(definition.getOfficePlace())
            .birthday(definition.getBirthday())
            .educationalBackgroundCode(definition.getEducationalBackgroundCode())
            .educationalBackground(definition.getEducationalBackground())
            .marriageCode(definition.getMarriageCode())
            .marriage(definition.getMarriage())
            .nativePlace(definition.getNativePlace())
            .genderCode(definition.getGenderCode())
            .gender(definition.getGender())
            .actualFormalDate(definition.getActualFormalDate())
            .lastWorkDate(definition.getLastWorkDate())
            .manageUserId(definition.getManageUserId())
            .manageStaffNo(definition.getManageStaffNo())
            .manageStaffName(definition.getManageStaffName())
            .tutorUserId(definition.getTutorUserId())
            .tutorStaffNo(definition.getTutorStaffNo())
            .tutorStaffName(definition.getTutorStaffName())
            .postCode(definition.getPostCode())
            .postName(definition.getPostName())
            .postTypeCode(definition.getPostTypeCode())
            .postType(definition.getPostType())
            .departmentCode(definition.getDepartmentCode())
            .departmentName(definition.getDepartmentName())
            .oneDepartmentCode(definition.getOneDepartmentCode())
            .twoDepartmentCode(definition.getTwoDepartmentCode())
            .threeDepartmentCode(definition.getThreeDepartmentCode())
            .fourDepartmentCode(definition.getFourDepartmentCode())
            .fiveDepartmentCode(definition.getFiveDepartmentCode())
            .sixDepartmentCode(definition.getSixDepartmentCode())
            .sevenDepartmentCode(definition.getSevenDepartmentCode())
            .eightDepartmentCode(definition.getEightDepartmentCode())
            .nineDepartmentCode(definition.getNineDepartmentCode())
            .tenDepartmentCode(definition.getTenDepartmentCode())
            .oneDepartmentName(definition.getOneDepartmentName())
            .twoDepartmentName(definition.getTwoDepartmentName())
            .threeDepartmentName(definition.getThreeDepartmentName())
            .fourDepartmentName(definition.getFourDepartmentName())
            .fiveDepartmentName(definition.getFiveDepartmentName())
            .sixDepartmentName(definition.getSixDepartmentName())
            .sevenDepartmentName(definition.getSevenDepartmentName())
            .eightDepartmentName(definition.getEightDepartmentName())
            .nineDepartmentName(definition.getNineDepartmentName())
            .tenDepartmentName(definition.getTenDepartmentName())
            .organizationTypeCode(definition.getOrganizationTypeCode())
            .organizationType(definition.getOrganizationType())
            .computerTypeCode(definition.getComputerTypeCode())
            .computerType(definition.getComputerType())
            .countryCode(definition.getCountryCode())
            .permanentCountryAndArea(definition.getPermanentCountryAndArea())
            .permanentCountryAndAreaCode(definition.getPermanentCountryAndAreaCode())
            .venderCompany(definition.getVenderCompany())
            .employmentTypeCode(definition.getEmploymentTypeCode())
            .empId(definition.getEmpId())
            .englishCountryAndArea(definition.getEnglishCountryAndArea())
            .englishPermanentCountryAndArea(definition.getEnglishPermanentCountryAndArea())
            .paySubjectCode(definition.getPaySubjectCode())
            .paySubject(definition.getPaySubject())
            .isDeptManage(definition.getIsDeptManage())
            .isDeptPortionManage(definition.getIsDeptPortionManage())
            .staffManagementJurisdiction(definition.getStaffManagementJurisdiction())
            .hcmPayloadJson(definition.getHcmPayloadJson())
            .orgCode(definition.getOrgCode())
            .status(definition.getStatus())
            .attributes(definition.getAttributes())
            .build();
    }

    public PositionResponse toResponse(SysPosition definition) {
        return PositionResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .positionCode(definition.getPositionCode())
            .positionName(definition.getPositionName())
            .orgCode(definition.getOrgCode())
            .status(definition.getStatus())
            .attributes(definition.getAttributes())
            .build();
    }

    public UserGroupResponse toResponse(SysUserGroup definition) {
        return UserGroupResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .groupCode(definition.getGroupCode())
            .groupName(definition.getGroupName())
            .status(definition.getStatus())
            .attributes(definition.getAttributes())
            .build();
    }

    public RoleResponse toResponse(AuthRole definition) {
        return RoleResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .roleCode(definition.getRoleCode())
            .roleName(definition.getRoleName())
            .roleScope(definition.getRoleScope())
            .status(definition.getStatus())
            .attributes(definition.getAttributes())
            .build();
    }

    public SubjectRelationResponse toResponse(AuthSubjectRelation definition) {
        return SubjectRelationResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .subjectModel(definition.getSubjectModel())
            .subjectId(definition.getSubjectId())
            .relatedSubjectModel(definition.getRelatedSubjectModel())
            .relatedSubjectId(definition.getRelatedSubjectId())
            .relationType(definition.getRelationType())
            .build();
    }

    /**
     * 转换分页结果。
     *
     * @param pageResult 领域分页结果
     * @param mapper 记录转换函数
     * @param <T> 领域记录类型
     * @param <R> 响应记录类型
     * @return 接口分页响应
     */
    public <T, R> PageResponse<R> toPageResponse(PageResult<T> pageResult, Function<T, R> mapper) {
        List<R> records = pageResult.getRecords().stream().map(mapper).collect(Collectors.toList());
        return PageResponse.<R>builder()
            .pageNo(pageResult.getPageNo())
            .pageSize(pageResult.getPageSize())
            .total(pageResult.getTotal())
            .records(records)
            .build();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}