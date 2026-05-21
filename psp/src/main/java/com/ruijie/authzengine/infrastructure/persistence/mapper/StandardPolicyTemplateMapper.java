package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.StandardPolicyTemplateEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标准策略模板 Mapper。
 */
@Mapper
public interface StandardPolicyTemplateMapper extends BaseMapper<StandardPolicyTemplateEntity> {
}