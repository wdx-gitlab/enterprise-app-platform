package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.application.spi.BoMetaModelAdapter;
import com.ruijie.authzengine.shared.exception.AuthzIntegrationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 业务对象 Resolver 路由器，负责把 BO 元模型中的 resolver 路由到宿主 Bean。
 */
@Component
public class BoResolverRouter {

    private final ApplicationContext applicationContext;

    @Autowired
    public BoResolverRouter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private BoResolverRouter() {
        this.applicationContext = null;
    }

    public static BoResolverRouter noop() {
        return new BoResolverRouter();
    }

    /**
     * 按 adapterType + resolver 解析业务对象适配器。
     *
     * @param adapterType 适配类型
     * @param resolver 宿主 Bean 名称
     * @return 业务对象适配器；未配置时返回 null
     */
    public BoMetaModelAdapter resolve(String adapterType, String resolver) {
        if (!StringUtils.hasText(resolver)) {
            return null;
        }
        if ("noopHook".equalsIgnoreCase(resolver.trim())) {
            return null;
        }
        if (!StringUtils.hasText(adapterType)) {
            throw new AuthzIntegrationException("业务对象 Hook 配置缺少 adapterType, resolver=" + resolver);
        }
        if (!"JAVA_BEAN".equalsIgnoreCase(adapterType.trim())) {
            throw new AuthzIntegrationException("暂不支持的业务对象适配类型: " + adapterType);
        }
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(resolver.trim(), BoMetaModelAdapter.class);
        } catch (NoSuchBeanDefinitionException exception) {
            throw new AuthzIntegrationException("未找到业务对象 Hook Bean: " + resolver, exception);
        } catch (BeansException exception) {
            throw new AuthzIntegrationException("加载业务对象 Hook Bean 失败: " + resolver, exception);
        }
    }
}