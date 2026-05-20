package com.ruijie.dapengine.repository;

import com.ruijie.dapengine.autoconfigure.DapEngineAutoConfiguration;
import com.ruijie.dapengine.entity.SyncConfigEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.*;

/**
 * SyncConfigRepository 数据访问层单元测试（H2 内存数据库）。
 * 覆盖 save-then-find 与 UPSERT-on-save 场景。
 */
@RunWith(SpringRunner.class)
public class SyncConfigRepositoryTest {

    private static final String H2_URL_TPL =
            "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JacksonAutoConfiguration.class, DapEngineAutoConfiguration.class);

    private SyncConfigEntity buildEntity(String subjectCode, String syncMode, String providerType) {
        SyncConfigEntity entity = new SyncConfigEntity();
        entity.setSubjectId(1L);
        entity.setSubjectCode(subjectCode);
        entity.setSubjectName("测试主题");
        entity.setSyncMode(syncMode);
        entity.setProviderType(providerType);
        entity.setCronExpr("0 0 * * * ?");
        entity.setDatasourceConfig("{\"url\":\"http://example.com\"}");
        entity.setFieldMapping("[{\"source\":\"id\",\"target\":\"code\"}]");
        entity.setSyncAction("DELTA");
        entity.setStatus(1);
        entity.setIsDelete(0);
        entity.setCreatedBy("admin");
        entity.setUpdatedBy("admin");
        return entity;
    }

    @Test
    public void should_save_and_findBySubjectCode() {
        contextRunner
                .withPropertyValues(
                        "dap.engine.tenant-id=t1",
                        "dap.engine.app-code=app1",
                        "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "sync_cfg_save"),
                        "rj.unify.engine.datasource.username=sa",
                        "rj.unify.engine.datasource.password=",
                        "dap.engine.security.encrypt-key=" + VALID_KEY
                )
                .run(ctx -> {
                    SyncConfigRepository repo = ctx.getBean(SyncConfigRepository.class);

                    // 首次 save: INSERT
                    SyncConfigEntity entity = buildEntity("CUSTOMER", "SCHEDULE", "HTTP");
                    repo.save(entity);

                    SyncConfigEntity found = repo.findBySubjectCode("CUSTOMER");
                    assertThat(found).isNotNull();
                    assertThat(found.getSubjectCode()).isEqualTo("CUSTOMER");
                    assertThat(found.getSyncMode()).isEqualTo("SCHEDULE");
                    assertThat(found.getProviderType()).isEqualTo("HTTP");
                    assertThat(found.getStatus()).isEqualTo(1);
                });
    }

    @Test
    public void should_return_null_for_missing_config() {
        contextRunner
                .withPropertyValues(
                        "dap.engine.tenant-id=t1",
                        "dap.engine.app-code=app1",
                        "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "sync_cfg_notfound"),
                        "rj.unify.engine.datasource.username=sa",
                        "rj.unify.engine.datasource.password=",
                        "dap.engine.security.encrypt-key=" + VALID_KEY
                )
                .run(ctx -> {
                    SyncConfigRepository repo = ctx.getBean(SyncConfigRepository.class);
                    assertThat(repo.findBySubjectCode("NONEXISTENT")).isNull();
                });
    }

    @Test
    public void should_upsert_on_second_save() {
        contextRunner
                .withPropertyValues(
                        "dap.engine.tenant-id=t1",
                        "dap.engine.app-code=app1",
                        "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "sync_cfg_upsert"),
                        "rj.unify.engine.datasource.username=sa",
                        "rj.unify.engine.datasource.password=",
                        "dap.engine.security.encrypt-key=" + VALID_KEY
                )
                .run(ctx -> {
                    SyncConfigRepository repo = ctx.getBean(SyncConfigRepository.class);

                    // 首次 INSERT
                    SyncConfigEntity e1 = buildEntity("VENDOR", "SCHEDULE", "HTTP");
                    e1.setStatus(1);
                    repo.save(e1);

                    // 第二次 UPSERT：更新 status=0（模拟停用）
                    SyncConfigEntity e2 = buildEntity("VENDOR", "SCHEDULE", "DB");
                    e2.setStatus(0);
                    repo.save(e2);

                    SyncConfigEntity updated = repo.findBySubjectCode("VENDOR");
                    assertThat(updated).isNotNull();
                    // 第二次保存的字段生效
                    assertThat(updated.getProviderType()).isEqualTo("DB");
                    assertThat(updated.getStatus()).isEqualTo(0);
                    // created_by 保留首次值（INSERT 时写入）
                    assertThat(updated.getCreatedBy()).isEqualTo("admin");
                });
    }
}
