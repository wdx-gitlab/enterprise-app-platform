package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysPositionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 岗位目录 Mapper。
 */
@Mapper
public interface SysPositionMapper extends BaseMapper<SysPositionEntity> {
}