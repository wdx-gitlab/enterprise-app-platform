package com.ruijie.authzengine.autoconfigure;

import org.springframework.context.annotation.Import;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AuthzEngineCoreConfiguration.class)
public @interface EnableAuthzEngine {
}
