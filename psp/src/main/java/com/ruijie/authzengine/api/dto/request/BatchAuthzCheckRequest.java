package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 批量鉴权占位请求。
 */
@Data
@Schema(description = "批量鉴权占位请求")
public class BatchAuthzCheckRequest {

    @Valid
    @NotEmpty(message = "批量请求列表不能为空")
    @Schema(description = "批量鉴权请求列表")
    private List<AuthzCheckRequest> requests;

    @Schema(description = "聚合模式", example = "INDEPENDENT")
    private String aggregationMode;
}