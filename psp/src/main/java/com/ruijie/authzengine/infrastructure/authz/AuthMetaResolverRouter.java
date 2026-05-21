package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.application.spi.AuthMetaModelAdapter;
import com.ruijie.authzengine.shared.exception.AuthzIntegrationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 主体元模型 Resolver 路由器，负责把 Subject 元模型中的 resolver 路由到宿主 Bean。
 */
@Component
public class AuthMetaResolverRouter {

    private final ApplicationContext applicationContext;

    @Autowired
    public AuthMetaResolverRouter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private AuthMetaResolverRouter() {
        this.applicationContext = null;
    }

    public static AuthMetaResolverRouter noop() {
        return new AuthMetaResolverRouter();
    }

    /**
     * 按 adapterType + resolver 解析主体适配器。
     *
     * @param adapterType 适配类型
     * @param resolver 宿主 Bean 名称
     * @return 主体适配器；未配置时返回 null
     */
    public AuthMetaModelAdapter resolve(String adapterType, String resolver) {
        if (!StringUtils.hasText(resolver)) {
            return null;
        }
        if ("noopHook".equalsIgnoreCase(resolver.trim())) {
            return null;
        }
        if (!StringUtils.hasText(adapterType)) {
            throw new AuthzIntegrationException("主体 Hook 配置缺少 adapterType, resolver=" + resolver);
        }
        if (!"JAVA_BEAN".equalsIgnoreCase(adapterType.trim())) {
            throw new AuthzIntegrationException("暂不支持的主体适配类型: " + adapterType);
        }
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(resolver.trim(), AuthMetaModelAdapter.class);
        } catch (NoSuchBeanDefinitionException exception) {
            throw new AuthzIntegrationException("未找到主体 Hook Bean: " + resolver, exception);
        } catch (BeansException exception) {
            throw new AuthzIntegrationException("加载主体 Hook Bean 失败: " + resolver, exception);
        }
    }
}