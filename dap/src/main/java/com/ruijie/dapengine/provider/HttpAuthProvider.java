package com.ruijie.dapengine.provider;

import com.ruijie.dapengine.common.model.SyncDataSourceConfig;

import java.util.Map;

/**
 * HTTP 动态鉴权 SPI 接口（扩展点）。
 *
 * <p>当外部 HTTP 数据源需要 HMAC 签名、临时 Token 等动态鉴权时，
 * 宿主应用可实现此接口并注册为 Bean，{@code HttpDataProvider} 在发起请求前
 * 将调用 {@code getAuthHeaders()} 合并到请求头中（优先级高于配置的静态 headers）。</p>
 *
 * <p>如无特殊鉴权需求，不需要实现此接口；{@code HttpDataProvider} 将直接使用
 * 配置中的静态 headers（含 AES 解密后的 Authorization 值）。</p>
 */
public interface HttpAuthProvider {

    /**
     * 返回动态生成的鉴权 Header Map，将被合并到 HTTP 请求头中。
     *
     * @param config 当前数据源配置（含 url、method 等上下文信息）
     * @return 鉴权 Header Map，key 为 Header 名称，value 为 Header 值；不能为 null
     */
    Map<String, String> getAuthHeaders(SyncDataSourceConfig config);
}
