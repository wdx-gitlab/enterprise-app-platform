package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthAssignmentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 授权分配 Mapper。
 */
@Mapper
public interface SysAuthAssignmentMapper extends BaseMapper<SysAuthAssignmentEntity> {
}