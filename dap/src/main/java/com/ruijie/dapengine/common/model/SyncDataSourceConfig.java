package com.ruijie.dapengine.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 同步数据源配置 POJO（datasource_config JSON 的内存模型）。
 *
 * <p>HTTP、DB、MQ 三种类型的配置字段合并在同一类中，按 providerType 使用对应字段。
 * 序列化/反序列化由 Jackson ObjectMapper 完成，未使用字段值为 null（Jackson 默认不序列化 null）。</p>
 *
 * <p>password 字段在落库前由 SyncConfigService 进行 AES-256 加密，
 * 查询时先解密再掩码处理（替换为 "****"）。</p>
 */
@Data
@NoArgsConstructor
public class SyncDataSourceConfig {

    // ---- HTTP 配置 ----

    /** 请求 URL */
    private String url;

    /** HTTP 方法：GET / POST */
    private String method;

    /** 请求头（含静态鉴权 Header，敏感值 AES 加密存储） */
    private Map<String, String> headers;

    /**
     * 响应 JSON 中数据数组的路径，按点分割逐层提取。
     * 示例："data.items" 表示提取 response.data.items 节点。
     * 空或 null 时直接解析响应顶层为数组。
     */
    private String dataPath;

    /** 增量参数名，GET 请求时追加为 QueryString 参数，POST 时可用于 bodyTemplate 占位符 */
    private String incrementalParamName;

    /**
     * POST 请求体模板（JSON 字符串），支持占位符 {{lastVersion}} / {{lastSyncTime}} / {{page}} / {{pageSize}}。
     * GET 请求时忽略此字段。
     */
    private String bodyTemplate;

    /**
     * 分页：页码参数名（如 page、pageNo、pageNum）。
     * 非空时启用自动分页拉取模式：循环请求直到返回空页或不足一页为止。
     * GET 请求追加为 QueryString；POST 请求替换 bodyTemplate 中的 {{page}} / {{pageSize}} 占位符。
     */
    private String pageParam;

    /** 分页：每页大小参数名（如 size、pageSize、limit），与 pageParam 配合使用。 */
    private String sizeParam;

    /** 分页：每页条数，默认 500。pageParam 非空时生效。 */
    private int pageSize;

    /** HTTP 连接超时（毫秒），0 表示使用全局配置 dap.engine.sync.http.connect-timeout-ms */
    private int connectTimeoutMs;

    /** HTTP 读取超时（毫秒），0 表示使用全局配置 dap.engine.sync.http.read-timeout-ms */
    private int readTimeoutMs;

    // ---- DB 配置 ----

    /** JDBC URL，示例：jdbc:mysql://host:3306/db?useSSL=false */
    private String jdbcUrl;

    /** 数据库用户名 */
    private String username;

    /** 数据库密码（AES-256 加密存储） */
    private String password;

    /**
     * 查询 SQL。DELTA 模式须包含恰好一个 {@code ?} 占位符，绑定 {@code checkpoint.lastVersion}（毫秒时间戳）。
     * FULL_REFRESH 无占位符，直接执行全量 SELECT。
     */
    private String querySql;

    // ---- MQ 配置 ----

    /** 消息队列 Topic / Queue 名称 */
    private String topic;

    /** 消费者组 ID */
    private String consumerGroup;

    /** Kafka Bootstrap Servers，示例：broker1:9092,broker2:9092 */
    private String bootstrapServers;

    /** RocketMQ Name Server 地址，示例：namesrv:9876 */
    private String nameServer;

    /** 消息格式：JSON / AVRO / PROTOBUF */
    private String messageFormat;

    // ---- 表达式签名参数 ----

    /**
     * Header 表达式求值变量：不作为 HTTP Header 发送给服务器，
     * 仅供 Header 值中的 {@code ${vars.key}} 表达式引用（如动态签名计算）。
     * 明文存储，请勿存放高安全级别凭证。
     */
    private Map<String, String> signVars;
}
