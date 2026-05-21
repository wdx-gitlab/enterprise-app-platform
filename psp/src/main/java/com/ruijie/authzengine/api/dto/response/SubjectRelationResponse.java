package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 主体关系响应。
 */
@Data
@Builder
@Schema(description = "主体关系响应")
public class SubjectRelationResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", example = "1005")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "主体模型")
    private String subjectModel;

    @Schema(description = "主体标识")
    private String subjectId;

    @Schema(description = "关联主体模型")
    private String relatedSubjectModel;

    @Schema(description = "关联主体标识")
    private String relatedSubjectId;

    @Schema(description = "关系类型")
    private String relationType;
}