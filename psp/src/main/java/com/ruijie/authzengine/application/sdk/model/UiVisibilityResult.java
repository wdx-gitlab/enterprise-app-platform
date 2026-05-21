package com.ruijie.authzengine.application.sdk.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UI 组件可见性结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UiVisibilityResult {

    private Map<String, VisibilityItem> visibility;

    private long evalTimeMs;

    /**
     * 单个 UI 组件的渲染状态。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisibilityItem {

        private boolean visible;

        private boolean disabled;

        private boolean readonly;
    }
}