package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * UI 元素可见性批量查询响应 DTO（Q3）。
 */
@Data
@Builder
@Schema(description = "UI 元素可见性批量查询响应")
public class UiVisibilityResponse {

    @Schema(description = "组件可见性状态 Map，key 为 componentCode，value 为可见性状态")
    private Map<String, UiVisibilityItem> visibility;

    @Schema(description = "引擎评估耗时（毫秒）", example = "6")
    private long evalTimeMs;

    /**
     * 单个 UI 组件的可见性状态。
     */
    @Data
    @Builder
    @Schema(description = "UI 组件可见性状态")
    public static class UiVisibilityItem {

        @Schema(description = "是否可见：false 表示隐藏组件", example = "true")
        private boolean visible;

        @Schema(description = "是否禁用（置灰）：true 时组件可见但不可操作", example = "false")
        private boolean disabled;

        @Schema(description = "是否只读：true 时只允许查看，不允许编辑或操作", example = "false")
        private boolean readonly;
    }
}

