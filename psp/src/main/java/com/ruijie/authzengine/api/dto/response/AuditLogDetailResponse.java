package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 审计日志详情响应。
 */
@Data
@Builder
@Schema(description = "审计日志详情响应")
public class AuditLogDetailResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "审计日志标识", example = "9000001")
    private Long auditLogId;

    @Schema(description = "请求标识", example = "TRACE-20260402-0001")
    private String requestId;

    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @Schema(description = "主体模型", example = "SUB_USER")
    private String subjectModel;

    @Schema(description = "主体标识", example = "demo-user")
    private String subjectId;

    @Schema(description = "资源模型", example = "RES_DATA_BO")
    private String resourceModel;

    @Schema(description = "资源标识", example = "CONTRACT")
    private String resId;

    @Schema(description = "动作编码", example = "APPROVE")
    private String actionCode;

    @Schema(description = "决策结果", example = "NOT_PERMIT")
    private String decision;

    @Schema(description = "命中权限项编码")
    private List<String> matchedPermissionCodes;

    @Schema(description = "命中授权分配标识")
    private List<String> matchedAssignmentIds;

    @Schema(description = "命中委托标识")
    private List<String> matchedDelegateIds;

    @Schema(description = "命中策略模板编码")
    private List<String> matchedPolicyTemplateCodes;

    @Schema(description = "失败原因")
    private String failureReason;

    @Schema(description = "耗时毫秒", example = "18")
    private Long costMs;
}
