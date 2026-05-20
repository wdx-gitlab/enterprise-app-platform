package com.ruijie.uspportal.security;

import com.ruijie.uspportal.auth.integration.sso.SsoAuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.ArrayList;

import java.util.Arrays;

@Configuration
public class SecurityConfiguration {

    @Bean
    public WebMvcConfigurer portalAuthenticationWebMvcConfigurer(
            SsoAuthenticationService ssoAuthenticationService,
            PortalAuthenticationProperties portalAuthenticationProperties) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new PortalAuthenticationInterceptor(
                                ssoAuthenticationService,
                                portalAuthenticationProperties.getAuthenticationWhitelist()))
                        .addPathPatterns("/**");
            }
        };
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(
            @Value("${usp.portal.frontend-origins:http://localhost:5173,http://127.0.0.1:5173,http://10.40.148.28:5173}") String frontendOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseCsv(frontendOrigins));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList(LoginSessionService.UNION_SESSION_TICKET_HEADER));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(1);
        return bean;
    }

    private List<String> parseCsv(String value) {
        List<String> result = new ArrayList<>();
        for (String item : value.split(",")) {
            String candidate = item.trim();
            if (!candidate.isEmpty()) {
                result.add(candidate);
            }
        }
        return result;
    }
}
