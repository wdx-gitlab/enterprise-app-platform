package com.ruijie.uspportal.appregistry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruijie.uspportal.appregistry.dto.AppSaveRequest;
import com.ruijie.uspportal.appregistry.entity.AppRegistryEntity;
import com.ruijie.uspportal.appregistry.mapper.AppRegistryMapper;
import com.ruijie.uspportal.appregistry.service.AppRegistryService;
import com.ruijie.uspportal.common.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用注册服务默认实现。
 *
 * <p>负责应用注册数据的持久化维护，以及应用发布与下线状态的流转。</p>
 */
@Service
public class AppRegistryServiceImpl implements AppRegistryService {

    private final AppRegistryMapper appRegistryMapper;

    @Autowired
    public AppRegistryServiceImpl(AppRegistryMapper appRegistryMapper) {
        this.appRegistryMapper = appRegistryMapper;
    }

    /**
     * 查询全部应用注册记录。
     *
     * @return 应用列表
     */
    @Override
    public List<AppRegistryEntity> list() {
        QueryWrapper<AppRegistryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0).orderByDesc("updated_time");
        return appRegistryMapper.selectList(wrapper);
    }

    /**
     * 查询已发布的应用目录。
     *
     * @return 已发布应用列表
     */
    @Override
    public List<AppRegistryEntity> catalog() {
        QueryWrapper<AppRegistryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0).eq("publish_status", "PUBLISHED").orderByAsc("app_name");
        return appRegistryMapper.selectList(wrapper);
    }

    /**
     * 查询指定应用详情。
     *
     * @param id 应用主键
     * @return 应用详情
     */
    @Override
    public AppRegistryEntity get(Long id) {
        AppRegistryEntity entity = appRegistryMapper.selectById(id);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BusinessException("应用不存在");
        }
        return entity;
    }

    /**
     * 创建应用注册记录。
     *
     * @param request 应用保存请求
     * @return 创建后的应用信息
     */
    @Override
    public AppRegistryEntity create(AppSaveRequest request) {
        AppRegistryEntity entity = new AppRegistryEntity();
        apply(entity, request);
        entity.setPublishStatus("DRAFT");
        appRegistryMapper.insert(entity);
        return entity;
    }

    /**
     * 更新指定应用注册记录。
     *
     * @param id 应用主键
     * @param request 应用保存请求
     * @return 更新后的应用信息
     */
    @Override
    public AppRegistryEntity update(Long id, AppSaveRequest request) {
        AppRegistryEntity entity = get(id);
        apply(entity, request);
        appRegistryMapper.updateById(entity);
        return get(id);
    }

    /**
     * 发布指定应用。
     *
     * @param id 应用主键
     */
    @Override
    public void publish(Long id) {
        AppRegistryEntity entity = get(id);
        entity.setPublishStatus("PUBLISHED");
        entity.setPublishedTime(LocalDateTime.now());
        appRegistryMapper.updateById(entity);
    }

    /**
     * 下线指定应用。
     *
     * @param id 应用主键
     */
    @Override
    public void offline(Long id) {
        AppRegistryEntity entity = get(id);
        entity.setPublishStatus("OFFLINE");
        appRegistryMapper.updateById(entity);
    }

    /**
     * 将请求参数写入应用实体。
     *
     * @param entity 应用实体
     * @param request 应用保存请求
     */
    private void apply(AppRegistryEntity entity, AppSaveRequest request) {
        entity.setAppCode(request.getAppCode());
        entity.setAppName(request.getAppName());
        entity.setEntryUrl(request.getEntryUrl());
        entity.setAppType(request.getAppType());
        entity.setRoutePrefix(request.getRoutePrefix());
        entity.setAppIcon(request.getAppIcon());
        entity.setAppDesc(request.getAppDesc());
    }
}
