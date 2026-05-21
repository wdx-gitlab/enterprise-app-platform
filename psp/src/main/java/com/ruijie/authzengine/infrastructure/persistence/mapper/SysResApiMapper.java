package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysResApiEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * API 资源目录 Mapper。
 */
@Mapper
public interface SysResApiMapper extends BaseMapper<SysResApiEntity> {
}