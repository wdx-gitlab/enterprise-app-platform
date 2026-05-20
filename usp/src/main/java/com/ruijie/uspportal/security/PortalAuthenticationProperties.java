package com.ruijie.uspportal.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "usp.portal.security")
public class PortalAuthenticationProperties {

    private static final List<String> DEFAULT_AUTHENTICATION_WHITELIST = Arrays.asList(
            "/usp-portal/api/auth/login",
            "/usp-portal/api/auth/login-options",
            "/usp-portal/api/auth/cs05/callback",
            "/heath",
            "/error"
    );

    private List<String> additionalWhitelist = new ArrayList<>();

    public List<String> getAuthenticationWhitelist() {
        LinkedHashSet<String> whitelist = new LinkedHashSet<>(DEFAULT_AUTHENTICATION_WHITELIST);
        whitelist.addAll(additionalWhitelist);
        return new ArrayList<>(whitelist);
    }
}