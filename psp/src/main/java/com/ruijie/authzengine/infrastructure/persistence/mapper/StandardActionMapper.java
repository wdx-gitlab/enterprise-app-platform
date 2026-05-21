package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.StandardActionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标准动作 Mapper。
 */
@Mapper
public interface StandardActionMapper extends BaseMapper<StandardActionEntity> {
}