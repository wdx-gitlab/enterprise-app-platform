package com.ruijie.dapengine.common.model;

import lombok.Data;

import java.io.Serializable;

/**
 * OSDS 员工扩展信息。
 */
@Data
public class StaffExtendInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String staffNo;
    private String userId;
    private String staffCompanyName;
    private String staffName;
    private String staffEnName;
    private Integer staffStatus;
    private String cardNum;
    private String cardTypeCode;
    private String cardType;
    private String staffEmail;
    private String staffTypeCode;
    private String staffType;
    private String staffPhoto;
    private String personalEmail;
    private String personalMobile;
    private String joinDate;
    private String joinJobDate;
    private String nationCode;
    private String nation;
    private String countryAndAreaCode;
    private String countryAndArea;
    private String workPlaceCode;
    private String workPlace;
    private String officePlaceCode;
    private String officePlace;
    private String birthday;
    private String educationalBackgroundCode;
    private String educationalBackground;
    private String marriageCode;
    private String marriage;
    private String nativePlace;
    private String genderCode;
    private String gender;
    private String actualFormalDate;
    private String lastWorkDate;
    private String manageUserId;
    private String manageStaffNo;
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
    private Integer empId;
    private String englishCountryAndArea;
    private String englishPermanentCountryAndArea;
    private String paySubjectCode;
    private String paySubject;
    private String manageStaffName;
    private Boolean isDeptManage;
    private Boolean isDeptPortionManage;
    private String staffManagementJurisidiction;
}

