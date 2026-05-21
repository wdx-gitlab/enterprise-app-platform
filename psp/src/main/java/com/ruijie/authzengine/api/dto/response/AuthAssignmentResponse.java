package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 授权分配响应。
 */
@Data
@Builder
@Schema(description = "授权分配响应")
public class AuthAssignmentResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", example = "5001")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "主体模型")
    private String subjectModel;

    @Schema(description = "主体标识")
    private String subjectId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "权限项主键")
    private Long permItemId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "策略模板主键")
    private Long policyTplId;

    @Schema(description = "策略参数")
    private String policyParams;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
