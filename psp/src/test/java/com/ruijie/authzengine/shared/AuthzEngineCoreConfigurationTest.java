package com.ruijie.authzengine.shared;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;

class AuthzEngineCoreConfigurationTest {

    @Test
    void shouldScanSharedExceptionPackageForGlobalExceptionHandler() {
        ComponentScan componentScan = AuthzEngineCoreConfiguration.class.getAnnotation(ComponentScan.class);

        Assertions.assertNotNull(componentScan, "AuthzEngineCoreConfiguration 必须声明 ComponentScan");
        Assertions.assertTrue(
            Arrays.asList(componentScan.basePackages()).contains("com.ruijie.authzengine.shared.exception"),
            "AuthzEngineCoreConfiguration 必须扫描 shared.exception 包，确保全局异常处理器生效"
        );
    }
}