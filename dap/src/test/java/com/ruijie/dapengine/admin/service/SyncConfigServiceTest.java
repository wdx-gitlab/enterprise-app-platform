package com.ruijie.dapengine.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.common.enums.SchemaStatus;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.FieldMapping;
import com.ruijie.dapengine.common.model.SubjectDTO;
import com.ruijie.dapengine.common.model.SyncConfigDTO;
import com.ruijie.dapengine.common.model.SyncConfigRequest;
import com.ruijie.dapengine.common.model.SyncDataSourceConfig;
import com.ruijie.dapengine.common.util.AesCipher;
import com.ruijie.dapengine.entity.SyncConfigEntity;
import com.ruijie.dapengine.repository.SubjectRepository;
import com.ruijie.dapengine.repository.SyncConfigRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SyncConfigService 业务逻辑单元测试（Mockito）。
 * 覆盖：save 合法 HTTP+SCHEDULE 成功、EVENT+cronExpr 非空返回 4001。
 * 覆盖 cronExpr 格式非法、fieldMapping.target 不存在等校验场景。
 * 同时验证 get 返回 password 掩码、配置不存在时返回 null。
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncConfigServiceTest {

    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";
    private static final String VALID_CRON = "0 0 * * * ?";
    private static final String SUBJECT_CODE = "CUSTOMER";

    @Mock
    private SyncConfigRepository syncConfigRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private MetadataConfigService metadataConfigService;
    @Mock
    private SchemaStatusService schemaStatusService;

    private AesCipher aesCipher;
    private ObjectMapper objectMapper;
    private SyncConfigService service;

    @Before
    public void setUp() {
        aesCipher = new AesCipher(VALID_KEY);
        objectMapper = new ObjectMapper();
        // SyncScheduler 为 null（Phase 7 前不注册）
        service = new SyncConfigService(syncConfigRepository, subjectRepository,
                metadataConfigService, schemaStatusService, aesCipher, null, objectMapper);

        // 默认 subject 存在，schemaStatus=APPLIED
        SubjectDTO subject = new SubjectDTO();
        subject.setId(1L);
        subject.setCode(SUBJECT_CODE);
        subject.setName("客户");
        subject.setIsDelete(0);
        when(subjectRepository.findByCode(SUBJECT_CODE)).thenReturn(subject);

        FieldConfigDTO field = new FieldConfigDTO();
        field.setFieldName("credit_code");
        when(metadataConfigService.getActiveFieldDTOs(SUBJECT_CODE))
                .thenReturn(Collections.singletonList(field));

        when(schemaStatusService.computeStatus(eq(SUBJECT_CODE), anyList()))
                .thenReturn(SchemaStatus.APPLIED);
    }

    private SyncConfigRequest buildHttpScheduleRequest() {
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setUrl("http://api.example.com/data");
        ds.setMethod("GET");

        FieldMapping fm = new FieldMapping("id", "credit_code");

        SyncConfigRequest req = new SyncConfigRequest();
        req.setSyncMode("SCHEDULE");
        req.setProviderType("HTTP");
        req.setCronExpr(VALID_CRON);
        req.setDatasourceConfig(ds);
        req.setFieldMapping(Collections.singletonList(fm));
        req.setSyncAction("DELTA");
        req.setStatus(1);
        return req;
    }

    // -------------------------------------------------------------------------
    // save() 正向场景
    // -------------------------------------------------------------------------

    @Test
    public void save_valid_http_schedule_should_succeed() {
        SyncConfigRequest req = buildHttpScheduleRequest();

        assertThatCode(() -> service.save(SUBJECT_CODE, req, "admin"))
                .doesNotThrowAnyException();

        verify(syncConfigRepository).save(any(SyncConfigEntity.class));
    }

    // -------------------------------------------------------------------------
    // save() 校验场景
    // -------------------------------------------------------------------------

    @Test
    public void save_event_mode_with_cronExpr_should_fail() {
        SyncConfigRequest req = buildHttpScheduleRequest();
        req.setSyncMode("EVENT");
        req.setCronExpr(VALID_CRON); // EVENT 时不允许传 cronExpr

        assertThatThrownBy(() -> service.save(SUBJECT_CODE, req, "admin"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("cronExpr must be null or empty when syncMode=EVENT");
    }

    @Test
    public void save_invalid_cronExpr_should_fail() {
        SyncConfigRequest req = buildHttpScheduleRequest();
        req.setCronExpr("not-a-cron");

        assertThatThrownBy(() -> service.save(SUBJECT_CODE, req, "admin"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("Invalid cronExpr");
    }

    @Test
    public void save_fieldMapping_target_not_in_metadata_should_fail() {
        SyncConfigRequest req = buildHttpScheduleRequest();
        // 替换为不存在的 target
        req.setFieldMapping(Collections.singletonList(new FieldMapping("id", "nonexistent_field")));

        assertThatThrownBy(() -> service.save(SUBJECT_CODE, req, "admin"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("nonexistent_field");
    }

    @Test
    public void save_schema_not_applied_should_fail() {
        when(schemaStatusService.computeStatus(eq(SUBJECT_CODE), anyList()))
                .thenReturn(SchemaStatus.PENDING);

        SyncConfigRequest req = buildHttpScheduleRequest();

        assertThatThrownBy(() -> service.save(SUBJECT_CODE, req, "admin"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("schemaStatus");
    }

    @Test
    public void save_subject_not_found_should_fail() {
        when(subjectRepository.findByCode(SUBJECT_CODE)).thenReturn(null);

        SyncConfigRequest req = buildHttpScheduleRequest();

        assertThatThrownBy(() -> service.save(SUBJECT_CODE, req, "admin"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("Subject not found");
    }

    @Test
    public void save_http_missing_url_should_fail() {
        SyncConfigRequest req = buildHttpScheduleRequest();
        req.getDatasourceConfig().setUrl(null);

        assertThatThrownBy(() -> service.save(SUBJECT_CODE, req, "admin"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("url is required");
    }

    // -------------------------------------------------------------------------
    // get() 场景
    // -------------------------------------------------------------------------

    @Test
    public void get_should_return_null_when_no_config() {
        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(null);
        assertThat(service.get(SUBJECT_CODE)).isNull();
    }

    @Test
    public void get_should_mask_password_in_datasource_config() throws Exception {
        String encryptedPwd = aesCipher.encrypt("secret123");
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setJdbcUrl("jdbc:mysql://localhost/db");
        ds.setUsername("root");
        ds.setPassword(encryptedPwd);
        ds.setQuerySql("SELECT * FROM t");
        String dsJson = objectMapper.writeValueAsString(ds);

        SyncConfigEntity entity = new SyncConfigEntity();
        entity.setSubjectCode(SUBJECT_CODE);
        entity.setSubjectName("客户");
        entity.setSyncMode("SCHEDULE");
        entity.setProviderType("DB");
        entity.setCronExpr(VALID_CRON);
        entity.setDatasourceConfig(dsJson);
        entity.setFieldMapping("[{\"source\":\"id\",\"target\":\"credit_code\"}]");
        entity.setSyncAction("DELTA");
        entity.setStatus(1);

        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(entity);

        SyncConfigDTO dto = service.get(SUBJECT_CODE);
        assertThat(dto).isNotNull();
        assertThat(dto.getDatasourceConfig()).isNotNull();
        // 密码已掩码
        assertThat(dto.getDatasourceConfig().getPassword()).isEqualTo("****");
        // JDBC URL 不掩码
        assertThat(dto.getDatasourceConfig().getJdbcUrl()).isEqualTo("jdbc:mysql://localhost/db");
    }

    // -------------------------------------------------------------------------
    // US2: save() 组合校验场景（T005/T006/T007）
    // -------------------------------------------------------------------------

    @Test
    public void save_mq_schedule_combination_should_fail() {
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setBootstrapServers("localhost:9092");

        SyncConfigRequest req = new SyncConfigRequest();
        req.setSyncMode("SCHEDULE");
        req.setProviderType("MQ");
        req.setCronExpr(VALID_CRON);
        req.setDatasourceConfig(ds);
        req.setFieldMapping(Collections.singletonList(new FieldMapping("id", "credit_code")));
        req.setSyncAction("DELTA");
        req.setStatus(1);

        assertThatThrownBy(() -> service.save(SUBJECT_CODE, req, "admin"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("MQ");
    }

    @Test
    public void save_db_delta_without_placeholder_should_fail() {
        SyncDataSourceConfig ds = new SyncDataSourceConfig();
        ds.setJdbcUrl("jdbc:mysql://localhost/db");
        ds.setUsername("root");
        ds.setQuerySql("SELECT * FROM customer");  // 无占位符

        SyncConfigRequest req = new SyncConfigRequest();
        req.setSyncMode("SCHEDULE");
        req.setProviderType("DB");
        req.setCronExpr(VALID_CRON);
        req.setDatasourceConfig(ds);
        req.setFieldMapping(Collections.singletonList(new FieldMapping("id", "credit_code")));
        req.setSyncAction("DELTA");
        req.setStatus(1);

        assertThatThrownBy(() -> service.save(SUBJECT_CODE, req, "admin"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("lastSyncTime");
    }

    @Test
    public void update_with_masked_password_should_preserve_original() throws Exception {
        String encryptedPwd = aesCipher.encrypt("realSecret");

        SyncDataSourceConfig existingDs = new SyncDataSourceConfig();
        existingDs.setJdbcUrl("jdbc:mysql://localhost/db");
        existingDs.setUsername("root");
        existingDs.setPassword(encryptedPwd);
        existingDs.setQuerySql("SELECT * FROM t WHERE updated_at > '${lastSyncTime}'");

        SyncConfigEntity existing = new SyncConfigEntity();
        existing.setSubjectCode(SUBJECT_CODE);
        existing.setProviderType("DB");
        existing.setSyncMode("SCHEDULE");
        existing.setDatasourceConfig(objectMapper.writeValueAsString(existingDs));
        existing.setFieldMapping("[]");
        existing.setSyncAction("DELTA");
        existing.setStatus(1);
        when(syncConfigRepository.findBySubjectCode(SUBJECT_CODE)).thenReturn(existing);

        SyncDataSourceConfig maskedDs = new SyncDataSourceConfig();
        maskedDs.setJdbcUrl("jdbc:mysql://localhost/db");
        maskedDs.setUsername("root");
        maskedDs.setPassword("****");
        maskedDs.setQuerySql("SELECT * FROM t WHERE updated_at > '${lastSyncTime}'");

        SyncConfigRequest req = new SyncConfigRequest();
        req.setSyncMode("SCHEDULE");
        req.setProviderType("DB");
        req.setCronExpr(VALID_CRON);
        req.setDatasourceConfig(maskedDs);
        req.setFieldMapping(Collections.singletonList(new FieldMapping("id", "credit_code")));
        req.setSyncAction("DELTA");
        req.setStatus(1);

        service.save(SUBJECT_CODE, req, "admin");

        verify(syncConfigRepository).save(argThat(entity -> {
            try {
                SyncDataSourceConfig saved = objectMapper.readValue(
                        entity.getDatasourceConfig(), SyncDataSourceConfig.class);
                return !"****".equals(saved.getPassword());
            } catch (Exception e) {
                return false;
            }
        }));
    }
}
