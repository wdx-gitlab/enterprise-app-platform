package com.ruijie.dapengine.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.common.model.SyncDataSourceConfig;
import com.ruijie.dapengine.common.model.TestConnectResultDTO;
import com.ruijie.dapengine.common.util.AesCipher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HttpDataProvider 单元测试（Mockito mock RestTemplate）。
 * 覆盖：testConnect 正常返回 sourceFields、dataPath 多级提取、连接异常返回 success=false。
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpDataProviderTest {

    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";

    @Mock
    private RestTemplate restTemplate;

    private HttpDataProvider provider;

    @Before
    public void setUp() {
        AesCipher aesCipher = new AesCipher(VALID_KEY);
        provider = new HttpDataProvider(aesCipher, new ObjectMapper());
        // 注入 mock RestTemplate，替代默认实例化逻辑
        provider.restTemplateOverride = restTemplate;
    }

    private SyncDataSourceConfig buildHttpConfig(String url, String dataPath) {
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setUrl(url);
        ds.setMethod("GET");
        ds.setDataPath(dataPath);
        return ds;
    }

    @Test
    public void testConnect_should_return_sourceFields_on_success() {
        String json = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        SyncDataSourceConfig ds = buildHttpConfig("http://api.test/data", null);
        TestConnectResultDTO result = provider.testConnect(ds);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSourceFields()).containsExactlyInAnyOrder("id", "name");
        assertThat(result.getSampleRows()).hasSize(2);
    }

    @Test
    public void testConnect_should_limit_to_5_sample_rows() {
        String json = "[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4},{\"id\":5},{\"id\":6},{\"id\":7}]";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        SyncDataSourceConfig ds = buildHttpConfig("http://api.test/data", null);
        TestConnectResultDTO result = provider.testConnect(ds);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSampleRows().size()).isLessThanOrEqualTo(5);
    }

    @Test
    public void testConnect_should_extract_dataPath_multilevel() {
        // dataPath = "data.items"，指向 response.data.items 数组
        String json = "{\"data\":{\"items\":[{\"code\":\"C001\",\"label\":\"客户A\"}]}}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        SyncDataSourceConfig ds = buildHttpConfig("http://api.test/nested", "data.items");
        TestConnectResultDTO result = provider.testConnect(ds);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSourceFields()).containsExactlyInAnyOrder("code", "label");
        assertThat(result.getSampleRows()).hasSize(1);
        assertThat(result.getSampleRows().get(0).get("code")).isEqualTo("C001");
    }

    @Test
    public void testConnect_should_return_failure_on_exception() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        SyncDataSourceConfig ds = buildHttpConfig("http://unreachable.test/data", null);
        TestConnectResultDTO result = provider.testConnect(ds);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMsg()).contains("Connection refused");
        assertThat(result.getSourceFields()).isEmpty();
        assertThat(result.getSampleRows()).isEmpty();
    }

    @Test
    public void type_should_return_HTTP() {
        assertThat(provider.type()).isEqualTo("HTTP");
    }

    @Test
    public void testConnect_should_handle_jsonpath_dollar_prefix_in_dataPath() {
        // dataPath = "$.data" 等价于 "data"，前缀应自动剥离
        String json = "{\"data\":[{\"code\":\"ORG001\",\"name\":\"总部\"}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        SyncDataSourceConfig ds = buildHttpConfig("http://api.test/orgs", "$.data");
        TestConnectResultDTO result = provider.testConnect(ds);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSourceFields()).containsExactlyInAnyOrder("code", "name");
        assertThat(result.getSampleRows()).hasSize(1);
    }

    @Test
    public void testConnect_should_handle_single_object_response() {
        // dataPath 指向单个 JSON 对象（非数组），应自动包装为单条记录
        String json = "{\"status\":\"200\",\"data\":{\"code\":\"ORG001\",\"name\":\"总部\",\"parentCode\":\"ROOT\"}}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        SyncDataSourceConfig ds = buildHttpConfig("http://api.test/org/ORG001", "$.data");
        TestConnectResultDTO result = provider.testConnect(ds);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSourceFields()).containsExactlyInAnyOrder("code", "name", "parentCode");
        assertThat(result.getSampleRows()).hasSize(1);
        assertThat(result.getSampleRows().get(0).get("code")).isEqualTo("ORG001");
    }

    @Test
    public void fetch_should_return_single_object_wrapped_as_one_record() {
        String json = "{\"err\":\"操作成功\",\"status\":\"200\",\"data\":{\"code\":\"D001\",\"name\":\"架构部\"}}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        SyncDataSourceConfig ds = buildHttpConfig("http://api.test/dept/D001", "$.data");
        com.ruijie.dapengine.common.model.FetchResult result =
                provider.fetch(ds, com.ruijie.dapengine.common.model.SyncCheckpoint.empty("dept"));

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).get("code")).isEqualTo("D001");
        assertThat(result.getRecords().get(0).get("name")).isEqualTo("架构部");
    }
}
