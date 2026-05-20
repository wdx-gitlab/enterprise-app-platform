package com.ruijie.uspportal.workbench.service;

import com.ruijie.uspportal.workbench.dto.WidgetSaveRequest;
import com.ruijie.uspportal.workbench.dto.WorkbenchSaveRequest;
import com.ruijie.uspportal.workbench.entity.WorkbenchEntity;
import com.ruijie.uspportal.workbench.entity.WorkbenchWidgetEntity;

import java.util.List;

/**
 * 工作台服务。
 *
 * <p>定义工作台与工作台组件的查询、维护及删除能力。</p>
 */
public interface WorkbenchService {

    /**
     * 查询当前用户的工作台列表。
     *
     * @return 工作台集合
     */
    List<WorkbenchEntity> list();

    /**
     * 查询当前默认工作台。
     *
     * @return 当前工作台信息
     */
    WorkbenchEntity current();

    /**
     * 创建工作台。
     *
     * @param request 工作台保存请求
     * @return 创建后的工作台信息
     */
    WorkbenchEntity create(WorkbenchSaveRequest request);

    /**
     * 更新指定工作台。
     *
     * @param id 工作台主键
     * @param request 工作台保存请求
     * @return 更新后的工作台信息
     */
    WorkbenchEntity update(Long id, WorkbenchSaveRequest request);

    /**
     * 查询指定工作台的组件列表。
     *
     * @param workbenchId 工作台主键
     * @return 组件集合
     */
    List<WorkbenchWidgetEntity> widgets(Long workbenchId);

    /**
     * 创建工作台组件。
     *
     * @param request 组件保存请求
     * @return 创建后的组件信息
     */
    WorkbenchWidgetEntity createWidget(WidgetSaveRequest request);

    /**
     * 更新指定工作台组件。
     *
     * @param id 组件主键
     * @param request 组件保存请求
     * @return 更新后的组件信息
     */
    WorkbenchWidgetEntity updateWidget(Long id, WidgetSaveRequest request);

    /**
     * 删除指定工作台组件。
     *
     * @param id 组件主键
     */
    void deleteWidget(Long id);
}
