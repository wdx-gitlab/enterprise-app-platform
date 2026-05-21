package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysUserGroupEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户组目录 Mapper。
 */
@Mapper
public interface SysUserGroupMapper extends BaseMapper<SysUserGroupEntity> {
}