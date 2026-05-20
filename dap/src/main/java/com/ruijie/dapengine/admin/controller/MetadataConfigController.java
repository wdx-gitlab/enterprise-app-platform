package com.ruijie.dapengine.admin.controller;

import com.ruijie.dapengine.admin.service.MetadataConfigService;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.Result;
import com.ruijie.dapengine.common.model.SchemaChangeResult;
import com.ruijie.dapengine.common.model.SubjectDTO;
import com.ruijie.dapengine.common.model.SubjectRequest;
import com.ruijie.dapengine.common.util.HeaderUserResolver;
import com.ruijie.dapengine.migration.DapEngineSchemaInitializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 主数据类型与字段元数据管理 Admin API 控制器。
 * 仅在 Spring MVC（DispatcherServlet）可用时注册。
 */
@RestController
@RequestMapping("/dap-engine/admin/metadata")
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
public class MetadataConfigController {

    private final MetadataConfigService metadataConfigService;
    private final DapEngineSchemaInitializer schemaInitializer;

    public MetadataConfigController(MetadataConfigService metadataConfigService,
                                    DapEngineSchemaInitializer schemaInitializer) {
        this.metadataConfigService = metadataConfigService;
        this.schemaInitializer = schemaInitializer;
    }

    /**
     * GET /dap-engine/admin/metadata/subjects
     * 查询所有未删除的 Subject 列表，附带 schemaStatus。
     */
    @GetMapping("/subjects")
    public Result<List<SubjectDTO>> listSubjects() {
        return Result.ok(metadataConfigService.listSubjects());
    }

    /**
     * POST /dap-engine/admin/metadata/subjects
     * 创建 Subject 及其字段配置。
     */
    @PostMapping("/subjects")
    public Result<SubjectDTO> createSubject(@RequestBody SubjectRequest request,
                                            HttpServletRequest httpRequest) {
        if (request.getSubject() == null || request.getSubject().getCode() == null) {
            throw new DapValidationException("[DAP Engine] subject.code 不能为空");
        }
        String subjectCode = request.getSubject().getCode();
        String operatorId = HeaderUserResolver.resolve(httpRequest);
        return Result.ok(metadataConfigService.saveSubjectConfig(subjectCode, request, operatorId));
    }

    /**
     * PUT /dap-engine/admin/metadata/subjects/{subject}
     * 更新 Subject 及其字段配置，路径参数中的 code 优先，忽略 body 中的 code。
     */
    @PutMapping("/subjects/{subject}")
    public Result<SubjectDTO> updateSubject(@PathVariable("subject") String subjectCode,
                                            @RequestBody SubjectRequest request,
                                            HttpServletRequest httpRequest) {
        // 路径参数 code 优先，body 中的 code 忽略
        if (request.getSubject() == null) {
            request.setSubject(new SubjectRequest.SubjectInfo());
        }
        String operatorId = HeaderUserResolver.resolve(httpRequest);
        return Result.ok(metadataConfigService.saveSubjectConfig(subjectCode, request, operatorId));
    }

    /**
     * DELETE /dap-engine/admin/metadata/subjects/{subject}
     * 逻辑删除 Subject。
     */
    @DeleteMapping("/subjects/{subject}")
    public Result<Void> deleteSubject(@PathVariable("subject") String subjectCode,
                                      HttpServletRequest httpRequest) {
        String operatorId = HeaderUserResolver.resolve(httpRequest);
        metadataConfigService.deleteSubject(subjectCode, operatorId);
        return Result.ok();
    }

    /**
     * GET /dap-engine/admin/metadata/subjects/{subject}/fields
     * 获取 Subject 字段列表（含废弃字段），由调用方通过 is_delete 过滤。
     */
    @GetMapping("/subjects/{subject}/fields")
    public Result<SubjectDTO> getFields(@PathVariable("subject") String subjectCode) {
        return Result.ok(metadataConfigService.getFieldsBySubject(subjectCode));
    }

    /**
     * POST /dap-engine/admin/metadata/subjects/{subject}/apply-schema
     * 对指定 Subject 应用 Schema 变更（建表或补列/扩容列）。
     * 幂等操作，结构已同步时返回空 executedDdl。
     */
    @PostMapping("/subjects/{subject}/apply-schema")
    public Result<SchemaChangeResult> applySchema(@PathVariable("subject") String subjectCode) {
        return Result.ok(schemaInitializer.applySchema(subjectCode));
    }
}

