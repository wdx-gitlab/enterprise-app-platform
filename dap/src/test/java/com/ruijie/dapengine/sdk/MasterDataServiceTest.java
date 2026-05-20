package com.ruijie.dapengine.sdk;

import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.autoconfigure.DapEngineJdbcTemplate;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.DapEngineProperties;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.TreeNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MasterDataService 单元测试（JUnit4 + Mockito）。
 * 覆盖 US4：getByCode 缓存命中/未命中、query 字段白名单校验、getTree parent_code 检查。
 */
@RunWith(MockitoJUnitRunner.class)
public class MasterDataServiceTest {

    @Mock private DapEngineJdbcTemplate dapJdbcTemplate;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private MetadataConfigService metadataConfigService;
    @Mock private MasterDataCacheService cacheService;

    private MasterDataService service;

    @Before
    public void setUp() {
        when(dapJdbcTemplate.getJdbcTemplate()).thenReturn(jdbcTemplate);
        DapEngineProperties props = new DapEngineProperties();
        props.setTenantId("T1");
        props.setAppCode("APP1");
        service = new MasterDataService(dapJdbcTemplate, metadataConfigService, cacheService, props);
    }

    // -------------------------------------------------------------------------
    // T011: getByCode，缓存命中，不查 DB
    // -------------------------------------------------------------------------

    @Test
    public void getByCode_should_return_cached_and_skip_db() {
        Map<String, Object> cached = Collections.singletonMap("code", "C001");
        when(cacheService.get("CUSTOMER", "C001")).thenReturn(cached);

        Map<String, Object> result = service.getByCode("CUSTOMER", "C001");

        assertThat(result).isEqualTo(cached);
        verify(jdbcTemplate, never()).queryForList(anyString(), (Object[]) any());
    }

    // -------------------------------------------------------------------------
    // T012: getByCode，缓存未命中，查 DB 后回写缓存
    // -------------------------------------------------------------------------

    @Test
    public void getByCode_cache_miss_should_query_db_and_write_cache() {
        when(cacheService.get("CUSTOMER", "C001")).thenReturn(null);

        Map<String, Object> dbRow = new HashMap<>();
        dbRow.put("code", "C001");
        dbRow.put("name", "测试客户");
        when(jdbcTemplate.queryForList(anyString(), (Object[]) any()))
                .thenReturn(Collections.singletonList(dbRow));

        Map<String, Object> result = service.getByCode("CUSTOMER", "C001");

        assertThat(result.get("code")).isEqualTo("C001");
        verify(cacheService).put("CUSTOMER", "C001", dbRow);
    }

    // -------------------------------------------------------------------------
    // T013: query，非白名单字段应抛 DapValidationException
    // -------------------------------------------------------------------------

    @Test
    public void query_with_non_whitelist_field_should_throw_validation_exception() {
        FieldConfigDTO f = new FieldConfigDTO();
        f.setFieldName("name");
        when(metadataConfigService.getActiveFieldDTOs("CUSTOMER"))
                .thenReturn(Collections.singletonList(f));

        Map<String, Object> filters = Collections.singletonMap("_injected", "1 OR 1=1");

        assertThatThrownBy(() -> service.query("CUSTOMER", filters, Map.class))
                .isInstanceOf(DapValidationException.class);
    }

    // -------------------------------------------------------------------------
    // T014: getTree，存在 parent_code 字段时返回正确树形结果
    // -------------------------------------------------------------------------

    @Test
    public void getTree_with_parent_code_field_should_build_tree() {
        FieldConfigDTO parentCodeField = new FieldConfigDTO();
        parentCodeField.setFieldName("parent_code");
        when(metadataConfigService.getActiveFieldDTOs("ORG"))
                .thenReturn(Collections.singletonList(parentCodeField));

        // count 小于 treeFullLoadThreshold，走 loadAll 分支
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), (Object[]) any()))
                .thenReturn(2L);

        Map<String, Object> root = new HashMap<>();
        root.put("code", "ROOT");
        root.put("name", "根节点");
        root.put("parent_code", "");

        Map<String, Object> child = new HashMap<>();
        child.put("code", "C1");
        child.put("name", "子节点");
        child.put("parent_code", "ROOT");

        when(jdbcTemplate.queryForList(anyString(), (Object[]) any()))
                .thenReturn(Arrays.asList(root, child));

        List<TreeNode> tree = service.getTree("ORG", null);

        assertThat(tree).isNotEmpty();
        assertThat(tree.get(0).getChildren()).isNotEmpty();
    }

    // -------------------------------------------------------------------------
    // T015: getTree，不存在 parent_code 字段应抛 DapValidationException
    // -------------------------------------------------------------------------

    @Test
    public void getTree_throws_when_no_parent_code_field() {
        when(metadataConfigService.getActiveFieldDTOs("CUSTOMER"))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.getTree("CUSTOMER", null))
                .isInstanceOf(DapValidationException.class);
    }
}
