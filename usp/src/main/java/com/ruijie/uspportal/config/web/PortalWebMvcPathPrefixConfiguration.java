package com.ruijie.uspportal.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Portal Web MVC 路径前缀配置。
 *
 * <p>统一为门户后端控制器添加 `/usp-portal` 请求前缀，避免与宿主侧接口路径冲突。</p>
 */
@Configuration
public class PortalWebMvcPathPrefixConfiguration implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorPathExtension(false)
            .favorParameter(false)
            .ignoreAcceptHeader(true)
            .defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    /**
     * 配置门户控制器的统一路径前缀。
     *
     * @param configurer 路径匹配配置器
     */
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/usp-portal",
            HandlerTypePredicate.forBasePackage("com.ruijie.uspportal"));
    }
}
