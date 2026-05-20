package com.ruijie.uspportal.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "usp.portal.login.override")
/**
 * PortalLoginOverride 配置属性类。
 */
public class PortalLoginOverrideProperties {

    private boolean enabled;

    private Boolean internalLoginEnabled;

    private Boolean ssoLoginEnabled;

    private String defaultLoginMode;

    private String ssoButtonText;
}
