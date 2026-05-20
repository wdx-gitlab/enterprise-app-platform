package com.ruijie.dapengine;

import com.ruijie.dapengine.admin.controller.GlobalExceptionHandler;
import com.ruijie.dapengine.admin.controller.MetadataConfigController;
import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.admin.service.SchemaStatusService;
import com.ruijie.dapengine.autoconfigure.DapEngineAutoConfiguration;
import com.ruijie.dapengine.migration.DapEngineSchemaInitializer;
import com.ruijie.dapengine.repository.MetadataRepository;
import com.ruijie.dapengine.repository.SubjectRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 元数据管理集成冒烟测试。
 * 验证 ApplicationContextRunner（H2）启动后 Admin API 的完整生命周期。
 * 通过 POST→GET→DELETE→GET 验证 subject 生命周期，并校验 schemaStatus=PENDING。
 */
@RunWith(SpringRunner.class)
public class MetadataManagementIntegrationTest {

    private static final String H2_URL_TPL =
        "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String VALID_KEY = "TestEncryptKey32CharactersLong!!";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(JacksonAutoConfiguration.class, DapEngineAutoConfiguration.class);

    @Test
    public void should_register_all_admin_beans() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "e2e_beans"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            assertThat(ctx).hasSingleBean(SubjectRepository.class);
            assertThat(ctx).hasSingleBean(MetadataRepository.class);
            assertThat(ctx).hasSingleBean(SchemaStatusService.class);
            assertThat(ctx).hasSingleBean(MetadataConfigService.class);
        });
    }

    @Test
    public void should_complete_subject_lifecycle_via_service() {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "e2e_lifecycle"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);

            // POST: 创建
            com.ruijie.dapengine.common.model.SubjectRequest req = new com.ruijie.dapengine.common.model.SubjectRequest();
            com.ruijie.dapengine.common.model.SubjectRequest.SubjectInfo info = new com.ruijie.dapengine.common.model.SubjectRequest.SubjectInfo();
            info.setCode("CUST_E2E");
            info.setName("E2E 客户");
            req.setSubject(info);
            req.setFields(java.util.Collections.emptyList());

            com.ruijie.dapengine.common.model.SubjectDTO created = svc.saveSubjectConfig("CUST_E2E", req, "admin");
            assertThat(created.getCode()).isEqualTo("CUST_E2E");
            // 动态表不存在，schemaStatus 应为 PENDING
            assertThat(created.getSchemaStatus()).isEqualTo(com.ruijie.dapengine.common.enums.SchemaStatus.PENDING);

            // GET: 查询列表
            java.util.List<com.ruijie.dapengine.common.model.SubjectDTO> list = svc.listSubjects();
            assertThat(list).hasSize(1);
            assertThat(list.get(0).getCode()).isEqualTo("CUST_E2E");

            // DELETE: 逻辑删除
            svc.deleteSubject("CUST_E2E", "admin");

            // GET: 删除后不应返回数据
            java.util.List<com.ruijie.dapengine.common.model.SubjectDTO> afterDelete = svc.listSubjects();
            assertThat(afterDelete).isEmpty();
        });
    }

    @Test
    public void should_complete_lifecycle_via_mockmvc() throws Exception {
        contextRunner.withPropertyValues(
            "dap.engine.tenant-id=t1", "dap.engine.app-code=app1",
            "rj.unify.engine.datasource.url=" + String.format(H2_URL_TPL, "e2e_mvc"),
            "rj.unify.engine.datasource.username=sa", "rj.unify.engine.datasource.password=",
            "dap.engine.security.encrypt-key=" + VALID_KEY
        ).run(ctx -> {
            MetadataConfigService svc = ctx.getBean(MetadataConfigService.class);
            DapEngineSchemaInitializer schemaInit = ctx.getBean(DapEngineSchemaInitializer.class);
            MetadataConfigController controller = new MetadataConfigController(svc, schemaInit);
            MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

            // POST 创建
            String body = "{\"subject\":{\"code\":\"ITEM_E2E\",\"name\":\"E2E条目\",\"isTree\":false,\"status\":1},\"fields\":[]}";
            mockMvc.perform(post("/dap-engine/admin/metadata/subjects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.schemaStatus").value("PENDING"));

            // GET 查询列表
            mockMvc.perform(get("/dap-engine/admin/metadata/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("ITEM_E2E"));

            // DELETE 删除
            mockMvc.perform(delete("/dap-engine/admin/metadata/subjects/ITEM_E2E"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

            // GET 确认已删除
            mockMvc.perform(get("/dap-engine/admin/metadata/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
        });
    }
}
