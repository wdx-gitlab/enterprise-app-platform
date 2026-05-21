package com.ruijie.authzengine.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * authz-engine Starter 前置总开关配置。
 */
@ConfigurationProperties(prefix = "authz.engine")
public class AuthzEngineProperties {

    private boolean enabled = true;

    private String tenantId = "default";

    private String appCode = "default";

    /**
     * 是否启用 authz-engine Starter 前置装配能力。
     *
     * @return true 表示启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置 Starter 前置装配开关。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取租户 ID。
     *
     * @return 租户 ID
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * 设置租户 ID。
     *
     * @param tenantId 租户 ID
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 获取应用编码。
     *
     * @return 应用编码
     */
    public String getAppCode() {
        return appCode;
    }

    /**
     * 设置应用编码。
     *
     * @param appCode 应用编码
     */
    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }
}