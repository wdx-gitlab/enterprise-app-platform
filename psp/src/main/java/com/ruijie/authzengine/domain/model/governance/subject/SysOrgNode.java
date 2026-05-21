package com.ruijie.authzengine.domain.model.governance.subject;

import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 组织目录定义，对应 sys_org 表的一行记录。
 * <p>
 * 作为 SUB_ORG 类型主数据，支持树形结构（通过 parentDepartmentCode / orgPath）。
 * 用户通过 authz_subject_relation (relation_type=ORG) 关联到组织，
 * PIP 补全时将组织标识加入 SubjectKey 集合。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysOrgNode {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    // HCM 标识与名称
    /** 组织编码，唯一约束 uk_dap_sys_org(tenant_id, app_code, department_code)。 */
    private String departmentCode;

    /** 组织名称。 */
    private String departmentName;

    /** 组织英文名称。 */
    private String departmentEnName;

    // 层级与类型
    /** 组织层级。 */
    private Integer departmentLevel;

    /** 组织类型编码。 */
    private String departmentTypeCode;

    /** 组织类型。 */
    private String departmentType;

    /** 组织分类。 */
    private String departmentCategory;

    // 父组织
    /** 父组织编码，根节点为 null。 */
    private String parentDepartmentCode;

    /** 父组织名称。 */
    private String parentDepartmentName;

    /** 组织路径，如 /ORG-ROOT/DEPT-A，用于快速判断祖先关系。 */
    private String orgPath;

    // 负责人
    /** 负责人用户 ID。 */
    private String manageUserId;

    /** 负责人工号。 */
    private String manageStaffNo;

    /** 负责人姓名。 */
    private String manageName;

    /** 分管负责人用户 ID。 */
    private String portionManageUserId;

    /** 分管负责人工号。 */
    private String portionManageStaffNo;

    /** 分管负责人姓名。 */
    private String portionManageName;

    // HCM 其他字段
    /** 是否启用（1）/ 停用（0）。 */
    private Integer isEnable;

    /** HCM 来源创建时间。 */
    private LocalDateTime createTime;

    /** HRBP 列表，JSON 字符串。 */
    private String departmentHrbpList;

    /** HCM 原始完整报文备份。 */
    private String hcmPayloadJson;

    /** 状态：ENABLED / DISABLED。 */
    private String status;

    /** 宿主系统透传的动态扩展属性。 */
    @TableField(exist = false)
    private Map<String, Object> attributes;
}