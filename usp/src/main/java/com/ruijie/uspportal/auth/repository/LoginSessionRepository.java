package com.ruijie.uspportal.auth.repository;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruijie.uspportal.auth.entity.LoginSessionEntity;
import com.ruijie.uspportal.auth.mapper.LoginSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class LoginSessionRepository {

    private final LoginSessionMapper loginSessionMapper;

    @Autowired
    public LoginSessionRepository(LoginSessionMapper loginSessionMapper) {
        this.loginSessionMapper = loginSessionMapper;
    }

    public void insert(LoginSessionEntity entity) {
        loginSessionMapper.insert(entity);
    }

    public LoginSessionEntity selectById(String id) {
        return loginSessionMapper.selectById(id);
    }

    public void touchSession(String sessionId, LocalDateTime activeTime) {
        LoginSessionEntity entity = new LoginSessionEntity();
        entity.setId(sessionId);
        entity.setLastActiveTime(activeTime);
        loginSessionMapper.updateById(entity);
    }

    public void logout(String sessionId, LocalDateTime logoutTime, String logoutReason) {
        LoginSessionEntity entity = new LoginSessionEntity();
        entity.setId(sessionId);
        entity.setStatus("LOGOUT");
        entity.setLogoutTime(logoutTime);
        entity.setLogoutReason(logoutReason);
        loginSessionMapper.updateById(entity);
    }

    public void invalidateByTenantCode(String tenantCode, LocalDateTime logoutTime, String logoutReason) {
        LoginSessionEntity entity = new LoginSessionEntity();
        entity.setStatus("LOGOUT");
        entity.setLogoutTime(logoutTime);
        entity.setLogoutReason(logoutReason);
        UpdateWrapper<LoginSessionEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("tenant_code", tenantCode)
                .eq("status", "ACTIVE");
        loginSessionMapper.update(entity, wrapper);
    }
}
