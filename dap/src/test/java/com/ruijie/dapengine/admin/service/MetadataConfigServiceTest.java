package com.ruijie.dapengine.admin.service;

import com.ruijie.dapengine.autoconfigure.DapEngineAutoConfiguration;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.FieldConfigDTO;
import com.ruijie.dapengine.common.model.FieldConfigRequest;
import com.ruijie.dapengine.common.model.SubjectDTO;
import com.ruijie.dapengine.common.model.SubjectRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * MetadataConfigService 业务逻辑单元测试（H2 内存数据库）。
 * 覆盖 US1（Subject 生命周期）和 US2（字段管理）的关键场景。
 */
@RunWith(SpringRunner.class)
public class MetadataConfigServiceTest {

    private static final String H2_URL_TPL =
        "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(JacksonAutoConfiguration.class, DapEngineAutoConfiguration.class);

    private SubjectRequest buildRequest(String code, String name, boolean isTree,
                                        List<FieldConfigRequest> fields) {
        SubjectRequest req = new SubjectRequest();
        SubjectRequest.SubjectInfo info = new SubjectRequest.SubjectInfo();
        info.setCode(code);
        info.setName(name);
        info.setTree(isTree);
        info.setStatus(1);
        req.setSubject(info);
        req.setFields(fields);
        return req;
    }

    private FieldConfigRequest buildField(String name, String type, String label, int sortOrder) {
        FieldConfigRequest f = new FieldConfigRequest();
        f.setFieldName(name);
        f.setFieldType(type);
        f.setFieldLabel(label);
        f.setSortOrder(sortOrder);
        return f;
    }

    private FieldConfigRequest buildField(String name, String type, int maxLength, String label, int sortOrder) {
        FieldConfigRequest field = buildField(name, type, label, sortOrder);
        field.setMaxLength(maxLength);
        return field;
    }

    // -------------------------------------------------------------------------
    // US1: Subject 生命周期
    // -------------------------------------------------------------------------

    @Test
    public void should_validateSubjectCode_reject_lowercase() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_code_lower"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            assertThatThrownBy(() -> svc.validateSubjectCode("customer"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("subject.code 格式不合");
        });
    }

    @Test
    public void should_validateSubjectCode_accept_valid_code() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_code_valid"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            // 不应抛异常
            svc.validateSubjectCode("CUSTOMER");
            svc.validateSubjectCode("ORDER_ITEM");
        });
    }

    @Test
    public void should_deleteSubject_set_is_delete() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_del"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            svc.saveSubjectConfig("SUPPLIER", buildRequest("SUPPLIER", "供应商", false,
                Collections.<FieldConfigRequest>emptyList()), "admin");

            List<SubjectDTO> before = svc.listSubjects();
            assertThat(before).hasSize(1);

            svc.deleteSubject("SUPPLIER", "admin");

            List<SubjectDTO> after = svc.listSubjects();
            assertThat(after).isEmpty();
        });
    }

    @Test
    public void should_deleteSubject_throw_for_nonexistent() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_del_notfound"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            assertThatThrownBy(() -> svc.deleteSubject("NOEXIST", "admin"))
                .isInstanceOf(DapValidationException.class);
        });
    }

    // -------------------------------------------------------------------------
    // US2: 字段元数据管理
    // -------------------------------------------------------------------------

    @Test
    public void should_auto_fill_system_fields_on_create() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_sys_fields"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            FieldConfigRequest f1 = buildField("credit_code", "STRING", "信用代码", 10);
            SubjectDTO result = svc.saveSubjectConfig("CUSTOMER",
                    buildRequest("CUSTOMER", "客户", false, Collections.singletonList(f1)), "admin");

            List<FieldConfigDTO> fields = result.getFields();
            // 应包含系统字段 code、name，以及自定义字段 credit_code
            assertThat(fields).hasSize(3);
            assertThat(fields.stream().anyMatch(f -> "code".equals(f.getFieldName()) && f.isSystem())).isTrue();
            assertThat(fields.stream().anyMatch(f -> "name".equals(f.getFieldName()) && f.isSystem())).isTrue();
            assertThat(fields.stream().anyMatch(f -> "credit_code".equals(f.getFieldName()) && !f.isSystem())).isTrue();
        });
    }

    @Test
    public void should_auto_fill_parent_code_when_is_tree() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_tree"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            SubjectDTO result = svc.saveSubjectConfig("CATEGORY",
                    buildRequest("CATEGORY", "类别", true, Collections.<FieldConfigRequest>emptyList()), "admin");

            List<FieldConfigDTO> fields = result.getFields();
            // code + name + parent_code = 3 个系统字段
            assertThat(fields.stream().anyMatch(f -> "parent_code".equals(f.getFieldName()))).isTrue();
        });
    }

    @Test
    public void should_deprecate_field_when_removed_from_request() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_deprecate"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            FieldConfigRequest f1 = buildField("field_a", "STRING", "字段A", 10);
            FieldConfigRequest f2 = buildField("field_b", "STRING", "字段B", 20);
            svc.saveSubjectConfig("PRODUCT",
                    buildRequest("PRODUCT", "产品", false, Arrays.asList(f1, f2)), "admin");

            // 第二次 PUT 去掉 field_b
            SubjectDTO updated = svc.saveSubjectConfig("PRODUCT",
                    buildRequest("PRODUCT", "产品", false, Collections.singletonList(f1)), "admin");

            // field_b 应被废弃（isDelete=1），仍然在列表中
            List<FieldConfigDTO> all = updated.getFields();
            FieldConfigDTO fieldB = null;
            for (FieldConfigDTO f : all) {
                if ("field_b".equals(f.getFieldName())) {
                    fieldB = f;
                }
            }
            assertThat(fieldB).isNotNull();
            assertThat(fieldB.getIsDelete()).isEqualTo(1);
        });
    }

    @Test
    public void should_reactivate_deprecated_field() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_reactivate"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            FieldConfigRequest f1 = buildField("old_field", "STRING", "旧字段", 10);
            // 创建后废弃
            svc.saveSubjectConfig("ORDER",
                    buildRequest("ORDER", "订单", false, Collections.singletonList(f1)), "admin");
            svc.saveSubjectConfig("ORDER",
                    buildRequest("ORDER", "订单", false, Collections.<FieldConfigRequest>emptyList()), "admin");

            // 重新启用
            SubjectDTO reactivated = svc.saveSubjectConfig("ORDER",
                    buildRequest("ORDER", "订单", false, Collections.singletonList(f1)), "admin");
            List<FieldConfigDTO> fields = reactivated.getFields();
            FieldConfigDTO oldField = null;
            for (FieldConfigDTO f : fields) {
                if ("old_field".equals(f.getFieldName())) {
                    oldField = f;
                }
            }
            assertThat(oldField).isNotNull();
            assertThat(oldField.getIsDelete()).isEqualTo(0);
        });
    }

    @Test
    public void should_allow_compatible_type_widening_STRING_to_STRING_LONG() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_widen_ok"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            FieldConfigRequest f1 = buildField("remark", "STRING", "备注", 10);
            svc.saveSubjectConfig("ITEM",
                    buildRequest("ITEM", "条目", false, Collections.singletonList(f1)), "admin");

            FieldConfigRequest f1Updated = buildField("remark", "STRING_LONG", "备注", 10);
            // 不应抛异常
            svc.saveSubjectConfig("ITEM",
                    buildRequest("ITEM", "条目", false, Collections.singletonList(f1Updated)), "admin");
        });
    }

    @Test
    public void should_persist_custom_string_max_length() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_max_length_persist"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            FieldConfigRequest customField = buildField("short_name", "STRING", 50, "简称", 10);

            SubjectDTO result = svc.saveSubjectConfig("CUSTOMER",
                buildRequest("CUSTOMER", "客户", false, Collections.singletonList(customField)), "admin");

            FieldConfigDTO targetField = result.getFields().stream()
                .filter(field -> "short_name".equals(field.getFieldName()))
                .findFirst()
                .orElse(null);
            assertThat(targetField).isNotNull();
            assertThat(targetField.getMaxLength()).isEqualTo(50);
        });
    }

    @Test
    public void should_reject_string_max_length_shrinking() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_max_length_shrink"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            FieldConfigRequest originalField = buildField("remark", "STRING", 500, "备注", 10);
            svc.saveSubjectConfig("ITEM",
                buildRequest("ITEM", "条目", false, Collections.singletonList(originalField)), "admin");

            FieldConfigRequest shrinkField = buildField("remark", "STRING", 50, "备注", 10);
            assertThatThrownBy(() -> svc.saveSubjectConfig("ITEM",
                buildRequest("ITEM", "条目", false, Collections.singletonList(shrinkField)), "admin"))
                .isInstanceOf(DapValidationException.class)
                .hasMessageContaining("最大长度不允许缩小");
        });
    }

    @Test
    public void should_reject_incompatible_type_change_STRING_to_INT() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_widen_bad"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            FieldConfigRequest f1 = buildField("amount", "STRING", "金额", 10);
            svc.saveSubjectConfig("BILL",
                    buildRequest("BILL", "账单", false, Collections.singletonList(f1)), "admin");

            FieldConfigRequest f1Bad = buildField("amount", "INT", "数量", 10);
            assertThatThrownBy(() ->
                    svc.saveSubjectConfig("BILL",
                            buildRequest("BILL", "账单", false, Collections.singletonList(f1Bad)), "admin"))
                    .isInstanceOf(DapValidationException.class)
                    .hasMessageContaining("类型变更不安全");
        });
    }

    @Test
    public void should_reject_reserved_field_name() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_reserved"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            FieldConfigRequest bad = buildField("id", "STRING", "ID", 10);
            assertThatThrownBy(() ->
                    svc.saveSubjectConfig("DEPT",
                            buildRequest("DEPT", "部门", false, Collections.singletonList(bad)), "admin"))
                    .isInstanceOf(DapValidationException.class)
                    .hasMessageContaining("系统保留字");
        });
    }

    @Test
    public void should_reject_ENUM_without_dictCode() {
        ApplicationContextRunner runner = contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_enum_nodict"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY);
        runner.run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            FieldConfigRequest bad = new FieldConfigRequest();
            bad.setFieldName("status_type");
            bad.setFieldType("ENUM");
            bad.setFieldLabel("状态类型");
            // dictCode 不填
            assertThatThrownBy(() ->
                    svc.saveSubjectConfig("REGION",
                            buildRequest("REGION", "地区", false, Collections.singletonList(bad)), "admin"))
                    .isInstanceOf(DapValidationException.class)
                    .hasMessageContaining("dictCode 不能为空");
        });
    }

    // -------------------------------------------------------------------------
    // T009: validateSubject / listSubjectCodes / getActiveFieldDTOs 新方法测试
    // -------------------------------------------------------------------------

    @Test
    public void validateSubject_throws_for_nonexistent() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_validate_nonexist"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            assertThatThrownBy(() -> svc.validateSubject("NOEXIST"))
                .isInstanceOf(com.ruijie.dapengine.common.exception.DapValidationException.class)
                .hasMessageContaining("不存在或已删除");
        });
    }

    @Test
    public void validateSubject_throws_for_deleted() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_validate_deleted"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            // 先创建 subject，再删除
            svc.saveSubjectConfig("DELETED_SUBJECT",
                buildRequest("DELETED_SUBJECT", "待删除主数据", false, Collections.<FieldConfigRequest>emptyList()),
                "admin");
            svc.deleteSubject("DELETED_SUBJECT", "admin");
            // 验证 validateSubject 抛出异常
            assertThatThrownBy(() -> svc.validateSubject("DELETED_SUBJECT"))
                .isInstanceOf(com.ruijie.dapengine.common.exception.DapValidationException.class)
                .hasMessageContaining("不存在或已删除");
        });
    }

    @Test
    public void listSubjectCodes_returns_active_codes() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_list_codes"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            svc.saveSubjectConfig("CODE_A",
                buildRequest("CODE_A", "A", false, Collections.<FieldConfigRequest>emptyList()), "admin");
            svc.saveSubjectConfig("CODE_B",
                buildRequest("CODE_B", "B", false, Collections.<FieldConfigRequest>emptyList()), "admin");
            svc.deleteSubject("CODE_B", "admin");

            List<String> codes = svc.listSubjectCodes();
            assertThat(codes).contains("CODE_A");
            assertThat(codes).doesNotContain("CODE_B");
        });
    }

    @Test
    public void getActiveFieldDTOs_returns_fields() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_active_fields"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            FieldConfigRequest f = buildField("credit_code", "STRING", "信用代码", 10);
            svc.saveSubjectConfig("VENDOR",
                buildRequest("VENDOR", "供应商", false, Collections.singletonList(f)), "admin");

            List<FieldConfigDTO> fields = svc.getActiveFieldDTOs("VENDOR");
            // 至少包含系统字段 code/name 和自定义字段 credit_code
            assertThat(fields).isNotEmpty();
            boolean hasField = false;
            for (FieldConfigDTO fd : fields) {
                if ("credit_code".equals(fd.getFieldName())) {
                    hasField = true;
                    break;
                }
            }
            assertThat(hasField).isTrue();
        });
    }

    @Test
    public void should_not_allow_subject_code_change() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "svc_code_immutable"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            // 先创建 VENDOR
            svc.saveSubjectConfig("VENDOR",
                buildRequest("VENDOR", "供应商", false, Collections.<FieldConfigRequest>emptyList()),
                "admin");
            // 再尝试以 path param=VENDOR、body.code=VENDOR_NEW 提交更新
            assertThatThrownBy(() ->
                svc.saveSubjectConfig("VENDOR",
                    buildRequest("VENDOR_NEW", "供应商更新", false, Collections.<FieldConfigRequest>emptyList()),
                    "admin"))
                .isInstanceOf(DapValidationException.class);
        });
    }
}

