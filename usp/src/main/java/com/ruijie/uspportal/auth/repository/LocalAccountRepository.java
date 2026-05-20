package com.ruijie.uspportal.auth.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruijie.uspportal.auth.entity.LocalAccountEntity;
import com.ruijie.uspportal.auth.mapper.LocalAccountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class LocalAccountRepository {

    private final LocalAccountMapper localAccountMapper;

    @Autowired
    public LocalAccountRepository(LocalAccountMapper localAccountMapper) {
        this.localAccountMapper = localAccountMapper;
    }

    public LocalAccountEntity findEnabledAccount(String loginName, String tenantCode) {
        QueryWrapper<LocalAccountEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name", loginName)
                .eq("tenant_code", tenantCode)
                .eq("is_deleted", 0)
                .eq("status", "ENABLED")
                .last("LIMIT 1");
        return localAccountMapper.selectOne(wrapper);
    }

    public LocalAccountEntity findEnabledAccountById(Long accountId, String tenantCode) {
        QueryWrapper<LocalAccountEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("id", accountId)
                .eq("tenant_code", tenantCode)
                .eq("is_deleted", 0)
                .eq("status", "ENABLED")
                .last("LIMIT 1");
        return localAccountMapper.selectOne(wrapper);
    }

    public void touchLastLogin(Long accountId) {
        LocalAccountEntity entity = new LocalAccountEntity();
        entity.setId(accountId);
        entity.setLastLoginTime(LocalDateTime.now());
        localAccountMapper.updateById(entity);
    }
}
