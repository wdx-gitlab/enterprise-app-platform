package com.ruijie.authzengine.application.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subject Hook 运行时上下文，由权限引擎在调用宿主适配器前注入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthMetaModelRuntimeContext {

    private String tenantId;

    private String appCode;

    private String modelCode;

    private String traceId;

    private String resolver;

    private boolean nativeMode;

    private boolean shadowMode;
}