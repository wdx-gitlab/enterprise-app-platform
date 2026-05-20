package com.ruijie.dapengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * dap_sys_user 表持久化实体。
 */
@Data
@TableName("dap_sys_user")
public class DapSysUserEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String tenantId;
    private String appCode;
    private String code;
    private String name;
    private String parentCode;
    private Long dapVersion;
    private LocalDateTime dapSyncTime;
    private Integer isDelete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long orgId;
    private String staffNo;
    private String userId;
    private String staffCompanyName;
    private String staffName;
    private String staffEnName;
    private Long staffStatus;
    private String cardNum;
    private String cardTypeCode;
    private String cardType;
    private String staffEmail;
    private String staffTypeCode;
    private String staffType;
    private String staffPhoto;
    private String personalEmail;
    private String personalMobile;
    private LocalDateTime joinDate;
    private LocalDateTime joinJobDate;
    private String nationCode;
    private String nation;
    private String countryAndAreaCode;
    private String countryAndArea;
    private String workPlaceCode;
    private String workPlace;
    private String officePlaceCode;
    private String officePlace;
    private LocalDateTime birthday;
    private String educationalBackgroundCode;
    private String educationalBackground;
    private String marriageCode;
    private String marriage;
    private String nativePlace;
    private String genderCode;
    private String gender;
    private LocalDateTime actualFormalDate;
    private LocalDateTime lastWorkDate;
    private String manageUserId;
    private String manageStaffNo;
    private String manageStaffName;
    private String tutorUserId;
    private String tutorStaffNo;
    private String tutorStaffName;
    private String postCode;
    private String postName;
    private String postTypeCode;
    private String postType;
    private String departmentCode;
    private String departmentName;
    private String oneDepartmentCode;
    private String twoDepartmentCode;
    private String threeDepartmentCode;
    private String fourDepartmentCode;
    private String fiveDepartmentCode;
    private String sixDepartmentCode;
    private String sevenDepartmentCode;
    private String eightDepartmentCode;
    private String nineDepartmentCode;
    private String tenDepartmentCode;
    private String oneDepartmentName;
    private String twoDepartmentName;
    private String threeDepartmentName;
    private String fourDepartmentName;
    private String fiveDepartmentName;
    private String sixDepartmentName;
    private String sevenDepartmentName;
    private String eightDepartmentName;
    private String nineDepartmentName;
    private String tenDepartmentName;
    private String organizationTypeCode;
    private String organizationType;
    private String computerTypeCode;
    private String computerType;
    private String countryCode;
    private String permanentCountryAndArea;
    private String permanentCountryAndAreaCode;
    private String venderCompany;
    private String employmentTypeCode;
    private Long empId;
    private String englishCountryAndArea;
    private String englishPermanentCountryAndArea;
    private String paySubjectCode;
    private String paySubject;
    private Long isDeptManage;
    private Long isDeptPortionManage;
    private String staffManagementJurisdiction;
    private String hcmPayloadJson;
}

