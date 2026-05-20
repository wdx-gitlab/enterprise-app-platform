package com.ruijie.uspportal.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruijie.uspportal.auth.dto.LocalAccountResponse;
import com.ruijie.uspportal.auth.dto.LocalAccountSaveRequest;
import com.ruijie.uspportal.auth.entity.LocalAccountEntity;
import com.ruijie.uspportal.auth.mapper.LocalAccountMapper;
import com.ruijie.uspportal.auth.service.LocalAccountService;
import com.ruijie.uspportal.auth.util.LocalAccountPasswordCodec;
import com.ruijie.uspportal.common.BusinessException;
import com.ruijie.uspportal.security.CurrentUserContext;
import com.ruijie.uspportal.tenant.entity.TenantEntity;
import com.ruijie.uspportal.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 本地账号服务实现类。
 *
 * <p>负责本地账号的持久化维护、租户归属校验、密码处理以及状态切换。</p>
 */
@Service
public class LocalAccountServiceImpl implements LocalAccountService {

    private static final List<String> SUPPORTED_STATUSES = Arrays.asList("ENABLED", "DISABLED", "LOCKED");

    private final LocalAccountMapper localAccountMapper;

    private final TenantRepository tenantRepository;

    @Autowired
    public LocalAccountServiceImpl(LocalAccountMapper localAccountMapper,
                                   TenantRepository tenantRepository) {
        this.localAccountMapper = localAccountMapper;
        this.tenantRepository = tenantRepository;
    }

    @Value("${usp.portal.default-tenant-code}")
    private String defaultTenantCode;

    /**
     * 查询本地账号列表。
     *
     * @return 本地账号集合
     */
    @Override
    public List<LocalAccountResponse> list() {
        QueryWrapper<LocalAccountEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
                .orderByDesc("is_admin")
                .orderByDesc("updated_time");
        return localAccountMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询指定本地账号详情。
     *
     * @param id 账号主键
     * @return 本地账号详情
     */
    @Override
    public LocalAccountResponse get(Long id) {
        return toResponse(requireAccount(id));
    }

    /**
     * 创建本地账号。
     *
     * @param request 账号保存请求
     * @return 创建后的账号信息
     */
    @Override
    public LocalAccountResponse create(LocalAccountSaveRequest request) {
        String loginName = normalizeRequired(request.getLoginName(), "登录账号不能为空");
        ensureUniqueLoginName(loginName, null);

        LocalAccountEntity entity = new LocalAccountEntity();
        apply(entity, request, true);
        String operator = resolveOperator();
        entity.setCreatedBy(operator);
        entity.setUpdatedBy(operator);
        entity.setDeleted(0);
        localAccountMapper.insert(entity);
        return get(entity.getId());
    }

    /**
     * 更新指定本地账号。
     *
     * @param id 账号主键
     * @param request 账号保存请求
     * @return 更新后的账号信息
     */
    @Override
    public LocalAccountResponse update(Long id, LocalAccountSaveRequest request) {
        LocalAccountEntity entity = requireAccount(id);
        String loginName = normalizeRequired(request.getLoginName(), "登录账号不能为空");
        ensureUniqueLoginName(loginName, id);

        apply(entity, request, false);
        entity.setUpdatedBy(resolveOperator());
        localAccountMapper.updateById(entity);
        return get(id);
    }

    /**
     * 启用指定本地账号。
     *
     * @param id 账号主键
     */
    @Override
    public void enable(Long id) {
        changeStatus(id, "ENABLED");
    }

    /**
     * 停用指定本地账号。
     *
     * @param id 账号主键
     */
    @Override
    public void disable(Long id) {
        changeStatus(id, "DISABLED");
    }

    /**
     * 切换本地账号状态。
     *
     * @param id 账号主键
     * @param status 目标状态
     */
    private void changeStatus(Long id, String status) {
        LocalAccountEntity entity = requireAccount(id);
        entity.setStatus(status);
        entity.setUpdatedBy(resolveOperator());
        localAccountMapper.updateById(entity);
    }

    /**
     * 将账号保存请求写入账号实体。
     *
     * @param entity 账号实体
     * @param request 账号保存请求
     * @param creating 是否为创建场景
     */
    private void apply(LocalAccountEntity entity, LocalAccountSaveRequest request, boolean creating) {
        entity.setLoginName(normalizeRequired(request.getLoginName(), "登录账号不能为空"));
        entity.setDisplayName(normalizeRequired(request.getDisplayName(), "显示名称不能为空"));
        entity.setEmail(normalizeNullable(request.getEmail()));
        entity.setPhoneNum(normalizeNullable(request.getPhoneNum()));
        entity.setTenantCode(resolveTenantCode(request.getTenantCode()));
        entity.setStatus(resolveStatus(request.getStatus()));
        entity.setAdmin(Boolean.TRUE.equals(request.getAdmin()));
        entity.setForceResetPassword(Boolean.TRUE.equals(request.getForceResetPassword()));

        if (StringUtils.hasText(request.getPassword())) {
            entity.setPasswordEncodeType(LocalAccountPasswordCodec.ENCODE_TYPE_SHA256_UPPER);
            entity.setPasswordSalt(null);
            entity.setPasswordHash(LocalAccountPasswordCodec.encode(request.getPassword(), LocalAccountPasswordCodec.ENCODE_TYPE_SHA256_UPPER));
        } else if (creating) {
            throw new BusinessException("创建本地账号时必须填写密码");
        }
    }

    /**
     * 查询并校验本地账号是否存在。
     *
     * @param id 账号主键
     * @return 本地账号实体
     */
    private LocalAccountEntity requireAccount(Long id) {
        LocalAccountEntity entity = localAccountMapper.selectById(id);
        if (entity == null || Integer.valueOf(1).equals(entity.getDeleted())) {
            throw new BusinessException(404, "本地账号不存在");
        }
        return entity;
    }

    /**
     * 校验登录账号在当前范围内唯一。
     *
     * @param loginName 登录账号
     * @param excludeId 更新场景下需要排除的账号主键
     */
    private void ensureUniqueLoginName(String loginName, Long excludeId) {
        QueryWrapper<LocalAccountEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name", loginName)
                .eq("is_deleted", 0)
                .last("LIMIT 1");
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (localAccountMapper.selectOne(wrapper) != null) {
            throw new BusinessException("登录账号已存在");
        }
    }

    /**
     * 解析账号所属租户编码。
     *
     * @param tenantCode 请求中的租户编码
     * @return 实际使用的租户编码
     */
    private String resolveTenantCode(String tenantCode) {
        String resolved = StringUtils.hasText(tenantCode) ? tenantCode.trim() : defaultTenantCode;
        if (defaultTenantCode.equals(resolved)) {
            return resolved;
        }
        TenantEntity tenant = tenantRepository.findByCode(resolved);
        if (tenant == null) {
            throw new BusinessException(404, "所属租户不存在");
        }
        return resolved;
    }

    /**
     * 解析并校验账号状态。
     *
     * @param status 请求中的账号状态
     * @return 规范化后的账号状态
     */
    private String resolveStatus(String status) {
        String resolved = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "ENABLED";
        if (!SUPPORTED_STATUSES.contains(resolved)) {
            throw new BusinessException("不支持的账号状态");
        }
        return resolved;
    }

    /**
     * 规范化必填字符串字段。
     *
     * @param value 原始值
     * @param message 为空时的异常消息
     * @return 去除首尾空白后的值
     */
    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    /**
     * 规范化可空字符串字段。
     *
     * @param value 原始值
     * @return 去除首尾空白后的值，若为空则返回 {@code null}
     */
    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 解析当前操作人。
     *
     * @return 当前登录名，若不存在则返回 SYSTEM
     */
    private String resolveOperator() {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.get();
        return currentUser == null || !StringUtils.hasText(currentUser.getLoginName()) ? "SYSTEM" : currentUser.getLoginName();
    }

    /**
     * 将账号实体转换为响应对象。
     *
     * @param entity 账号实体
     * @return 账号响应对象
     */
    private LocalAccountResponse toResponse(LocalAccountEntity entity) {
        return LocalAccountResponse.builder()
                .id(entity.getId())
                .loginName(entity.getLoginName())
                .displayName(entity.getDisplayName())
                .email(entity.getEmail())
                .phoneNum(entity.getPhoneNum())
                .tenantCode(entity.getTenantCode())
                .status(entity.getStatus())
                .admin(Boolean.TRUE.equals(entity.getAdmin()))
                .forceResetPassword(Boolean.TRUE.equals(entity.getForceResetPassword()))
                .passwordEncodeType(entity.getPasswordEncodeType())
                .createdBy(entity.getCreatedBy())
                .createdTime(entity.getCreatedTime())
                .updatedBy(entity.getUpdatedBy())
                .updatedTime(entity.getUpdatedTime())
                .lastLoginTime(entity.getLastLoginTime())
                .build();
    }
}
