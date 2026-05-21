package com.ruijie.authzengine.application.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SDK 资源引用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzResourceRef {

    /** 资源模型编码。 */
    private String resourceModel;

    /** 资源标识。 */
    private String resourceId;
}