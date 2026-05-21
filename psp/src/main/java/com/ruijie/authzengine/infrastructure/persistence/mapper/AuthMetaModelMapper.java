package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthMetaModelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限元模型 Mapper。
 */
@Mapper
public interface AuthMetaModelMapper extends BaseMapper<AuthMetaModelEntity> {
}