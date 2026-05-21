package com.ruijie.authzengine.shared;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(
    basePackages = {
        "com.ruijie.authzengine.api",
        "com.ruijie.authzengine.application",
        "com.ruijie.authzengine.domain",
        "com.ruijie.authzengine.infrastructure",
        "com.ruijie.authzengine.shared.exception"
    },
    nameGenerator = AuthzBeanNameGenerator.class
)
public class AuthzEngineCoreConfiguration {
}
