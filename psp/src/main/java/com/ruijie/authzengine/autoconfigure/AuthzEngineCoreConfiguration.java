package com.ruijie.authzengine.autoconfigure;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "com.ruijie.authzengine.api",
    "com.ruijie.authzengine.application",
    "com.ruijie.authzengine.domain",
    "com.ruijie.authzengine.infrastructure",
    "com.ruijie.authzengine.shared"
})
public class AuthzEngineCoreConfiguration {
}
