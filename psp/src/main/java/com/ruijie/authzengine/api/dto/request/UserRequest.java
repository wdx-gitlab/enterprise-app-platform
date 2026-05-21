package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户目录写入请求，字段与 HCM 数据源完整对齐。
 */
@Data
@Schema(description = "用户目录写入请求")
public class UserRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @Schema(description = "员工工号", example = "R13174")
    private String staffNo;

    @Schema(description = "用户账号", example = "wangdaoxin")
    private String userId;

    @Schema(description = "公司展示名", example = "锐捷网络")
    private String staffCompanyName;

    @Schema(description = "员工姓名", example = "王道鑫")
    private String staffName;

    @Schema(description = "员工英文名", example = "Wang Daoxin")
    private String staffEnName;

    @Schema(description = "员工状态，1 在职", example = "1")
    private Integer staffStatus;

    @Schema(description = "证件号码")
    private String cardNum;

    @Schema(description = "证件类型编码")
    private String cardTypeCode;

    @Schema(description = "证件类型名称")
    private String cardType;

    @Schema(description = "工作邮箱", example = "wangdaoxin@ruijie.com.cn")
    private String staffEmail;

    @Schema(description = "员工类型编码")
    private String staffTypeCode;

    @Schema(description = "员工类型名称")
    private String staffType;

    @Schema(description = "员工照片 URL")
    private String staffPhoto;

    @Schema(description = "个人邮箱")
    private String personalEmail;

    @Schema(description = "个人手机号", example = "15972590712")
    private String personalMobile;

    @Schema(description = "入职日期")
    private LocalDateTime joinDate;

    @Schema(description = "入职岗位日期")
    private LocalDateTime joinJobDate;

    @Schema(description = "民族编码")
    private String nationCode;

    @Schema(description = "民族名称")
    private String nation;

    @Schema(description = "国家地区编码")
    private String countryAndAreaCode;

    @Schema(description = "国家地区名称")
    private String countryAndArea;

    @Schema(description = "工作地点编码")
    private String workPlaceCode;

    @Schema(description = "工作地点名称")
    private String workPlace;

    @Schema(description = "办公地点编码")
    private String officePlaceCode;

    @Schema(description = "办公地点名称")
    private String officePlace;

    @Schema(description = "出生日期")
    private LocalDateTime birthday;

    @Schema(description = "学历编码")
    private String educationalBackgroundCode;

    @Schema(description = "学历名称")
    private String educationalBackground;

    @Schema(description = "婚姻状态编码")
    private String marriageCode;

    @Schema(description = "婚姻状态名称")
    private String marriage;

    @Schema(description = "籍贯")
    private String nativePlace;

    @Schema(description = "性别编码")
    private String genderCode;

    @Schema(description = "性别名称")
    private String gender;

    @Schema(description = "实际转正日期")
    private LocalDateTime actualFormalDate;

    @Schema(description = "最后在职日期")
    private LocalDateTime lastWorkDate;

    @Schema(description = "直属上级用户账号")
    private String manageUserId;

    @Schema(description = "直属上级工号")
    private String manageStaffNo;

    @Schema(description = "直属上级姓名")
    private String manageStaffName;

    @Schema(description = "导师用户账号")
    private String tutorUserId;

    @Schema(description = "导师工号")
    private String tutorStaffNo;

    @Schema(description = "导师姓名")
    private String tutorStaffName;

    @Schema(description = "岗位编码")
    private String postCode;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "岗位类型编码")
    private String postTypeCode;

    @Schema(description = "岗位类型名称")
    private String postType;

    @Schema(description = "部门编码", example = "000023662002")
    private String departmentCode;

    @Schema(description = "部门名称", example = "技术架构部")
    private String departmentName;

    @Schema(description = "一级部门编码")
    private String oneDepartmentCode;

    @Schema(description = "二级部门编码")
    private String twoDepartmentCode;

    @Schema(description = "三级部门编码")
    private String threeDepartmentCode;

    @Schema(description = "四级部门编码")
    private String fourDepartmentCode;

    @Schema(description = "五级部门编码")
    private String fiveDepartmentCode;

    @Schema(description = "六级部门编码")
    private String sixDepartmentCode;

    @Schema(description = "七级部门编码")
    private String sevenDepartmentCode;

    @Schema(description = "八级部门编码")
    private String eightDepartmentCode;

    @Schema(description = "九级部门编码")
    private String nineDepartmentCode;

    @Schema(description = "十级部门编码")
    private String tenDepartmentCode;

    @Schema(description = "一级部门名称")
    private String oneDepartmentName;

    @Schema(description = "二级部门名称")
    private String twoDepartmentName;

    @Schema(description = "三级部门名称")
    private String threeDepartmentName;

    @Schema(description = "四级部门名称")
    private String fourDepartmentName;

    @Schema(description = "五级部门名称")
    private String fiveDepartmentName;

    @Schema(description = "六级部门名称")
    private String sixDepartmentName;

    @Schema(description = "七级部门名称")
    private String sevenDepartmentName;

    @Schema(description = "八级部门名称")
    private String eightDepartmentName;

    @Schema(description = "九级部门名称")
    private String nineDepartmentName;

    @Schema(description = "十级部门名称")
    private String tenDepartmentName;

    @Schema(description = "组织类型编码")
    private String organizationTypeCode;

    @Schema(description = "组织类型名称")
    private String organizationType;

    @Schema(description = "电脑类型编码")
    private String computerTypeCode;

    @Schema(description = "电脑类型名称")
    private String computerType;

    @Schema(description = "国家编码")
    private String countryCode;

    @Schema(description = "常驻国家地区名称")
    private String permanentCountryAndArea;

    @Schema(description = "常驻国家地区编码")
    private String permanentCountryAndAreaCode;

    @Schema(description = "外包公司名称")
    private String venderCompany;

    @Schema(description = "用工类型编码")
    private String employmentTypeCode;

    @Schema(description = "HCM 内部员工 ID")
    private Long empId;

    @Schema(description = "英文国家地区名称")
    private String englishCountryAndArea;

    @Schema(description = "英文常驻国家地区名称")
    private String englishPermanentCountryAndArea;

    @Schema(description = "支付主体编码")
    private String paySubjectCode;

    @Schema(description = "支付主体名称")
    private String paySubject;

    @Schema(description = "是否部门管理员")
    private Boolean isDeptManage;

    @Schema(description = "是否部门兼管")
    private Boolean isDeptPortionManage;

    @Schema(description = "员工管理权限范围")
    private String staffManagementJurisdiction;

    @Schema(description = "HCM 原始报文 JSON")
    private String hcmPayloadJson;

    @Schema(description = "组织编码", example = "ORG-SALES")
    private String orgCode;

    @Schema(description = "状态", example = "ENABLED")
    private String status;
}