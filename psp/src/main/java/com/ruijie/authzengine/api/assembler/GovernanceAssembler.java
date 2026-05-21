package com.ruijie.authzengine.api.assembler;

import com.ruijie.authzengine.api.dto.request.ApiResourceUpsertRequest;
import com.ruijie.authzengine.api.dto.request.BoMetaModelRegisterRequest;
import com.ruijie.authzengine.api.dto.request.MetaModelRegisterRequest;
import com.ruijie.authzengine.api.dto.request.UserUpsertRequest;
import com.ruijie.authzengine.api.dto.response.ApiResourceResponse;
import com.ruijie.authzengine.api.dto.response.BoMetaModelResponse;
import com.ruijie.authzengine.api.dto.response.MetaModelResponse;
import com.ruijie.authzengine.api.dto.response.OperationAckResponse;
import com.ruijie.authzengine.api.dto.response.StandardActionResponse;
import com.ruijie.authzengine.api.dto.response.StandardPolicyTemplateResponse;
import com.ruijie.authzengine.api.dto.response.UserResponse;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 治理接口装配器。
 */
@Component
public class GovernanceAssembler {

    /**
     * 元模型请求转领域对象。
     *
     * @param request 元模型请求
     * @return 领域对象
     */
    public AuthMetaModelDefinition toDefinition(MetaModelRegisterRequest request) {
        return AuthMetaModelDefinition.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .modelCode(request.getModelCode())
            .modelName(StringUtils.hasText(request.getModelName()) ? request.getModelName() : request.getModelCode())
            .category(request.getCategory())
            .adapterType(request.getAdapterType())
            .resolver(request.getResolver())
            .schemaView(request.getSchemaView())
            .build();
    }

    /**
     * 业务对象元模型请求转领域对象。
     *
     * @param request 业务对象元模型请求
     * @return 领域对象
     */
    public BoMetaModelDefinition toDefinition(BoMetaModelRegisterRequest request) {
        return BoMetaModelDefinition.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .boCode(request.getBoCode())
            .boName(request.getBoName())
            .schemaJson(request.getSchemaJson())
            .adapterType(request.getAdapterType())
            .resolver(request.getResolver())
            .build();
    }

    /**
     * 用户目录请求转领域对象。
     *
     * @param request 用户目录请求
     * @return 领域对象
     */
    public SysUserAccount toDefinition(UserUpsertRequest request) {
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

    public OperationAckResponse toAckResponse(String businessId) {
        return OperationAckResponse.builder()
            .accepted(true)
            .businessId(businessId)
            .note("请求已受理")
            .build();
    }

    /**
     * API 资源请求转领域对象。
     *
     * @param request API 资源请求
     * @return 领域对象
     */
    public SysResApi toDefinition(ApiResourceUpsertRequest request) {
        return SysResApi.builder()
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .apiCode(request.getApiCode())
            .apiName(StringUtils.hasText(request.getApiName()) ? request.getApiName() : request.getApiCode())
            .httpMethod(request.getHttpMethod())
            .uriPattern(request.getUriPattern())
            .status(request.getStatus())
            .build();
    }

    public MetaModelResponse toResponse(AuthMetaModelDefinition definition) {
        return MetaModelResponse.builder()
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .modelCode(definition.getModelCode())
            .modelName(definition.getModelName())
            .category(definition.getCategory())
            .build();
    }

    public BoMetaModelResponse toResponse(BoMetaModelDefinition definition) {
        return BoMetaModelResponse.builder()
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .boCode(definition.getBoCode())
            .boName(definition.getBoName())
            .build();
    }

    public List<StandardActionResponse> toActionResponses(List<StandardActionDefinition> definitions) {
        return definitions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<StandardPolicyTemplateResponse> toPolicyTemplateResponses(List<StandardPolicyTemplateDefinition> definitions) {
        return definitions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<UserResponse> toUserResponses(List<SysUserAccount> definitions) {
        return definitions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ApiResourceResponse> toApiResponses(List<SysResApi> definitions) {
        return definitions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private StandardActionResponse toResponse(StandardActionDefinition definition) {
        return StandardActionResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .actCode(definition.getActCode())
            .actName(definition.getActName())
            .actType(definition.getActType())
            .resCategory(definition.getResCategory())
            .riskLevel(definition.getRiskLevel())
            .build();
    }

    private StandardPolicyTemplateResponse toResponse(StandardPolicyTemplateDefinition definition) {
        return StandardPolicyTemplateResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .templateCode(definition.getTemplateCode())
            .templateName(definition.getTemplateName())
            .polType(definition.getPolType())
            .expressionScript(definition.getExpressionScript())
            .paramSchema(definition.getParamSchema())
            .status(definition.getStatus())
            .build();
    }

    private UserResponse toResponse(SysUserAccount definition) {
        return UserResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .staffNo(definition.getStaffNo())
            .userId(definition.getUserId())
            .staffCompanyName(definition.getStaffCompanyName())
            .staffName(definition.getStaffName())
            .staffStatus(definition.getStaffStatus())
            .departmentCode(definition.getDepartmentCode())
            .departmentName(definition.getDepartmentName())
            .staffEmail(definition.getStaffEmail())
            .personalMobile(definition.getPersonalMobile())
            .hcmPayloadJson(definition.getHcmPayloadJson())
            .orgCode(definition.getOrgCode())
            .status(definition.getStatus())
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

    private ApiResourceResponse toResponse(SysResApi definition) {
        return ApiResourceResponse.builder()
            .id(definition.getId())
            .tenantId(definition.getTenantId())
            .appCode(definition.getAppCode())
            .apiCode(definition.getApiCode())
            .apiName(definition.getApiName())
            .httpMethod(definition.getHttpMethod())
            .uriPattern(definition.getUriPattern())
            .status(definition.getStatus())
            .build();
    }
}
