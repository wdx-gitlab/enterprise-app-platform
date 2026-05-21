package com.ruijie.authzengine.domain.model.governance.subject;

import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户目录定义，对应 sys_user 表的一行记录，字段与 HCM 数据源完整对齐。
 * <p>
 * 业务唯一标识为 staffNo，常用查询键为 userId。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUserAccount {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 员工工号，业务唯一标识。 */
    private String staffNo;

    /** 用户账号，常用查询键。 */
    private String userId;

    /** 公司展示名。 */
    private String staffCompanyName;

    /** 员工姓名。 */
    private String staffName;

    /** 员工英文名。 */
    private String staffEnName;

    /** 员工状态，1 在职，其他离职。 */
    private Integer staffStatus;

    /** 证件号码。 */
    private String cardNum;

    /** 证件类型编码。 */
    private String cardTypeCode;

    /** 证件类型名称。 */
    private String cardType;

    /** 工作邮箱。 */
    private String staffEmail;

    /** 员工类型编码。 */
    private String staffTypeCode;

    /** 员工类型名称。 */
    private String staffType;

    /** 员工照片。 */
    private String staffPhoto;

    /** 个人邮箱。 */
    private String personalEmail;

    /** 个人手机号。 */
    private String personalMobile;

    /** 入职日期。 */
    private LocalDateTime joinDate;

    /** 入职岗位日期。 */
    private LocalDateTime joinJobDate;

    /** 民族编码。 */
    private String nationCode;

    /** 民族名称。 */
    private String nation;

    /** 国家地区编码。 */
    private String countryAndAreaCode;

    /** 国家地区名称。 */
    private String countryAndArea;

    /** 工作地点编码。 */
    private String workPlaceCode;

    /** 工作地点名称。 */
    private String workPlace;

    /** 办公地点编码。 */
    private String officePlaceCode;

    /** 办公地点名称。 */
    private String officePlace;

    /** 出生日期。 */
    private LocalDateTime birthday;

    /** 学历编码。 */
    private String educationalBackgroundCode;

    /** 学历名称。 */
    private String educationalBackground;

    /** 婚姻状态编码。 */
    private String marriageCode;

    /** 婚姻状态名称。 */
    private String marriage;

    /** 籍贯。 */
    private String nativePlace;

    /** 性别编码。 */
    private String genderCode;

    /** 性别名称。 */
    private String gender;

    /** 实际转正日期。 */
    private LocalDateTime actualFormalDate;

    /** 最后在职日期。 */
    private LocalDateTime lastWorkDate;

    /** 直属上级用户账号。 */
    private String manageUserId;

    /** 直属上级工号。 */
    private String manageStaffNo;

    /** 直属上级姓名。 */
    private String manageStaffName;

    /** 导师用户账号。 */
    private String tutorUserId;

    /** 导师工号。 */
    private String tutorStaffNo;

    /** 导师姓名。 */
    private String tutorStaffName;

    /** 岗位编码。 */
    private String postCode;

    /** 岗位名称。 */
    private String postName;

    /** 岗位类型编码。 */
    private String postTypeCode;

    /** 岗位类型名称。 */
    private String postType;

    /** 部门编码，用于关联 sys_org.org_code。 */
    private String departmentCode;

    /** 部门名称。 */
    private String departmentName;

    /** 一级部门编码。 */
    private String oneDepartmentCode;

    /** 二级部门编码。 */
    private String twoDepartmentCode;

    /** 三级部门编码。 */
    private String threeDepartmentCode;

    /** 四级部门编码。 */
    private String fourDepartmentCode;

    /** 五级部门编码。 */
    private String fiveDepartmentCode;

    /** 六级部门编码。 */
    private String sixDepartmentCode;

    /** 七级部门编码。 */
    private String sevenDepartmentCode;

    /** 八级部门编码。 */
    private String eightDepartmentCode;

    /** 九级部门编码。 */
    private String nineDepartmentCode;

    /** 十级部门编码。 */
    private String tenDepartmentCode;

    /** 一级部门名称。 */
    private String oneDepartmentName;

    /** 二级部门名称。 */
    private String twoDepartmentName;

    /** 三级部门名称。 */
    private String threeDepartmentName;

    /** 四级部门名称。 */
    private String fourDepartmentName;

    /** 五级部门名称。 */
    private String fiveDepartmentName;

    /** 六级部门名称。 */
    private String sixDepartmentName;

    /** 七级部门名称。 */
    private String sevenDepartmentName;

    /** 八级部门名称。 */
    private String eightDepartmentName;

    /** 九级部门名称。 */
    private String nineDepartmentName;

    /** 十级部门名称。 */
    private String tenDepartmentName;

    /** 组织类型编码。 */
    private String organizationTypeCode;

    /** 组织类型名称。 */
    private String organizationType;

    /** 电脑类型编码。 */
    private String computerTypeCode;

    /** 电脑类型名称。 */
    private String computerType;

    /** 国家编码。 */
    private String countryCode;

    /** 常驻国家地区名称。 */
    private String permanentCountryAndArea;

    /** 常驻国家地区编码。 */
    private String permanentCountryAndAreaCode;

    /** 外包公司名称。 */
    private String venderCompany;

    /** 用工类型编码。 */
    private String employmentTypeCode;

    /** HCM 内部员工 ID。 */
    private Long empId;

    /** 英文国家地区名称。 */
    private String englishCountryAndArea;

    /** 英文常驻国家地区名称。 */
    private String englishPermanentCountryAndArea;

    /** 支付主体编码。 */
    private String paySubjectCode;

    /** 支付主体名称。 */
    private String paySubject;

    /** 是否部门管理员。 */
    private Boolean isDeptManage;

    /** 是否部门兼管。 */
    private Boolean isDeptPortionManage;

    /** 员工管理权限范围。 */
    private String staffManagementJurisdiction;

    /** HCM 原始报文 JSON。 */
    private String hcmPayloadJson;

    /** 所属组织编码，关联 SysOrgNode。 */
    private String orgCode;

    /** 状态：ENABLED / DISABLED。 */
    private String status;

    /** 宿主系统透传的动态扩展属性。 */
    @TableField(exist = false)
    private Map<String, Object> attributes;
}