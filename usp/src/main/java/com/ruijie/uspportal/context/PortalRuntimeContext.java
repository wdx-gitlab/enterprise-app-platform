package com.ruijie.uspportal.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
/**
 * PortalRuntime 上下文对象。
 */
public class PortalRuntimeContext {

    private String homeRoute;

    private String defaultAppCode;

    private String menuVersion;
}
