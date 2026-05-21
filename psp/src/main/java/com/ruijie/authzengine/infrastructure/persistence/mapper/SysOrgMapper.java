package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysOrgEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 组织目录 Mapper。
 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrgEntity> {
}