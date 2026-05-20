package com.ruijie.dapengine.repository;

import com.ruijie.dapengine.autoconfigure.DapEngineAutoConfiguration;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * MetadataRepository 数据访问层单元测试（H2 内存数据库）。
 */
@RunWith(SpringRunner.class)
public class MetadataRepositoryTest {

    private static final String H2_URL_TPL =
        "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(JacksonAutoConfiguration.class, DapEngineAutoConfiguration.class);

    @Test
    public void should_insertField_and_findBySubjectId() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "meta_insert"),
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(ctx -> {
                MetadataRepository repo = ctx.getBean(MetadataRepository.class);

                repo.insertField(1L, "CUSTOMER", "客户", "credit_code", "STRING",
                        50, "信用代码", false, null, 10, false, "admin");

                List<FieldConfigDTO> fields = repo.findBySubjectId(1L);
                assertThat(fields).hasSize(1);
                assertThat(fields.get(0).getFieldName()).isEqualTo("credit_code");
                assertThat(fields.get(0).getMaxLength()).isEqualTo(50);
                assertThat(fields.get(0).getIsDelete()).isEqualTo(0);
            });
    }

    @Test
    public void should_setIsDelete_to_1_and_remain_in_findBySubjectId() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "meta_del"),
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(ctx -> {
                MetadataRepository repo = ctx.getBean(MetadataRepository.class);

                repo.insertField(2L, "SUPPLIER", "供应商", "tax_code", "STRING",
                        128, "税号", false, null, 10, false, "admin");

                FieldConfigDTO field = repo.findBySubjectIdAndFieldName(2L, "tax_code");
                assertThat(field).isNotNull();

                repo.setIsDelete(field.getId(), 1, "admin");

                List<FieldConfigDTO> all = repo.findBySubjectId(2L);
                assertThat(all).hasSize(1);
                assertThat(all.get(0).getIsDelete()).isEqualTo(1);
            });
    }

    @Test
    public void should_updateField_label_and_sortOrder() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "meta_upd"),
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(ctx -> {
                MetadataRepository repo = ctx.getBean(MetadataRepository.class);

                repo.insertField(3L, "PRODUCT", "产品", "product_no", "STRING",
                        128, "产品编号", false, null, 10, false, "admin");

                FieldConfigDTO original = repo.findBySubjectIdAndFieldName(3L, "product_no");
                repo.updateField(original.getId(), "STRING_LONG", "产品编号（长）", 1024, false, null, 20, "admin");

                FieldConfigDTO updated = repo.findBySubjectIdAndFieldName(3L, "product_no");
                assertThat(updated.getFieldType()).isEqualTo("STRING_LONG");
                assertThat(updated.getMaxLength()).isEqualTo(1024);
                assertThat(updated.getFieldLabel()).isEqualTo("产品编号（长）");
                assertThat(updated.getSortOrder()).isEqualTo(20);
            });
    }

    @Test
    public void should_findActiveBySubjectId_excludes_deleted_fields() {
        contextRunner
            .withPropertyValues(
                "dap.engine.tenant-id=t1",
                "dap.engine.app-code=app1",
                "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "meta_active"),
                "rj.unify.engine.datasource.username=sa",
                "rj.unify.engine.datasource.password=",
                "dap.engine.security.encrypt-key=" + VALID_KEY
            )
            .run(ctx -> {
                MetadataRepository repo = ctx.getBean(MetadataRepository.class);

                repo.insertField(4L, "DEPT", "部门", "dept_code", "STRING", 128, "部门编码", false, null, 10, false, "admin");
                repo.insertField(4L, "DEPT", "部门", "dept_desc", "STRING", 128, "部门描述", false, null, 20, false, "admin");

                FieldConfigDTO f = repo.findBySubjectIdAndFieldName(4L, "dept_desc");
                repo.setIsDelete(f.getId(), 1, "admin");

                List<FieldConfigDTO> active = repo.findActiveBySubjectId(4L);
                assertThat(active).hasSize(1);
                assertThat(active.get(0).getFieldName()).isEqualTo("dept_code");
            });
    }
}
