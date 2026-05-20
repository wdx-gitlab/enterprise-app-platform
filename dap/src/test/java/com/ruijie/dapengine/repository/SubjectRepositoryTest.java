package com.ruijie.dapengine.repository;

import com.ruijie.dapengine.autoconfigure.DapEngineAutoConfiguration;
import com.ruijie.dapengine.common.model.SubjectDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SubjectRepository 数据访问层单元测试（H2 内存数据库）。
 */
@RunWith(SpringRunner.class)
public class SubjectRepositoryTest {

    private static final String H2_URL_TPL =
        "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(JacksonAutoConfiguration.class, DapEngineAutoConfiguration.class);

    @Test
    public void should_insert_and_findByCode() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "repo_insert"),
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(ctx -> {
                SubjectRepository repo = ctx.getBean(SubjectRepository.class);

                long id = repo.insert("CUSTOMER", "客户", "desc", false, 1, "admin");
                assertThat(id).isGreaterThan(0);

                SubjectDTO found = repo.findByCode("CUSTOMER");
                assertThat(found).isNotNull();
                assertThat(found.getCode()).isEqualTo("CUSTOMER");
                assertThat(found.getName()).isEqualTo("客户");
                assertThat(found.getStatus()).isEqualTo(1);
            });
    }

    @Test
    public void should_return_null_for_nonexistent_code() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "repo_notfound"),
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(ctx -> {
                SubjectRepository repo = ctx.getBean(SubjectRepository.class);
                assertThat(repo.findByCode("NONEXIST")).isNull();
            });
    }

    @Test
    public void should_logicDelete_and_listActive_filters_it_out() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "repo_delete"),
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(ctx -> {
                SubjectRepository repo = ctx.getBean(SubjectRepository.class);

                repo.insert("SUPPLIER", "供应商", null, false, 1, "admin");

                List<SubjectDTO> before = repo.listActive();
                assertThat(before).hasSize(1);

                int rows = repo.logicDelete("SUPPLIER");
                assertThat(rows).isEqualTo(1);

                List<SubjectDTO> after = repo.listActive();
                assertThat(after).isEmpty();
            });
    }

    @Test
    public void should_update_subject_name() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "repo_update"),
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(ctx -> {
                SubjectRepository repo = ctx.getBean(SubjectRepository.class);

                repo.insert("EMPLOYEE", "员工", null, false, 1, "admin");
                repo.update("EMPLOYEE", "员工（更新）", "new desc", false, 1, "admin");

                SubjectDTO found = repo.findByCode("EMPLOYEE");
                assertThat(found.getName()).isEqualTo("员工（更新）");
                assertThat(found.getDescription()).isEqualTo("new desc");
            });
    }

    @Test
    public void should_logicDelete_returns_zero_for_nonexistent() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "repo_del_zero"),
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(ctx -> {
                SubjectRepository repo = ctx.getBean(SubjectRepository.class);
                int rows = repo.logicDelete("NO_SUCH");
                assertThat(rows).isEqualTo(0);
            });
    }
}
