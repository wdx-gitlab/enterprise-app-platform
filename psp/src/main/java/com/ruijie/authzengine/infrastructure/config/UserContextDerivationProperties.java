package com.ruijie.authzengine.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * UserContext / Q3 派生查询配置。
 *
 * <p>当前仅收口查询侧两类开关：
 * <ul>
 *   <li>派生加载模式：兼容双读或纯派生；</li>
 *   <li>未配置派生关联时的降级语义：ALLOW / DENY。</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "authz.user-context.derivation")
public class UserContextDerivationProperties {

    /** 派生加载模式，默认兼容模式。 */
    private LoadMode mode = LoadMode.COMPAT;

    /** 未配置派生关联时的降级策略，默认严格拒绝。 */
    private MissingBindingStrategy missingBindingStrategy = MissingBindingStrategy.DENY;

    public enum LoadMode {
        COMPAT,
        DERIVATION_ONLY
    }

    public enum MissingBindingStrategy {
        ALLOW,
        DENY
    }
}