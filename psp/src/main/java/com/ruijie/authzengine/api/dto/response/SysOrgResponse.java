package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * 组织目录响应。
 */
@Data
@Builder
@Schema(description = "组织目录响应")
public class SysOrgResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", example = "1001")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "组织编码")
    private String departmentCode;

    @Schema(description = "组织名称")
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

    @Schema(description = "父组织编码")
    private String parentDepartmentCode;

    @Schema(description = "父组织名称")
    private String parentDepartmentName;

    @Schema(description = "组织路径")
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

    @Schema(description = "是否启用")
    private Integer isEnable;

    @Schema(description = "HCM 来源创建时间")
    private LocalDateTime createTime;

    @Schema(description = "HRBP 列表（JSON 数组字符串）")
    private String departmentHrbpList;

    @Schema(description = "HCM 原始报文")
    private String hcmPayloadJson;

    @Schema(description = "状态")
    private String status;

    @Getter(value = AccessLevel.NONE)
    @JsonIgnore
    private Map<String, Object> attributes;

    /** 将动态扩展属性平铺到 JSON 顶层，供前端动态表头/表单使用。 */
    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}