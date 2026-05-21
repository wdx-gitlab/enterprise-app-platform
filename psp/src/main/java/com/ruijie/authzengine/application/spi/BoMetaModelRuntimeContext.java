package com.ruijie.authzengine.application.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BO Hook 运行时上下文，由权限引擎在调用宿主适配器前注入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoMetaModelRuntimeContext {

    private String tenantId;

    private String appCode;

    private String boCode;

    private String resolver;
}