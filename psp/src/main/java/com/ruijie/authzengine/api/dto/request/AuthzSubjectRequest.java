package com.ruijie.authzengine.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 主体请求 DTO。
 */
@Data
@Schema(description = "鉴权主体")
public class AuthzSubjectRequest {

    @NotBlank(message = "主体 ID 不能为空")
    @JsonAlias("id")
    @Schema(description = "主体 ID", example = "demo-user")
    private String subjectId;

    @NotBlank(message = "主体类型不能为空")
    @JsonAlias("type")
    @Schema(description = "主体类型", example = "SUB_USER")
    private String subjectModel;
}