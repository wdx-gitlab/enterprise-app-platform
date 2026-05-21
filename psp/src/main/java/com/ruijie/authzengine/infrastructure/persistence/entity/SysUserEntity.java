package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户目录持久化实体，字段与 dap_sys_user 表完整对齐。
 */
@Data
@TableName("dap_sys_user")
@EqualsAndHashCode
public class SysUserEntity {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("org_id")
    private Long orgId;

    /** 员工工号，业务唯一标识。 */
    @TableField("staff_no")
    private String staffNo;

    /** 用户账号，常用查询键。 */
    @TableField("user_id")
    private String userId;

    @TableField("staff_company_name")
    private String staffCompanyName;

    @TableField("staff_name")
    private String staffName;

    @TableField("staff_en_name")
    private String staffEnName;

    @TableField("staff_status")
    private Integer staffStatus;

    @TableField("card_num")
    private String cardNum;

    @TableField("card_type_code")
    private String cardTypeCode;

    @TableField("card_type")
    private String cardType;

    @TableField("staff_email")
    private String staffEmail;

    @TableField("staff_type_code")
    private String staffTypeCode;

    @TableField("staff_type")
    private String staffType;

    @TableField("staff_photo")
    private String staffPhoto;

    @TableField("personal_email")
    private String personalEmail;

    @TableField("personal_mobile")
    private String personalMobile;

    @TableField("join_date")
    private LocalDateTime joinDate;

    @TableField("join_job_date")
    private LocalDateTime joinJobDate;

    @TableField("nation_code")
    private String nationCode;

    @TableField("nation")
    private String nation;

    @TableField("country_and_area_code")
    private String countryAndAreaCode;

    @TableField("country_and_area")
    private String countryAndArea;

    @TableField("work_place_code")
    private String workPlaceCode;

    @TableField("work_place")
    private String workPlace;

    @TableField("office_place_code")
    private String officePlaceCode;

    @TableField("office_place")
    private String officePlace;

    @TableField("birthday")
    private LocalDateTime birthday;

    @TableField("educational_background_code")
    private String educationalBackgroundCode;

    @TableField("educational_background")
    private String educationalBackground;

    @TableField("marriage_code")
    private String marriageCode;

    @TableField("marriage")
    private String marriage;

    @TableField("native_place")
    private String nativePlace;

    @TableField("gender_code")
    private String genderCode;

    @TableField("gender")
    private String gender;

    @TableField("actual_formal_date")
    private LocalDateTime actualFormalDate;

    @TableField("last_work_date")
    private LocalDateTime lastWorkDate;

    @TableField("manage_user_id")
    private String manageUserId;

    @TableField("manage_staff_no")
    private String manageStaffNo;

    @TableField("manage_staff_name")
    private String manageStaffName;

    @TableField("tutor_user_id")
    private String tutorUserId;

    @TableField("tutor_staff_no")
    private String tutorStaffNo;

    @TableField("tutor_staff_name")
    private String tutorStaffName;

    @TableField("post_code")
    private String postCode;

    @TableField("post_name")
    private String postName;

    @TableField("post_type_code")
    private String postTypeCode;

    @TableField("post_type")
    private String postType;

    @TableField("department_code")
    private String departmentCode;

    @TableField("department_name")
    private String departmentName;

    @TableField("one_department_code")
    private String oneDepartmentCode;

    @TableField("two_department_code")
    private String twoDepartmentCode;

    @TableField("three_department_code")
    private String threeDepartmentCode;

    @TableField("four_department_code")
    private String fourDepartmentCode;

    @TableField("five_department_code")
    private String fiveDepartmentCode;

    @TableField("six_department_code")
    private String sixDepartmentCode;

    @TableField("seven_department_code")
    private String sevenDepartmentCode;

    @TableField("eight_department_code")
    private String eightDepartmentCode;

    @TableField("nine_department_code")
    private String nineDepartmentCode;

    @TableField("ten_department_code")
    private String tenDepartmentCode;

    @TableField("one_department_name")
    private String oneDepartmentName;

    @TableField("two_department_name")
    private String twoDepartmentName;

    @TableField("three_department_name")
    private String threeDepartmentName;

    @TableField("four_department_name")
    private String fourDepartmentName;

    @TableField("five_department_name")
    private String fiveDepartmentName;

    @TableField("six_department_name")
    private String sixDepartmentName;

    @TableField("seven_department_name")
    private String sevenDepartmentName;

    @TableField("eight_department_name")
    private String eightDepartmentName;

    @TableField("nine_department_name")
    private String nineDepartmentName;

    @TableField("ten_department_name")
    private String tenDepartmentName;

    @TableField("organization_type_code")
    private String organizationTypeCode;

    @TableField("organization_type")
    private String organizationType;

    @TableField("computer_type_code")
    private String computerTypeCode;

    @TableField("computer_type")
    private String computerType;

    @TableField("country_code")
    private String countryCode;

    @TableField("permanent_country_and_area")
    private String permanentCountryAndArea;

    @TableField("permanent_country_and_area_code")
    private String permanentCountryAndAreaCode;

    @TableField("vender_company")
    private String venderCompany;

    @TableField("employment_type_code")
    private String employmentTypeCode;

    @TableField("emp_id")
    private Long empId;

    @TableField("english_country_and_area")
    private String englishCountryAndArea;

    @TableField("english_permanent_country_and_area")
    private String englishPermanentCountryAndArea;

    @TableField("pay_subject_code")
    private String paySubjectCode;

    @TableField("pay_subject")
    private String paySubject;

    @TableField("is_dept_manage")
    private Boolean isDeptManage;

    @TableField("is_dept_portion_manage")
    private Boolean isDeptPortionManage;

    @TableField("staff_management_jurisdiction")
    private String staffManagementJurisdiction;

    @TableField("hcm_payload_json")
    private String hcmPayloadJson;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人")
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人")
    private String updatedBy;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "逻辑删除标记")
    private Integer isDelete;
}