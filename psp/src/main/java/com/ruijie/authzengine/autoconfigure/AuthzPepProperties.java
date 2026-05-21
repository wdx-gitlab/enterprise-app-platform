package com.ruijie.authzengine.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PEP（策略执行点）配置属性。
 */
@ConfigurationProperties(prefix = "authz.engine.pep")
public class AuthzPepProperties {

    private boolean enabled = true;

    private List<String> includePatterns = new ArrayList<>();

    private List<String> excludePatterns = new ArrayList<>();

    /** 引擎自身白名单路径，始终放行（Ant 风格），默认放行引擎 UI 与 Actuator */
    private List<String> engineWhitelist = new ArrayList<>(java.util.Arrays.asList(
        "/authz-engine/**",
        "/authz-engine-ui/**",
        "/actuator/**",
        "/error"
    ));

    /**
     * 是否启用 HTTP Filter 全量拦截模式（默认 true）。
     *
     * <p>设为 false 时关闭统一 HTTP 请求鉴权入口。
     */
    private boolean httpFilterEnabled = true;

    /**
     * 未声明资源的处理策略（默认 ALLOW）。
     *
     * <p>当请求 URI 在 usp_api 中没有匹配记录，或 API 资源没有关联权限项时，
     * 使用本策略决定是放行（ALLOW）还是拒绝（DENY）。
     * <ul>
     *   <li>{@code ALLOW}：放行，适合灰度迁移阶段（默认）</li>
     *   <li>{@code DENY}：拒绝，适合已完成全量注册的生产环境</li>
     * </ul>
     */
    private String undeclaredResourceStrategy = "ALLOW";

    /**
     * HTTP PEP Filter 的 Servlet Filter 优先级（默认 100）。
     *
     * <p>数值越小优先级越高（越早执行）。默认值 {@code 100} 确保在宿主系统 SSO / 认证框架
     * Filter（通常 order ≤ 0）之后执行，从而保证 {@link com.ruijie.authzengine.domain.spi.AuthzSubjectProvider}
     * 能正确读取到已初始化的用户身份上下文。
     *
     * <p>若宿主 SSO Filter order 大于 100，需相应调高此值（如改为 200）。
     */
    private int filterOrder = 100;

    /**
    * 是否启用 PEP。
     *
     * @return true 表示启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置 PEP 开关。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取需要拦截的路径模式列表。
     *
     * @return 路径模式列表
     */
    public List<String> getIncludePatterns() {
        return includePatterns;
    }

    /**
     * 设置需要拦截的路径模式列表。
     *
     * @param includePatterns 路径模式列表
     */
    public void setIncludePatterns(List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }

    /**
     * 获取不拦截的路径模式列表（优先于 includePatterns）。
     *
     * @return 排除路径模式列表
     */
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    /**
     * 设置不拦截的路径模式列表。
     *
     * @param excludePatterns 排除路径模式列表
     */
    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    /**
     * 获取引擎自身白名单路径列表。
     *
     * @return 引擎白名单路径列表
     */
    public List<String> getEngineWhitelist() {
        return engineWhitelist;
    }

    /**
     * 设置引擎自身白名单路径列表。
     *
     * @param engineWhitelist 白名单路径列表
     */
    public void setEngineWhitelist(List<String> engineWhitelist) {
        this.engineWhitelist = engineWhitelist;
    }

    /**
     * 是否启用 HTTP Filter 全量拦截模式。
     *
     * @return true 表示启用
     */
    public boolean isHttpFilterEnabled() {
        return httpFilterEnabled;
    }

    /**
     * 设置是否启用 HTTP Filter 全量拦截模式。
     *
     * @param httpFilterEnabled 是否启用
     */
    public void setHttpFilterEnabled(boolean httpFilterEnabled) {
        this.httpFilterEnabled = httpFilterEnabled;
    }

    /**
     * 获取未声明资源处理策略。
     *
     * @return ALLOW 或 DENY
     */
    public String getUndeclaredResourceStrategy() {
        return undeclaredResourceStrategy;
    }

    /**
     * 设置未声明资源处理策略。
     *
     * @param undeclaredResourceStrategy ALLOW 或 DENY
     */
    public void setUndeclaredResourceStrategy(String undeclaredResourceStrategy) {
        this.undeclaredResourceStrategy = undeclaredResourceStrategy;
    }

    /**
     * 获取 HTTP PEP Filter 的 Servlet Filter 优先级。
     *
     * @return filter order，默认 100
     */
    public int getFilterOrder() {
        return filterOrder;
    }

    /**
     * 设置 HTTP PEP Filter 的 Servlet Filter 优先级。
     *
     * @param filterOrder filter order
     */
    public void setFilterOrder(int filterOrder) {
        this.filterOrder = filterOrder;
    }
}
