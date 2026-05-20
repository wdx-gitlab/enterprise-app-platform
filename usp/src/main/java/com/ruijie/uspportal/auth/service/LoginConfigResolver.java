package com.ruijie.uspportal.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruijie.uspportal.auth.config.PortalLoginOverrideProperties;
import com.ruijie.uspportal.auth.entity.LoginConfigEntity;
import com.ruijie.uspportal.auth.mapper.LoginConfigMapper;
import com.ruijie.uspportal.common.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 登录配置解析器。
 *
 * <p>负责读取当前已启用的登录配置，并在需要时叠加宿主侧覆盖参数。</p>
 */
@Component
public class LoginConfigResolver {

    private final LoginConfigMapper loginConfigMapper;

    private final PortalLoginOverrideProperties overrideProperties;

    @Autowired
    public LoginConfigResolver(LoginConfigMapper loginConfigMapper,
                               PortalLoginOverrideProperties overrideProperties) {
        this.loginConfigMapper = loginConfigMapper;
        this.overrideProperties = overrideProperties;
    }

    /**
     * 查询当前生效的登录配置。
     *
     * @return 已叠加覆盖项的登录配置
     */
    public LoginConfigEntity getEnabledLoginConfig() {
        QueryWrapper<LoginConfigEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ENABLED")
            .orderByAsc("id")
            .last("LIMIT 1");

        LoginConfigEntity config = loginConfigMapper.selectOne(wrapper);
        if (config == null) {
            throw new BusinessException(500, "未找到登录配置");
        }

        if (!overrideProperties.isEnabled()) {
            return config;
        }

        LoginConfigEntity resolved = new LoginConfigEntity();
        resolved.setId(config.getId());
        resolved.setStatus(config.getStatus());
        resolved.setUpdatedTime(config.getUpdatedTime());
        resolved.setPasswordPolicyJson(config.getPasswordPolicyJson());
        resolved.setAccountMappingRule(config.getAccountMappingRule());
        resolved.setInternalLoginEnabled(overrideProperties.getInternalLoginEnabled() != null
            ? overrideProperties.getInternalLoginEnabled()
            : config.getInternalLoginEnabled());
        resolved.setSsoLoginEnabled(overrideProperties.getSsoLoginEnabled() != null
            ? overrideProperties.getSsoLoginEnabled()
            : config.getSsoLoginEnabled());
        resolved.setDefaultLoginMode(StringUtils.hasText(overrideProperties.getDefaultLoginMode())
            ? overrideProperties.getDefaultLoginMode().trim()
            : config.getDefaultLoginMode());
        resolved.setSsoButtonText(StringUtils.hasText(overrideProperties.getSsoButtonText())
            ? overrideProperties.getSsoButtonText().trim()
            : config.getSsoButtonText());
        return resolved;
    }
}
