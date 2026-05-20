package com.ruijie.uspportal.workbench.controller;

import com.ruijie.uspportal.common.ApiResponse;
import com.ruijie.uspportal.workbench.dto.WidgetSaveRequest;
import com.ruijie.uspportal.workbench.dto.WorkbenchSaveRequest;
import com.ruijie.uspportal.workbench.entity.WorkbenchEntity;
import com.ruijie.uspportal.workbench.entity.WorkbenchWidgetEntity;
import com.ruijie.uspportal.workbench.service.WorkbenchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 工作台管理控制器。
 *
 * <p>提供工作台及组件的查询、创建、更新和删除接口，用于维护门户首页工作台布局。</p>
 */
@RestController
@Validated
public class WorkbenchController {

    private final WorkbenchService workbenchService;

    @Autowired
    public WorkbenchController(WorkbenchService workbenchService) {
        this.workbenchService = workbenchService;
    }

    /**
     * 查询工作台列表。
     *
     * @return 工作台集合
     */
    @GetMapping("/api/workbenches")
    public ApiResponse<List<WorkbenchEntity>> list() {
        return ApiResponse.success(workbenchService.list());
    }

    /**
     * 查询当前默认工作台。
     *
     * @return 当前工作台信息
     */
    @GetMapping("/api/workbenches/current")
    public ApiResponse<WorkbenchEntity> current() {
        return ApiResponse.success(workbenchService.current());
    }

    /**
     * 创建工作台。
     *
     * @param request 工作台保存请求
     * @return 创建后的工作台信息
     */
    @PostMapping("/api/workbenches")
    public ApiResponse<WorkbenchEntity> create(@Valid @RequestBody WorkbenchSaveRequest request) {
        return ApiResponse.success("创建成功", workbenchService.create(request));
    }

    /**
     * 更新指定工作台。
     *
     * @param id 工作台主键
     * @param request 工作台保存请求
     * @return 更新后的工作台信息
     */
    @PutMapping("/api/workbenches/{id}")
    public ApiResponse<WorkbenchEntity> update(@PathVariable Long id, @Valid @RequestBody WorkbenchSaveRequest request) {
        return ApiResponse.success("更新成功", workbenchService.update(id, request));
    }

    /**
     * 查询指定工作台下的组件列表。
     *
     * @param id 工作台主键
     * @return 组件集合
     */
    @GetMapping("/api/workbenches/{id}/widgets")
    public ApiResponse<List<WorkbenchWidgetEntity>> widgets(@PathVariable Long id) {
        return ApiResponse.success(workbenchService.widgets(id));
    }

    /**
     * 创建工作台组件。
     *
     * @param request 组件保存请求
     * @return 创建后的组件信息
     */
    @PostMapping("/api/workbench-widgets")
    public ApiResponse<WorkbenchWidgetEntity> createWidget(@Valid @RequestBody WidgetSaveRequest request) {
        return ApiResponse.success("创建成功", workbenchService.createWidget(request));
    }

    /**
     * 更新指定工作台组件。
     *
     * @param id 组件主键
     * @param request 组件保存请求
     * @return 更新后的组件信息
     */
    @PutMapping("/api/workbench-widgets/{id}")
    public ApiResponse<WorkbenchWidgetEntity> updateWidget(@PathVariable Long id, @Valid @RequestBody WidgetSaveRequest request) {
        return ApiResponse.success("更新成功", workbenchService.updateWidget(id, request));
    }

    /**
     * 删除指定工作台组件。
     *
     * @param id 组件主键
     * @return 空响应
     */
    @DeleteMapping("/api/workbench-widgets/{id}")
    public ApiResponse<Void> deleteWidget(@PathVariable Long id) {
        workbenchService.deleteWidget(id);
        return ApiResponse.success("删除成功", null);
    }
}
