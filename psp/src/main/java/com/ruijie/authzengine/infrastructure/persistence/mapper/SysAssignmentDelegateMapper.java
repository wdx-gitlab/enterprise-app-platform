package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAssignmentDelegateEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 委托授权 Mapper。
 */
@Mapper
public interface SysAssignmentDelegateMapper extends BaseMapper<SysAssignmentDelegateEntity> {
}