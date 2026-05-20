package com.ruijie.dapengine;

import com.ruijie.dapengine.admin.controller.SyncConfigController;
import com.ruijie.dapengine.admin.service.SyncConfigService;
import com.ruijie.dapengine.autoconfigure.DapEngineAutoConfiguration;
import com.ruijie.dapengine.provider.DataProvider;
import com.ruijie.dapengine.provider.DbDataProvider;
import com.ruijie.dapengine.provider.HttpDataProvider;
import com.ruijie.dapengine.repository.CheckpointRepository;
import com.ruijie.dapengine.repository.SyncConfigRepository;
import com.ruijie.dapengine.repository.SyncLogRepository;
import com.ruijie.dapengine.sync.FieldMappingService;
import com.ruijie.dapengine.sync.SubjectSyncLockManager;
import com.ruijie.dapengine.sync.SyncExecutor;
import com.ruijie.dapengine.sync.SyncScheduler;
import com.ruijie.dapengine.sync.SyncSchedulerImpl;
import com.ruijie.dapengine.writer.LocalDataWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 集成冒烟测试。
 * 验证 ApplicationContextRunner（H2）启动后同步配置相关 Bean 能正确加载。
 */
@RunWith(SpringRunner.class)
public class SyncConfigIntegrationTest {

    private static final String H2_URL_TPL =
            "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JacksonAutoConfiguration.class, DapEngineAutoConfiguration.class);

    private ApplicationContextRunner baseRunner(String dbName) {
        return contextRunner.withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, dbName),
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
        );
    }

    @Test
    public void should_register_sync_infrastructure_beans() {
        baseRunner("sync_infra").run(ctx -> {
            assertThat(ctx).hasSingleBean(SyncConfigRepository.class);
            assertThat(ctx).hasSingleBean(CheckpointRepository.class);
            assertThat(ctx).hasSingleBean(SyncLogRepository.class);
            assertThat(ctx).hasSingleBean(FieldMappingService.class);
            assertThat(ctx).hasSingleBean(LocalDataWriter.class);
            assertThat(ctx).hasSingleBean(SubjectSyncLockManager.class);
        });
    }

    @Test
    public void should_register_data_provider_beans() {
        baseRunner("sync_providers").run(ctx -> {
            assertThat(ctx).hasSingleBean(HttpDataProvider.class);
            assertThat(ctx).hasSingleBean(DbDataProvider.class);
            // DataProvider list should contain at least 3 implementations
            assertThat(ctx.getBeansOfType(DataProvider.class)).hasSizeGreaterThanOrEqualTo(3);
        });
    }

    @Test
    public void should_register_sync_executor_and_controller() {
        baseRunner("sync_executor").run(ctx -> {
            assertThat(ctx).hasSingleBean(SyncExecutor.class);
            assertThat(ctx).hasSingleBean(SyncConfigService.class);
            assertThat(ctx).hasSingleBean(SyncConfigController.class);
        });
    }

    @Test
    public void should_register_sync_scheduler_impl() {
        baseRunner("sync_scheduler").run(ctx -> {
            assertThat(ctx).hasSingleBean(SyncScheduler.class);
            SyncScheduler scheduler = ctx.getBean(SyncScheduler.class);
            assertThat(scheduler).isInstanceOf(SyncSchedulerImpl.class);
        });
    }

    @Test
    public void should_not_start_when_datasource_url_is_missing() {
        contextRunner.withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=",
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            assertThat(ctx).hasFailed();
        });
    }
}
