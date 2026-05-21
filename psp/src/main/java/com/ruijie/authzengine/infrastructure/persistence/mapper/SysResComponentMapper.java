package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysResComponentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 组件资源 Mapper。
 */
@Mapper
public interface SysResComponentMapper extends BaseMapper<SysResComponentEntity> {
}