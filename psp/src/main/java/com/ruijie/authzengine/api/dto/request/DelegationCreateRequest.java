package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 委托授权创建请求。
 */
@Data
@Schema(description = "委托授权创建请求")
public class DelegationCreateRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "委托人主体模型不能为空")
    @Schema(description = "委托人主体模型", example = "SUB_USER")
    private String grantorSubjectModel;

    @NotBlank(message = "委托人主体标识不能为空")
    @Schema(description = "委托人主体标识", example = "approver-a")
    private String grantorSubjectId;

    @NotBlank(message = "被委托人主体模型不能为空")
    @Schema(description = "被委托人主体模型", example = "SUB_USER")
    private String delegateSubjectModel;

    @NotBlank(message = "被委托人主体标识不能为空")
    @Schema(description = "被委托人主体标识", example = "approver-b")
    private String delegateSubjectId;

    @NotBlank(message = "权限项编码不能为空")
    @Schema(description = "权限项编码", example = "CONTRACT_APPROVE")
    private String permissionCode;

    @NotNull(message = "委托生效时间不能为空")
    @Schema(description = "委托生效时间")
    private LocalDateTime startTime;

    @NotNull(message = "委托失效时间不能为空")
    @Schema(description = "委托失效时间")
    private LocalDateTime endTime;

    @Schema(description = "委托原因", example = "请假期间委托")
    private String reason;
}