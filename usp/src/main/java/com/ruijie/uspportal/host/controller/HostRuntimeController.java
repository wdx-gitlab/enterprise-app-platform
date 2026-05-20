package com.ruijie.uspportal.host.controller;

import com.ruijie.uspportal.common.ApiResponse;
import com.ruijie.uspportal.host.dto.CurrentContextResponse;
import com.ruijie.uspportal.host.dto.IntegrationCapabilitiesResponse;
import com.ruijie.uspportal.host.service.HostIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 宿主运行时能力控制器。
 *
 * <p>该控制器向宿主前端或集成方暴露当前上下文快照与模块能力说明，
 * 用于宿主接入时获取登录用户、租户、门户运行态以及模块能力边界。</p>
 */
@RestController
@RequestMapping("/api")
public class HostRuntimeController {

    private final HostIntegrationService hostIntegrationService;

    @Autowired
    public HostRuntimeController(HostIntegrationService hostIntegrationService) {
        this.hostIntegrationService = hostIntegrationService;
    }

    /**
     * 查询当前请求上下文快照。
     *
     * @return 当前用户、租户、组织与门户运行时信息
     */
    @GetMapping("/context/current")
    public ApiResponse<CurrentContextResponse> currentContext() {
        return ApiResponse.success(hostIntegrationService.currentContext());
    }

    /**
     * 查询当前模块对宿主开放的能力说明。
     *
     * @return 模块版本、支持能力与认证模式等信息
     */
    @GetMapping("/integration/capabilities")
    public ApiResponse<IntegrationCapabilitiesResponse> capabilities() {
        return ApiResponse.success(hostIntegrationService.capabilities());
    }
}
