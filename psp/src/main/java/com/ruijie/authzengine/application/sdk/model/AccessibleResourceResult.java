package com.ruijie.authzengine.application.sdk.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可访问资源结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessibleResourceResult {

    private String resourceType;

    private List<String> resourceCodes;

    private long evalTimeMs;
}