package com.ruijie.dapengine.admin.controller;

import com.ruijie.dapengine.admin.service.SyncConfigService;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.enums.TriggerAction;
import com.ruijie.dapengine.common.model.PageResult;
import com.ruijie.dapengine.common.model.Result;
import com.ruijie.dapengine.common.model.SyncConfigDTO;
import com.ruijie.dapengine.common.model.SyncConfigRequest;
import com.ruijie.dapengine.common.model.SyncLogDTO;
import com.ruijie.dapengine.common.model.SyncResultDTO;
import com.ruijie.dapengine.common.model.TestConnectResultDTO;
import com.ruijie.dapengine.common.util.HeaderUserResolver;
import com.ruijie.dapengine.provider.DataProvider;
import com.ruijie.dapengine.repository.SyncLogRepository;
import com.ruijie.dapengine.sync.SyncExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 同步配置管理 Admin API 控制器。
 * 提供同步配置保存与查询端点，仅在 Spring MVC 可用时注册。
 *
 * <p>端点规范：
 * <ul>
 *   <li>EP-1 {@code GET /dap-engine/admin/sync/{subjectCode}} — 查询指定 Subject 的同步配置（敏感字段掩码）</li>
 *   <li>EP-2 {@code POST /dap-engine/admin/sync/{subjectCode}} — 保存（新建或覆盖更新）同步配置</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/dap-engine/admin/sync")
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
public class SyncConfigController {

    private final SyncConfigService syncConfigService;
    private final List<DataProvider> dataProviders;
    private final SyncExecutor syncExecutor;
    private final SyncLogRepository syncLogRepository;

    public SyncConfigController(SyncConfigService syncConfigService,
                                List<DataProvider> dataProviders,
                                SyncExecutor syncExecutor,
                                SyncLogRepository syncLogRepository) {
        this.syncConfigService = syncConfigService;
        this.dataProviders = dataProviders;
        this.syncExecutor = syncExecutor;
        this.syncLogRepository = syncLogRepository;
    }

    /**
     * EP-1 GET /dap-engine/admin/sync/{subjectCode}
     * 查询当前 Subject 的同步配置，密码等敏感字段替换为 "****"。
     *
     * @param subjectCode Subject code（路径参数）
     * @return 同步配置 DTO；未配置时 data 为 null
     */
    @GetMapping("/{subjectCode}")
    public Result<SyncConfigDTO> getConfig(@PathVariable String subjectCode) {
        SyncConfigDTO dto = syncConfigService.get(subjectCode);
        return Result.ok(dto);
    }

    /**
     * EP-2 POST /dap-engine/admin/sync/{subjectCode}
     * 保存同步配置（新建或全量覆盖更新）。
     *
     * <p>校验规则：
     * <ul>
     *   <li>Subject 存在且 schemaStatus=APPLIED</li>
     *   <li>providerType 对应必填字段不为空</li>
     *   <li>syncMode=SCHEDULE 时 cronExpr 必填且格式合法</li>
     *   <li>fieldMapping 不为空且 target 在元数据中存在</li>
     * </ul>
     * 校验失败时通过 {@link com.ruijie.dapengine.common.exception.DapValidationException} 抛出，
     * 由 {@link GlobalExceptionHandler} 统一返回 {@code code=4001}。</p>
     *
     * @param subjectCode Subject code（路径参数）
     * @param request     同步配置请求体（含明文敏感字段）
     * @param httpRequest HTTP 请求（用于提取操作人 Header）
     * @return 保存成功返回 code=0
     */
    @PostMapping("/{subjectCode}")
    public Result<Void> saveConfig(@PathVariable String subjectCode,
                                   @RequestBody SyncConfigRequest request,
                                   HttpServletRequest httpRequest) {
        String operator = HeaderUserResolver.resolve(httpRequest);
        syncConfigService.save(subjectCode, request, operator);
        return Result.ok(null);
    }

    /**
     * EP-3 POST /dap-engine/admin/sync/{subjectCode}/test-connect
     * 试连接：验证数据源可达性，返回 sourceFields 和 ≤5 条样例数据（MQ 仅探测）。
     *
     * <p>始终返回 HTTP 200；连接失败通过 {@code data.success=false} + {@code data.errorMsg} 表示。</p>
     *
     * @param subjectCode Subject code（路径参数）
     * @param request     含 providerType 和 datasourceConfig 的同步配置请求体
     * @return 试连接结果
     */
    @PostMapping("/{subjectCode}/test-connect")
    public Result<TestConnectResultDTO> testConnect(@PathVariable String subjectCode,
                                                    @RequestBody SyncConfigRequest request) {
        String providerType = request.getProviderType();
        DataProvider provider = dataProviders.stream()
                .filter(p -> p.type().equalsIgnoreCase(providerType))
                .findFirst()
                .orElse(null);
        if (provider == null) {
            return Result.ok(TestConnectResultDTO.failure(providerType,
                    "Unsupported providerType: " + providerType));
        }
        TestConnectResultDTO result = provider.testConnect(request.getDatasourceConfig());
        return Result.ok(result);
    }

    /**
     * EP-4 POST /dap-engine/admin/sync/{subjectCode}/trigger?action=DELTA|FULL_REFRESH
     * 手动触发一次同步（DELTA 增量 或 FULL_REFRESH 全量刷新）。
     *
     * @param subjectCode Subject code（路径参数）
     * @param action      触发动作：DELTA（默认）或 FULL_REFRESH
     * @return 同步结果 DTO（success=false 时仍返回 HTTP 200）
     */
    @PostMapping("/{subjectCode}/trigger")
    public Result<SyncResultDTO> trigger(@PathVariable String subjectCode,
                                          @RequestParam(defaultValue = "DELTA") String action) {
        TriggerAction triggerAction;
        try {
            triggerAction = TriggerAction.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new DapValidationException("[DAP Engine] 不合法的 action 参数: " + action
                    + "，合法值：DELTA, FULL_REFRESH");
        }
        SyncResultDTO result = syncExecutor.executeSync(subjectCode, triggerAction);
        return Result.ok(result);
    }

    /**
     * EP-5 GET /dap-engine/admin/sync/{subjectCode}/logs?page=1&size=20
     * 查询指定 Subject 的同步日志，按 created_at 倒序分页。
     *
     * @param subjectCode Subject code（路径参数）
     * @param page        页码（默认 1）
     * @param size        每页条数（默认 20）
     * @return 分页同步日志列表
     */
    @GetMapping("/{subjectCode}/logs")
    public Result<PageResult<SyncLogDTO>> getLogs(@PathVariable String subjectCode,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        PageResult<SyncLogDTO> result = syncLogRepository.findBySubjectCode(subjectCode, page, size);
        return Result.ok(result);
    }
}
