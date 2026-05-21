package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 主体权限查询通用请求 DTO（Q1 主体权限快照）。
 */
@Data
@Schema(description = "主体权限快照查询请求")
public class SubjectContextQueryRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "主体 ID 不能为空")
    @Schema(description = "主体标识", example = "U10001")
    private String subjectId;

    @Schema(description = "主体类型，默认 SUB_USER", example = "SUB_USER")
    private String subjectModel = "SUB_USER";
}

