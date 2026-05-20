package com.ruijie.uspportal.workbench.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruijie.uspportal.common.BusinessException;
import com.ruijie.uspportal.security.CurrentUserContext;
import com.ruijie.uspportal.workbench.dto.WidgetSaveRequest;
import com.ruijie.uspportal.workbench.dto.WorkbenchSaveRequest;
import com.ruijie.uspportal.workbench.entity.WorkbenchEntity;
import com.ruijie.uspportal.workbench.entity.WorkbenchWidgetEntity;
import com.ruijie.uspportal.workbench.mapper.WorkbenchMapper;
import com.ruijie.uspportal.workbench.mapper.WorkbenchWidgetMapper;
import com.ruijie.uspportal.workbench.service.WorkbenchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工作台服务默认实现。
 *
 * <p>负责当前用户工作台与组件的查询、创建、更新、删除以及默认工作台切换。</p>
 */
@Service
public class WorkbenchServiceImpl implements WorkbenchService {

    private final WorkbenchMapper workbenchMapper;

    private final WorkbenchWidgetMapper workbenchWidgetMapper;

    @Autowired
    public WorkbenchServiceImpl(WorkbenchMapper workbenchMapper,
                                WorkbenchWidgetMapper workbenchWidgetMapper) {
        this.workbenchMapper = workbenchMapper;
        this.workbenchWidgetMapper = workbenchWidgetMapper;
    }

    /**
     * 查询当前用户的工作台列表。
     *
     * @return 工作台集合
     */
    @Override
    public List<WorkbenchEntity> list() {
        QueryWrapper<WorkbenchEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0).eq("owner_user_id", CurrentUserContext.getUserId()).orderByDesc("updated_time");
        return workbenchMapper.selectList(wrapper);
    }

    /**
     * 查询当前默认工作台。
     *
     * @return 当前工作台信息
     */
    @Override
    public WorkbenchEntity current() {
        QueryWrapper<WorkbenchEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
                .eq("owner_user_id", CurrentUserContext.getUserId())
                .eq("is_default", 1)
                .last("LIMIT 1");
        WorkbenchEntity entity = workbenchMapper.selectOne(wrapper);
        if (entity != null) {
            return entity;
        }
        List<WorkbenchEntity> all = list();
        if (all.isEmpty()) {
            throw new BusinessException("当前用户还没有工作台");
        }
        return all.get(0);
    }

    /**
     * 创建工作台。
     *
     * @param request 工作台保存请求
     * @return 创建后的工作台信息
     */
    @Override
    public WorkbenchEntity create(WorkbenchSaveRequest request) {
        if (Boolean.TRUE.equals(request.getDefaultWorkbench())) {
            clearDefault();
        }
        WorkbenchEntity entity = new WorkbenchEntity();
        apply(entity, request);
        entity.setStatus("ENABLED");
        entity.setOwnerUserId(CurrentUserContext.getUserId());
        entity.setTenantCode(CurrentUserContext.get() == null ? null : CurrentUserContext.get().getTenantCode());
        workbenchMapper.insert(entity);
        return entity;
    }

    /**
     * 更新指定工作台。
     *
     * @param id 工作台主键
     * @param request 工作台保存请求
     * @return 更新后的工作台信息
     */
    @Override
    public WorkbenchEntity update(Long id, WorkbenchSaveRequest request) {
        WorkbenchEntity entity = get(id);
        if (Boolean.TRUE.equals(request.getDefaultWorkbench())) {
            clearDefault();
        }
        apply(entity, request);
        workbenchMapper.updateById(entity);
        return get(id);
    }

    /**
     * 查询指定工作台的组件列表。
     *
     * @param workbenchId 工作台主键
     * @return 组件集合
     */
    @Override
    public List<WorkbenchWidgetEntity> widgets(Long workbenchId) {
        QueryWrapper<WorkbenchWidgetEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0).eq("workbench_id", workbenchId).orderByAsc("row_no", "col_no");
        return workbenchWidgetMapper.selectList(wrapper);
    }

    /**
     * 创建工作台组件。
     *
     * @param request 组件保存请求
     * @return 创建后的组件信息
     */
    @Override
    public WorkbenchWidgetEntity createWidget(WidgetSaveRequest request) {
        get(request.getWorkbenchId());
        WorkbenchWidgetEntity entity = new WorkbenchWidgetEntity();
        apply(entity, request);
        entity.setStatus("ENABLED");
        workbenchWidgetMapper.insert(entity);
        return entity;
    }

    /**
     * 更新指定工作台组件。
     *
     * @param id 组件主键
     * @param request 组件保存请求
     * @return 更新后的组件信息
     */
    @Override
    public WorkbenchWidgetEntity updateWidget(Long id, WidgetSaveRequest request) {
        WorkbenchWidgetEntity entity = getWidget(id);
        apply(entity, request);
        workbenchWidgetMapper.updateById(entity);
        return getWidget(id);
    }

    /**
     * 删除指定工作台组件。
     *
     * @param id 组件主键
     */
    @Override
    public void deleteWidget(Long id) {
        WorkbenchWidgetEntity entity = getWidget(id);
        entity.setDeleted(1);
        workbenchWidgetMapper.updateById(entity);
    }

    /**
     * 查询并校验工作台是否存在。
     *
     * @param id 工作台主键
     * @return 工作台实体
     */
    private WorkbenchEntity get(Long id) {
        WorkbenchEntity entity = workbenchMapper.selectById(id);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BusinessException("工作台不存在");
        }
        return entity;
    }

    /**
     * 查询并校验工作台组件是否存在。
     *
     * @param id 组件主键
     * @return 工作台组件实体
     */
    private WorkbenchWidgetEntity getWidget(Long id) {
        WorkbenchWidgetEntity entity = workbenchWidgetMapper.selectById(id);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BusinessException("组件不存在");
        }
        return entity;
    }

    /**
     * 将工作台保存请求写入工作台实体。
     *
     * @param entity 工作台实体
     * @param request 工作台保存请求
     */
    private void apply(WorkbenchEntity entity, WorkbenchSaveRequest request) {
        entity.setWorkbenchCode(request.getWorkbenchCode());
        entity.setWorkbenchName(request.getWorkbenchName());
        entity.setWorkbenchType(request.getWorkbenchType());
        entity.setLayoutTemplate(request.getLayoutTemplate());
        entity.setDefaultWorkbench(Boolean.TRUE.equals(request.getDefaultWorkbench()));
    }

    /**
     * 将组件保存请求写入组件实体。
     *
     * @param entity 工作台组件实体
     * @param request 组件保存请求
     */
    private void apply(WorkbenchWidgetEntity entity, WidgetSaveRequest request) {
        entity.setWorkbenchId(request.getWorkbenchId());
        entity.setWidgetCode(request.getWidgetCode());
        entity.setWidgetName(request.getWidgetName());
        entity.setWidgetType(request.getWidgetType());
        entity.setSourceAppId(request.getSourceAppId());
        entity.setPropsJson(StringUtils.hasText(request.getPropsJson()) ? request.getPropsJson().trim() : null);
        entity.setRowNo(request.getRowNo() == null ? 1 : request.getRowNo());
        entity.setColNo(request.getColNo() == null ? 1 : request.getColNo());
        entity.setWidth(request.getWidth() == null ? 1 : request.getWidth());
        entity.setHeight(request.getHeight() == null ? 1 : request.getHeight());
        entity.setPermissionCode(request.getPermissionCode());
    }

    /**
     * 清空当前用户的默认工作台标记。
     */
    private void clearDefault() {
        UpdateWrapper<WorkbenchEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("owner_user_id", CurrentUserContext.getUserId()).set("is_default", 0);
        workbenchMapper.update(null, wrapper);
    }
}
