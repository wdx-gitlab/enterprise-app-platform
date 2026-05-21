package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户目录 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {
}