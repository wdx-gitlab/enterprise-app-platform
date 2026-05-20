package com.ruijie.uspportal.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
/**
 * AppRuntime 上下文对象。
 */
public class AppRuntimeContext {

    private Long appId;

    private String appCode;

    private String appName;

    private String entryUrl;

    private String routePrefix;

    private String accessMode;

    private String openMode;

    @Builder.Default
    private Map<String, String> contextHeaders = Collections.emptyMap();
}
