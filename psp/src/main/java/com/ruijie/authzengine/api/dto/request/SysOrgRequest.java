package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 组织目录写入请求。
 */
@Data
@Schema(description = "组织目录写入请求")
public class SysOrgRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "组织编码不能为空")
    @Schema(description = "组织编码", example = "ORG-SALES")
    private String departmentCode;

    @NotBlank(message = "组织名称不能为空")
    @Schema(description = "组织名称", example = "销售组织")
    private String departmentName;

    @Schema(description = "组织英文名称")
    private String departmentEnName;

    @Schema(description = "组织层级")
    private Integer departmentLevel;

    @Schema(description = "组织类型编码")
    private String departmentTypeCode;

    @Schema(description = "组织类型")
    private String departmentType;

    @Schema(description = "组织分类")
    private String departmentCategory;

    @Schema(description = "父组织编码", example = "ORG-ROOT")
    private String parentDepartmentCode;

    @Schema(description = "父组织名称")
    private String parentDepartmentName;

    @Schema(description = "组织路径", example = "/ORG-ROOT/ORG-SALES")
    private String orgPath;

    @Schema(description = "负责人用户 ID")
    private String manageUserId;

    @Schema(description = "负责人工号")
    private String manageStaffNo;

    @Schema(description = "负责人姓名")
    private String manageName;

    @Schema(description = "分管负责人用户 ID")
    private String portionManageUserId;

    @Schema(description = "分管负责人工号")
    private String portionManageStaffNo;

    @Schema(description = "分管负责人姓名")
    private String portionManageName;

    @Schema(description = "是否启用，1-启用 0-停用")
    private Integer isEnable;

    @Schema(description = "HCM 来源创建时间")
    private LocalDateTime createTime;

    @Schema(description = "HRBP 列表（JSON 数组字符串）")
    private String departmentHrbpList;

    @Schema(description = "HCM 原始报文")
    private String hcmPayloadJson;

    @Schema(description = "状态", example = "ENABLED")
    private String status;
}