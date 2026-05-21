package com.ruijie.authzengine.autoconfigure;

import com.ruijie.authzengine.domain.spi.AuthzSubjectProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 默认的鉴权主体提供者（空实现）。
 *
 * <p>始终返回 null，触发 PEP 切面抛出 UNAUTHENTICATED 异常。
 * 宿主应用应注入自己的实现替换此默认 Bean。
 *
 * <p>使用 {@link Ordered#LOWEST_PRECEDENCE} 确保宿主应用提供的实现始终优先于此兜底实现。
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultAuthzSubjectProvider implements AuthzSubjectProvider {

    @Override
    public String getCurrentUserId() {
        log.debug("使用默认 AuthzSubjectProvider，未获取到用户身份");
        return null;
    }
}
