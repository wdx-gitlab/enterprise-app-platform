package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 委托授权响应。
 */
@Data
@Builder
@Schema(description = "委托授权响应")
public class DelegationResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "委托记录标识", example = "9000001")
    private Long delegationId;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "委托人主体模型")
    private String grantorSubjectModel;

    @Schema(description = "委托人主体标识")
    private String grantorSubjectId;

    @Schema(description = "被委托人主体模型")
    private String delegateSubjectModel;

    @Schema(description = "被委托人主体标识")
    private String delegateSubjectId;

    @Schema(description = "权限项编码")
    private String permissionCode;

    @Schema(description = "委托生效时间")
    private LocalDateTime startTime;

    @Schema(description = "委托失效时间")
    private LocalDateTime endTime;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "委托原因")
    private String reason;
}