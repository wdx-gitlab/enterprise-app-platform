package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 可访问资源列表响应 DTO（Q2）。
 */
@Data
@Builder
@Schema(description = "可访问资源列表响应")
public class AccessibleResourceResponse {

    @Schema(description = "资源类型", example = "RES_UI_MENU")
    private String resourceType;

    @Schema(description = "主体有权访问的资源编码列表",
        example = "[\"MENU_HOME\", \"MENU_CONTRACT_LIST\", \"MENU_CONTRACT_APPROVE\"]")
    private List<String> resourceCodes;

    @Schema(description = "引擎评估耗时（毫秒）", example = "12")
    private long evalTimeMs;
}

