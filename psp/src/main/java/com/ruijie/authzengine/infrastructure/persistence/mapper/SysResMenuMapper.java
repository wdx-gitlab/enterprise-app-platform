package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysResMenuEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单资源 Mapper。
 */
@Mapper
public interface SysResMenuMapper extends BaseMapper<SysResMenuEntity> {
}