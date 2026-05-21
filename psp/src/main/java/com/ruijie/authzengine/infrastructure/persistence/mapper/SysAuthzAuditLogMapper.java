package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthzAuditLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 鉴权审计日志 Mapper。
 */
@Mapper
public interface SysAuthzAuditLogMapper extends BaseMapper<SysAuthzAuditLogEntity> {
}