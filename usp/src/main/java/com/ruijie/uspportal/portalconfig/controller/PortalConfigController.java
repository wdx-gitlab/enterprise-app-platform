package com.ruijie.uspportal.portalconfig.controller;

import com.ruijie.uspportal.common.ApiResponse;
import com.ruijie.uspportal.portalconfig.dto.FeatureFlagEvaluateRequest;
import com.ruijie.uspportal.portalconfig.dto.FeatureFlagSaveRequest;
import com.ruijie.uspportal.portalconfig.dto.PortalParamSaveRequest;
import com.ruijie.uspportal.portalconfig.entity.FeatureFlagEntity;
import com.ruijie.uspportal.portalconfig.entity.FeatureFlagRuleEntity;
import com.ruijie.uspportal.portalconfig.entity.PortalParamEntity;
import com.ruijie.uspportal.portalconfig.entity.PortalParamHistoryEntity;
import com.ruijie.uspportal.portalconfig.service.PortalConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 门户配置管理控制器。
 *
 * <p>负责门户参数、灰度开关及规则的维护与查询，并提供灰度规则评估能力。</p>
 */
@RestController
@Validated
@RequestMapping("/api")
public class PortalConfigController {

    private final PortalConfigService portalConfigService;

    @Autowired
    public PortalConfigController(PortalConfigService portalConfigService) {
        this.portalConfigService = portalConfigService;
    }

    /**
     * 查询门户参数列表。
     *
     * @return 参数集合
     */
    @GetMapping("/portal-params")
    public ApiResponse<List<PortalParamEntity>> listParams() {
        return ApiResponse.success(portalConfigService.listParams());
    }

    /**
     * 创建门户参数。
     *
     * @param request 参数保存请求
     * @return 创建后的参数信息
     */
    @PostMapping("/portal-params")
    public ApiResponse<PortalParamEntity> saveParam(@Valid @RequestBody PortalParamSaveRequest request) {
        return ApiResponse.success("创建成功", portalConfigService.saveParam(request));
    }

    /**
     * 更新指定门户参数。
     *
     * @param paramKey 参数键
     * @param request 参数保存请求
     * @return 更新后的参数信息
     */
    @PutMapping("/portal-params/{paramKey}")
    public ApiResponse<PortalParamEntity> updateParam(@PathVariable String paramKey,
                                                      @Valid @RequestBody PortalParamSaveRequest request) {
        return ApiResponse.success("更新成功", portalConfigService.updateParam(paramKey, request));
    }

    /**
     * 查询指定门户参数的变更历史。
     *
     * @param paramKey 参数键
     * @return 参数历史记录
     */
    @GetMapping("/portal-params/{paramKey}/history")
    public ApiResponse<List<PortalParamHistoryEntity>> histories(@PathVariable String paramKey) {
        return ApiResponse.success(portalConfigService.listHistories(paramKey));
    }

    /**
     * 查询功能开关列表。
     *
     * @return 功能开关集合
     */
    @GetMapping("/feature-flags")
    public ApiResponse<List<FeatureFlagEntity>> listFlags() {
        return ApiResponse.success(portalConfigService.listFlags());
    }

    /**
     * 创建功能开关。
     *
     * @param request 功能开关保存请求
     * @return 创建后的功能开关信息
     */
    @PostMapping("/feature-flags")
    public ApiResponse<FeatureFlagEntity> saveFlag(@Valid @RequestBody FeatureFlagSaveRequest request) {
        return ApiResponse.success("创建成功", portalConfigService.saveFlag(request));
    }

    /**
     * 更新指定功能开关。
     *
     * @param id 功能开关主键
     * @param request 功能开关保存请求
     * @return 更新后的功能开关信息
     */
    @PutMapping("/feature-flags/{id}")
    public ApiResponse<FeatureFlagEntity> updateFlag(@PathVariable Long id,
                                                     @Valid @RequestBody FeatureFlagSaveRequest request) {
        return ApiResponse.success("更新成功", portalConfigService.updateFlag(id, request));
    }

    /**
     * 查询指定功能开关的规则列表。
     *
     * @param id 功能开关主键
     * @return 功能开关规则集合
     */
    @GetMapping("/feature-flags/{id}/rules")
    public ApiResponse<List<FeatureFlagRuleEntity>> listRules(@PathVariable Long id) {
        return ApiResponse.success(portalConfigService.listRules(id));
    }

    /**
     * 评估灰度规则是否命中。
     *
     * @param request 灰度评估请求
     * @return 评估结果与命中原因
     */
    @PostMapping("/feature-flags/evaluate")
    public ApiResponse<Map<String, Object>> evaluate(@RequestBody FeatureFlagEvaluateRequest request) {
        return ApiResponse.success(portalConfigService.evaluate(request));
    }
}
