package com.ruijie.dapengine.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.common.model.DapEngineProperties;
import com.ruijie.dapengine.common.model.DepartmentExtendInfo;
import com.ruijie.dapengine.common.model.RemoteResult;
import com.ruijie.dapengine.common.model.RuijieAuthProperties;
import com.ruijie.dapengine.common.model.StaffExtendInfo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * OSDS服务
 */
public class OSDSService {

    private static final String DEFAULT_BASE_URL = "http://service-gw.ruijie.com.cn/api";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 8000;
    private static final String SIGN_DELIMITER = "|";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String sysId;
    private final String accessKeySecret;

    public OSDSService(ObjectMapper objectMapper,
                       DapEngineProperties dapEngineProperties,
                       RuijieAuthProperties ruijieAuthProperties) {
        this(objectMapper, dapEngineProperties, ruijieAuthProperties,
                buildRestTemplate(resolveConnectTimeout(dapEngineProperties), resolveReadTimeout(dapEngineProperties)));
    }

    OSDSService(ObjectMapper objectMapper,
                DapEngineProperties dapEngineProperties,
                RuijieAuthProperties ruijieAuthProperties,
                RestTemplate restTemplate) {
        this(objectMapper,
                resolveBaseUrl(dapEngineProperties),
                ruijieAuthProperties != null ? ruijieAuthProperties.getId() : null,
                ruijieAuthProperties != null ? ruijieAuthProperties.getAccessKeySecret() : null,
                restTemplate);
    }

    OSDSService(ObjectMapper objectMapper, String baseUrl, String sysId,
                String accessKeySecret, RestTemplate restTemplate) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.sysId = trimToNull(sysId);
        this.accessKeySecret = trimToNull(accessKeySecret);
        this.restTemplate = restTemplate != null
                ? restTemplate
                : buildRestTemplate(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
    }

    /**
     * 查询全量员工信息
     *
     * @param pageSize 每页条数
     * @param pageIndex 页码
     * @return OSDS返回结果
     */
    public RemoteResult<List<LinkedHashMap<String, Object>>> getAllStaff(int pageSize, int pageIndex) {
        String requestUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/osds-api/staff")
                .queryParam("pageSize", pageSize)
                .queryParam("pageIndex", pageIndex)
                .toUriString();
        return get(requestUrl, new TypeReference<List<LinkedHashMap<String, Object>>>() {
        });
    }

    /**
     * 根据工号查询员工信息
     *
     * @param staffNo 工号
     * @return OSDS返回结果
     */
    public RemoteResult<StaffExtendInfo> getStaff(String staffNo) {
        String requestUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/osds-api/staff/{staffNo}/data")
                .buildAndExpand(staffNo)
                .toUriString();
        return get(requestUrl, new TypeReference<StaffExtendInfo>() {
        });
    }

    /**
     * 根据部门编码查询部门信息。
     *
     * @param departmentCode 部门编码
     * @return OSDS 返回结果
     */
    public RemoteResult<DepartmentExtendInfo> getDepartment(String departmentCode) {
        String requestUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/osds-api/department/{departmentCode}/data")
                .buildAndExpand(departmentCode)
                .toUriString();
        return get(requestUrl, new TypeReference<DepartmentExtendInfo>() {
        });
    }

    private <T> RemoteResult<T> get(String requestUrl, TypeReference<T> dataType) {
        if (sysId == null || accessKeySecret == null) {
            return RemoteResult.failed("OSDS鉴权配置缺失，请配置 spring.ruijie.auth.sys.id 和 spring.ruijie.auth.sys.accessKeySecret");
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(buildAuthHeaders()),
                    String.class);
            return parseResult(response.getBody(), dataType);
        } catch (RestClientException e) {
            return RemoteResult.failed("OSDS请求失败: " + e.getMessage());
        }
    }

    private <T> RemoteResult<T> parseResult(String responseBody, TypeReference<T> dataType) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return RemoteResult.failed("OSDS返回为空");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("status") || root.has("err")) {
                String status = root.path("status").asText();
                String err = root.path("err").asText();
                if (!isSuccessStatus(status)) {
                    return RemoteResult.failed(err);
                }
                return convertDataNode(root.get("data"), dataType);
            }

            if (root.has("code") || root.has("message")) {
                String code = root.path("code").asText();
                String message = root.path("message").asText("success");
                if (!isSuccessStatus(code)) {
                    return RemoteResult.failed(message);
                }
                return convertDataNode(root.get("data"), dataType);
            }

            if (root.has("data")) {
                return convertDataNode(root.get("data"), dataType);
            }

            T data = objectMapper.convertValue(root, dataType);
            return RemoteResult.success(data);
        } catch (IOException e) {
            return RemoteResult.failed("OSDS响应解析失败: " + e.getMessage());
        }
    }

    private <T> RemoteResult<T> convertDataNode(JsonNode dataNode, TypeReference<T> dataType) {
        if (dataNode == null || dataNode.isNull()) {
            return RemoteResult.success(null);
        }
        T data = objectMapper.convertValue(dataNode, dataType);
        return RemoteResult.success(data);
    }

    private boolean isSuccessStatus(String codeOrStatus) {
        return "0".equals(codeOrStatus) || "200".equals(codeOrStatus);
    }

    private HttpHeaders buildAuthHeaders() {
        long timestamp = System.currentTimeMillis();
        String signature = buildSignature(timestamp);
        HttpHeaders headers = new HttpHeaders();
        headers.set("sysid", sysId);
        headers.set("sign-server-auth", signature);
        return headers;
    }

    private String buildSignature(long timestamp) {
        String digest = md5Upper(sysId + timestamp + accessKeySecret);
        return sysId + SIGN_DELIMITER + timestamp + SIGN_DELIMITER + digest;
    }

    private String md5Upper(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return String.format("%032x", new BigInteger(1, digest)).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    private static RestTemplate buildRestTemplate(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }

    private static String resolveBaseUrl(DapEngineProperties properties) {
        if (properties == null || properties.getSync() == null || properties.getSync().getOsds() == null) {
            return DEFAULT_BASE_URL;
        }
        return properties.getSync().getOsds().getBaseUrl();
    }

    private static int resolveConnectTimeout(DapEngineProperties properties) {
        if (properties == null || properties.getSync() == null || properties.getSync().getHttp() == null) {
            return DEFAULT_CONNECT_TIMEOUT_MS;
        }
        return properties.getSync().getHttp().getConnectTimeoutMs();
    }

    private static int resolveReadTimeout(DapEngineProperties properties) {
        if (properties == null || properties.getSync() == null || properties.getSync().getHttp() == null) {
            return DEFAULT_READ_TIMEOUT_MS;
        }
        return properties.getSync().getHttp().getReadTimeoutMs();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String target = (baseUrl == null || baseUrl.trim().isEmpty()) ? DEFAULT_BASE_URL : baseUrl.trim();
        return target.endsWith("/") ? target.substring(0, target.length() - 1) : target;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}