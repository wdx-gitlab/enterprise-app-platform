package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 组织目录持久化实体。
 */
@Data
@TableName("dap_sys_org")
@EqualsAndHashCode
public class SysOrgEntity {
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    // HCM 标识与名称
    @TableField("department_code")
    private String departmentCode;

    @TableField("department_name")
    private String departmentName;

    @TableField("department_en_name")
    private String departmentEnName;

    // 层级与类型
    @TableField("department_level")
    private Integer departmentLevel;

    @TableField("department_type_code")
    private String departmentTypeCode;

    @TableField("department_type")
    private String departmentType;

    @TableField("department_category")
    private String departmentCategory;

    // 父组织（引擎 FK + HCM 来源）
    @TableField("parent_org_id")
    private Long parentOrgId;

    @TableField("parent_department_code")
    private String parentDepartmentCode;

    @TableField("parent_department_name")
    private String parentDepartmentName;

    @TableField("org_path")
    private String orgPath;

    // 负责人
    @TableField("manage_user_id")
    private String manageUserId;

    @TableField("manage_staff_no")
    private String manageStaffNo;

    @TableField("manage_name")
    private String manageName;

    @TableField("portion_manage_user_id")
    private String portionManageUserId;

    @TableField("portion_manage_staff_no")
    private String portionManageStaffNo;

    @TableField("portion_manage_name")
    private String portionManageName;

    // HCM 其他字段
    @TableField("is_enable")
    private Integer isEnable;

    @TableField("create_time")
    private LocalDateTime createTime;

    /** departmentHrbpList，以 JSON 字符串存储。 */
    @TableField("department_hrbp_list")
    private String departmentHrbpList;

    /** HCM 原始报文备份。 */
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