package com.ruijie.uspportal.tenant.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruijie.uspportal.tenant.entity.TenantEntity;
import com.ruijie.uspportal.tenant.mapper.TenantMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRepository {

    private final TenantMapper tenantMapper;

    @Autowired
    public TenantRepository(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    public TenantEntity findByCode(String tenantCode) {
        QueryWrapper<TenantEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_code", tenantCode)
                .eq("is_deleted", 0)
                .last("LIMIT 1");
        return tenantMapper.selectOne(wrapper);
    }
}
