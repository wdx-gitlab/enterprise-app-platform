package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthSubjectRelationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 主体关系 Mapper。
 */
@Mapper
public interface AuthSubjectRelationMapper extends BaseMapper<AuthSubjectRelationEntity> {
}