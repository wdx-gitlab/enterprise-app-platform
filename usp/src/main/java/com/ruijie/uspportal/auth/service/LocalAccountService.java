package com.ruijie.uspportal.auth.service;

import com.ruijie.uspportal.auth.dto.LocalAccountResponse;
import com.ruijie.uspportal.auth.dto.LocalAccountSaveRequest;

import java.util.List;

/**
 * 本地账号服务。
 *
 * <p>定义本地账号的查询、创建、更新与启停用能力。</p>
 */
public interface LocalAccountService {

    /**
     * 查询本地账号列表。
     *
     * @return 本地账号集合
     */
    List<LocalAccountResponse> list();

    /**
     * 查询指定本地账号详情。
     *
     * @param id 账号主键
     * @return 本地账号详情
     */
    LocalAccountResponse get(Long id);

    /**
     * 创建本地账号。
     *
     * @param request 账号保存请求
     * @return 创建后的账号信息
     */
    LocalAccountResponse create(LocalAccountSaveRequest request);

    /**
     * 更新指定本地账号。
     *
     * @param id 账号主键
     * @param request 账号保存请求
     * @return 更新后的账号信息
     */
    LocalAccountResponse update(Long id, LocalAccountSaveRequest request);

    /**
     * 启用指定本地账号。
     *
     * @param id 账号主键
     */
    void enable(Long id);

    /**
     * 停用指定本地账号。
     *
     * @param id 账号主键
     */
    void disable(Long id);
}
