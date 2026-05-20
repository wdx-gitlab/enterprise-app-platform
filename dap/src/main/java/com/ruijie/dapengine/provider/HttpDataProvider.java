package com.ruijie.dapengine.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.common.model.FetchResult;
import com.ruijie.dapengine.common.model.SyncCheckpoint;
import com.ruijie.dapengine.common.model.SyncDataSourceConfig;
import com.ruijie.dapengine.common.model.TestConnectResultDTO;
import com.ruijie.dapengine.common.util.AesCipher;
import com.ruijie.dapengine.common.util.HeaderExpressionEvaluator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * HTTP DataProvider 实现。
 *
 * <p>支持 GET/POST 请求从 REST API 拉取数据，通过 {@code dataPath} 路径提取 JSON 数组节点。
 * 敏感 Header 值（含 "authorization"）在运行时动态解密，不持久化明文。</p>
 *
 * <p>每次 testConnect/fetch 都新建 {@code RestTemplate} 实例，使用独立的连接/读取超时配置，
 * 不共享连接池，避免影响宿主应用 HTTP 客户端配置。</p>
 */
public class HttpDataProvider implements DataProvider {

    /** 默认 HTTP 连接超时（毫秒） */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;

    /** 默认 HTTP 读取超时（毫秒）——全量同步时单次拉取可能返回大报文，默认 60s */
    private static final int DEFAULT_READ_TIMEOUT_MS = 60000;

    /** testConnect 最多返回的样例行数 */
    private static final int MAX_SAMPLE_ROWS = 5;

    /** Authorization 类 Header 名前缀（小写匹配） */
    private static final String AUTH_HEADER_PREFIX = "authorization";

    private final AesCipher aesCipher;
    private final ObjectMapper objectMapper;
    /** 可注入 RestTemplate（测试用），null 时每次新建 */
    RestTemplate restTemplateOverride;

    public HttpDataProvider(AesCipher aesCipher, ObjectMapper objectMapper) {
        this.aesCipher = aesCipher;
        this.objectMapper = objectMapper;
    }

    @Override
    public String type() {
        return "HTTP";
    }

    @Override
    public TestConnectResultDTO testConnect(SyncDataSourceConfig config) {
        try {
            RestTemplate restTemplate = buildRestTemplate(config);
            HttpHeaders headers = buildHeaders(config);
            String url = buildUrl(config, null);
            String body = config.getBodyTemplate();

            ResponseEntity<String> response = execute(restTemplate, config, url, headers, body);
            JsonNode dataNode = extractDataNode(response.getBody(), config.getDataPath());
            List<Map<String, Object>> sampleRows = toRowList(dataNode, MAX_SAMPLE_ROWS);
            List<String> sourceFields = extractFieldNames(dataNode);

            return TestConnectResultDTO.success("HTTP", sourceFields, sampleRows);
        } catch (Exception e) {
            return TestConnectResultDTO.failure("HTTP", e.getMessage());
        }
    }

    @Override
    public FetchResult fetch(SyncDataSourceConfig config, SyncCheckpoint checkpoint) {
        try {
            if (!isEmpty(config.getPageParam())) {
                // 有分页配置：通过 fetchStreaming 积累所有页（保持 API 兼容；大数据量推荐直接使用 fetchStreaming）
                List<Map<String, Object>> allRows = new ArrayList<>();
                fetchStreaming(config, checkpoint, allRows::addAll);
                return FetchResult.of(allRows);
            }
            // 无分页：单次请求
            return fetchSingleRequest(config, checkpoint);
        } catch (RuntimeException e) {
            throw e;  // 保留原始类型（DapValidationException / DataAccessException 等），不二次包装
        } catch (Exception e) {
            throw new RuntimeException("[DAP Engine] HTTP fetch failed: " + e.getMessage(), e);
        }
    }

    /**
     * 流式分页拉取：每拉到一页立即回调 pageConsumer，不在内存积累全量数据。
     * 无分页配置时视为单页，单次回调。
     */
    @Override
    public void fetchStreaming(SyncDataSourceConfig config, SyncCheckpoint checkpoint,
                               Consumer<List<Map<String, Object>>> pageConsumer) {
        try {
            if (isEmpty(config.getPageParam())) {
                FetchResult single = fetchSingleRequest(config, checkpoint);
                if (!single.isEmpty()) {
                    pageConsumer.accept(single.getRecords());
                }
                return;
            }
            int pageSize = config.getPageSize() > 0 ? config.getPageSize() : 500;
            // RestTemplate 在循环外创建，避免每页重复构造（headers 含动态签名，须每页重建）
            RestTemplate restTemplate = buildRestTemplate(config);
            for (int page = 1; page <= MAX_PAGES; page++) {
                List<Map<String, Object>> rows = fetchSinglePage(restTemplate, config, checkpoint, page, pageSize);
                if (rows.isEmpty()) break;
                pageConsumer.accept(rows);
                if (rows.size() < pageSize) break;  // 最后一页
                if (page == MAX_PAGES) {
                    throw new IllegalStateException(
                            "[DAP Engine] 自动翻页超过安全上限 " + MAX_PAGES + " 页（pageSize=" + pageSize
                            + "，已拉取约 " + ((long) page * pageSize) + " 条）。"
                            + "请增大「每页条数」或检查数据源是否返回异常大量数据。");
                }
            }
        } catch (RuntimeException ex) {
            // 保留原始 RuntimeException 类型（包括 DapValidationException、DataAccessException
            // 以及 consumer lambda 内抛出的所有运行时异常），不二次包装
            throw ex;
        } catch (Exception e) {
            // 仅包装真正的受检异常（实践中极少出现，RestTemplate 只抛 RuntimeException）
            throw new RuntimeException("[DAP Engine] HTTP fetchStreaming failed: " + e.getMessage(), e);
        }
    }

    /** 不带分页的单次 HTTP 请求，返回 FetchResult。 */
    private FetchResult fetchSingleRequest(SyncDataSourceConfig config, SyncCheckpoint checkpoint) throws Exception {
        RestTemplate restTemplate = buildRestTemplate(config);
        HttpHeaders headers = buildHeaders(config);
        String url = buildUrl(config, checkpoint);
        String body = buildBody(config, checkpoint);

        ResponseEntity<String> response = execute(restTemplate, config, url, headers, body);
        JsonNode dataNode = extractDataNode(response.getBody(), config.getDataPath());
        List<Map<String, Object>> rows = toRowList(dataNode, Integer.MAX_VALUE);
        return FetchResult.of(rows);
    }

    /** 拉取指定页码的一页数据（由调用方传入已复用的 RestTemplate）。 */
    private List<Map<String, Object>> fetchSinglePage(RestTemplate restTemplate,
                                                       SyncDataSourceConfig config,
                                                       SyncCheckpoint checkpoint,
                                                       int page, int pageSize) throws Exception {
        HttpHeaders headers = buildHeaders(config);  // headers 含动态签名时间戳，每页必须重建
        String url = buildUrl(config, checkpoint);
        String body = buildBody(config, checkpoint);

        if ("GET".equalsIgnoreCase(config.getMethod())) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            if (!isEmpty(config.getPageParam())) {
                builder = builder.replaceQueryParam(config.getPageParam(), page);
            }
            if (!isEmpty(config.getSizeParam())) {
                builder = builder.replaceQueryParam(config.getSizeParam(), pageSize);
            }
            url = builder.toUriString();
        }
        if (body != null) {
            body = body.replace("{{page}}", String.valueOf(page))
                       .replace("{{pageSize}}", String.valueOf(pageSize));
        }

        ResponseEntity<String> response = execute(restTemplate, config, url, headers, body);
        JsonNode dataNode = extractDataNode(response.getBody(), config.getDataPath());
        return toRowList(dataNode, Integer.MAX_VALUE);
    }

    /** 自动翻页单次同步的最大页数上限，防止无限循环。超出时抛异常（而非静默截断）。*/
    static final int MAX_PAGES = 2000;

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private RestTemplate buildRestTemplate(SyncDataSourceConfig config) {
        if (restTemplateOverride != null) {
            return restTemplateOverride;
        }
        int connectTimeout = config.getConnectTimeoutMs() > 0
                ? config.getConnectTimeoutMs() : DEFAULT_CONNECT_TIMEOUT_MS;
        int readTimeout = config.getReadTimeoutMs() > 0
                ? config.getReadTimeoutMs() : DEFAULT_READ_TIMEOUT_MS;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    private HttpHeaders buildHeaders(SyncDataSourceConfig config) {
        HttpHeaders headers = new HttpHeaders();
        if (config.getHeaders() != null) {
            Map<String, String> signVars = config.getSignVars();
            for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
                String key   = entry.getKey();
                String value = entry.getValue();
                // 1. 先对 Header 值中的 ${...} 表达式求值（如动态时间戳签名）
                if (value != null && value.contains("${")) {
                    value = HeaderExpressionEvaluator.evaluate(value, signVars);
                }
                // 2. 再解密 AES 加密的敏感 Header 值（如 Authorization）
                if (value != null && key.toLowerCase().contains(AUTH_HEADER_PREFIX)
                        && aesCipher.isEncrypted(value)) {
                    value = aesCipher.decrypt(value);
                }
                headers.set(key, value != null ? value : "");
            }
        }
        return headers;
    }

    /**
     * 构建请求 URL。GET 模式时将 lastVersion 追加为 QueryString 参数。
     */
    private String buildUrl(SyncDataSourceConfig config, SyncCheckpoint checkpoint) {
        String url = config.getUrl();
        if ("GET".equalsIgnoreCase(config.getMethod())
                && !isEmpty(config.getIncrementalParamName())
                && checkpoint != null) {
            url = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam(config.getIncrementalParamName(), checkpoint.getLastVersion())
                    .toUriString();
        }
        return url;
    }

    /**
     * 构建 POST 请求体，替换 bodyTemplate 中的占位符 {{lastVersion}}/{{lastSyncTime}}。
     */
    private String buildBody(SyncDataSourceConfig config, SyncCheckpoint checkpoint) {
        if (!isEmpty(config.getBodyTemplate()) && checkpoint != null) {
            return config.getBodyTemplate()
                    .replace("{{lastVersion}}", String.valueOf(checkpoint.getLastVersion()))
                    .replace("{{lastSyncTime}}", checkpoint.getLastSyncTime() != null
                            ? checkpoint.getLastSyncTime().toString() : "");
        }
        return config.getBodyTemplate();
    }

    private ResponseEntity<String> execute(RestTemplate restTemplate, SyncDataSourceConfig config,
                                           String url, HttpHeaders headers, String body) {
        if ("POST".equalsIgnoreCase(config.getMethod())) {
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } else {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        }
    }

    /**
     * 按 dataPath 点分割逐层提取 JSON 节点（数组或对象均可）。
     * dataPath 为空时直接解析响应顶层。
     * 兼容 JSONPath 根符号："$.data" 与 "data" 等价，"$" 或 "$." 前缀均自动剥离。
     */
    private JsonNode extractDataNode(String responseBody, String dataPath) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (!isEmpty(dataPath)) {
            // 剥离 JSONPath 根符号前缀（"$." 或 "$"），使 "$.data" 与 "data" 等价
            String normalizedPath = dataPath;
            if (normalizedPath.startsWith("$.")) {
                normalizedPath = normalizedPath.substring(2);
            } else if (normalizedPath.startsWith("$")) {
                normalizedPath = normalizedPath.substring(1);
            }
            if (!isEmpty(normalizedPath)) {
                String[] paths = normalizedPath.split("\\.");
                for (String path : paths) {
                    root = root.path(path);
                }
            }
        }
        return root;
    }

    /**
     * 将 JsonNode（数组或单对象）转换为 Map 行列表，最多取 maxRows 行。
     * 当 dataNode 为 JSON 对象（单条记录 API）时自动包装成单元素列表。
     */
    private List<Map<String, Object>> toRowList(JsonNode dataNode, int maxRows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (dataNode == null || dataNode.isMissingNode() || dataNode.isNull()) {
            return rows;
        }
        // 单对象响应：包装为单元素列表
        if (dataNode.isObject()) {
            Map<String, Object> row = objectMapper.convertValue(dataNode,
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
            rows.add(row);
            return rows;
        }
        if (!dataNode.isArray()) {
            return rows;
        }
        int count = 0;
        for (JsonNode item : dataNode) {
            if (count >= maxRows) {
                break;
            }
            if (item.isObject()) {
                Map<String, Object> row = objectMapper.convertValue(item,
                        objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
                rows.add(row);
                count++;
            }
        }
        return rows;
    }

    /**
     * 从数据节点提取字段名列表（sourceFields）。
     * 支持数组（取第一个元素）和单对象两种形式。
     */
    private List<String> extractFieldNames(JsonNode dataNode) {
        if (dataNode == null || dataNode.isMissingNode() || dataNode.isNull()) {
            return Collections.emptyList();
        }
        // 单对象：直接取字段名
        if (dataNode.isObject()) {
            List<String> fields = new ArrayList<>();
            Iterator<String> fieldNames = dataNode.fieldNames();
            while (fieldNames.hasNext()) {
                fields.add(fieldNames.next());
            }
            return fields;
        }
        if (!dataNode.isArray() || !dataNode.elements().hasNext()) {
            return Collections.emptyList();
        }
        JsonNode firstItem = dataNode.elements().next();
        if (!firstItem.isObject()) {
            return Collections.emptyList();
        }
        List<String> fields = new ArrayList<>();
        Iterator<String> fieldNames = firstItem.fieldNames();
        while (fieldNames.hasNext()) {
            fields.add(fieldNames.next());
        }
        return fields;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
