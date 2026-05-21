package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthRoleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色目录 Mapper。
 */
@Mapper
public interface AuthRoleMapper extends BaseMapper<AuthRoleEntity> {
}