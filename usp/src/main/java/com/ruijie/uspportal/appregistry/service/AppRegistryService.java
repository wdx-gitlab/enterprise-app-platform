package com.ruijie.uspportal.appregistry.service;

import com.ruijie.uspportal.appregistry.dto.AppSaveRequest;
import com.ruijie.uspportal.appregistry.entity.AppRegistryEntity;

import java.util.List;

/**
 * 应用注册服务。
 *
 * <p>定义应用目录的查询、创建、更新、发布与下线等领域操作。</p>
 */
public interface AppRegistryService {

    /**
     * 查询全部应用注册记录。
     *
     * @return 应用列表
     */
    List<AppRegistryEntity> list();

    /**
     * 查询当前可见的应用目录。
     *
     * @return 已发布应用列表
     */
    List<AppRegistryEntity> catalog();

    /**
     * 查询指定应用详情。
     *
     * @param id 应用主键
     * @return 应用详情
     */
    AppRegistryEntity get(Long id);

    /**
     * 创建应用注册记录。
     *
     * @param request 应用保存请求
     * @return 创建后的应用信息
     */
    AppRegistryEntity create(AppSaveRequest request);

    /**
     * 更新指定应用注册记录。
     *
     * @param id 应用主键
     * @param request 应用保存请求
     * @return 更新后的应用信息
     */
    AppRegistryEntity update(Long id, AppSaveRequest request);

    /**
     * 发布指定应用。
     *
     * @param id 应用主键
     */
    void publish(Long id);

    /**
     * 下线指定应用。
     *
     * @param id 应用主键
     */
    void offline(Long id);
}
