package com.ruijie.uspportal.navigation.controller;

import com.ruijie.uspportal.common.ApiResponse;
import com.ruijie.uspportal.navigation.dto.MenuSaveRequest;
import com.ruijie.uspportal.navigation.dto.NavigationNode;
import com.ruijie.uspportal.navigation.entity.MenuItemEntity;
import com.ruijie.uspportal.navigation.service.NavigationService;
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

/**
 * 门户导航管理控制器。
 *
 * <p>负责菜单配置的增删改查、发布以及导航树查询，供门户前后端统一消费导航结构。</p>
 */
@RestController
@Validated
public class NavigationController {

    private final NavigationService navigationService;

    @Autowired
    public NavigationController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    /**
     * 查询菜单配置列表。
     *
     * @return 菜单实体列表
     */
    @GetMapping("/api/menus")
    public ApiResponse<List<MenuItemEntity>> listMenus() {
        return ApiResponse.success(navigationService.listMenus());
    }

    /**
     * 查询指定菜单详情。
     *
     * @param id 菜单主键
     * @return 菜单详情
     */
    @GetMapping("/api/menus/{id}")
    public ApiResponse<MenuItemEntity> get(@PathVariable Long id) {
        return ApiResponse.success(navigationService.getMenu(id));
    }

    /**
     * 创建菜单配置。
     *
     * @param request 菜单保存请求
     * @return 创建后的菜单信息
     */
    @PostMapping("/api/menus")
    public ApiResponse<MenuItemEntity> create(@Valid @RequestBody MenuSaveRequest request) {
        return ApiResponse.success("创建成功", navigationService.create(request));
    }

    /**
     * 更新指定菜单配置。
     *
     * @param id 菜单主键
     * @param request 菜单保存请求
     * @return 更新后的菜单信息
     */
    @PutMapping("/api/menus/{id}")
    public ApiResponse<MenuItemEntity> update(@PathVariable Long id, @Valid @RequestBody MenuSaveRequest request) {
        return ApiResponse.success("更新成功", navigationService.update(id, request));
    }

    /**
     * 发布指定菜单配置。
     *
     * @param id 菜单主键
     * @return 空响应
     */
    @PostMapping("/api/menus/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        navigationService.publish(id);
        return ApiResponse.success("发布成功", null);
    }

    /**
     * 查询当前导航树。
     *
     * @return 面向前端消费的导航树节点列表
     */
    @GetMapping("/api/navigation/tree")
    public ApiResponse<List<NavigationNode>> tree() {
        return ApiResponse.success(navigationService.tree());
    }
}
