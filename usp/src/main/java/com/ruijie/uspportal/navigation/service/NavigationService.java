package com.ruijie.uspportal.navigation.service;

import com.ruijie.uspportal.navigation.dto.MenuSaveRequest;
import com.ruijie.uspportal.navigation.dto.NavigationNode;
import com.ruijie.uspportal.navigation.entity.MenuItemEntity;

import java.util.List;

/**
 * 门户导航服务。
 *
 * <p>定义菜单配置与导航树查询的核心能力。</p>
 */
public interface NavigationService {

    /**
     * 查询菜单配置列表。
     *
     * @return 菜单列表
     */
    List<MenuItemEntity> listMenus();

    /**
     * 查询指定菜单详情。
     *
     * @param id 菜单主键
     * @return 菜单详情
     */
    MenuItemEntity getMenu(Long id);

    /**
     * 创建菜单配置。
     *
     * @param request 菜单保存请求
     * @return 创建后的菜单信息
     */
    MenuItemEntity create(MenuSaveRequest request);

    /**
     * 更新指定菜单配置。
     *
     * @param id 菜单主键
     * @param request 菜单保存请求
     * @return 更新后的菜单信息
     */
    MenuItemEntity update(Long id, MenuSaveRequest request);

    /**
     * 发布指定菜单配置。
     *
     * @param id 菜单主键
     */
    void publish(Long id);

    /**
     * 查询当前用户可见的导航树。
     *
     * @return 导航树节点列表
     */
    List<NavigationNode> tree();
}
