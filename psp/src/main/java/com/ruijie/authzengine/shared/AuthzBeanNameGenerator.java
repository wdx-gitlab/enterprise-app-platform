package com.ruijie.authzengine.shared;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

/**
 * authz-engine 专属 Bean 命名生成器。
 *
 * <p>为 ComponentScan 扫描到的内部 Bean 统一添加 {@code authz.} 前缀，
 * 避免与宿主系统同名 Bean 冲突。按类型注入不受影响。
 */
public class AuthzBeanNameGenerator extends AnnotationBeanNameGenerator {

    private static final String PREFIX = "authz.";

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        String original = super.generateBeanName(definition, registry);
        return PREFIX + original;
    }
}
