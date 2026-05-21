package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审计日志查询请求。
 */
@Data
@Schema(description = "审计日志查询请求")
public class AuditLogQueryRequest {

    @NotBlank(message = "tenantId 不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "appCode 不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @Schema(description = "主体标识", example = "demo-user")
    private String subjectId;

    @Schema(description = "资源标识", example = "CONTRACT")
    private String resId;

    @Schema(description = "动作编码", example = "APPROVE")
    private String actionCode;

    @Schema(description = "决策结果", example = "NOT_PERMIT")
    private String decision;

    @Min(value = 1, message = "pageNo 必须大于等于 1")
    @Schema(description = "页码", example = "1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize 必须大于等于 1")
    @Schema(description = "分页大小", example = "20")
    private Integer pageSize = 20;
}
