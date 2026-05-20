package com.ruijie.uspportal.portalconfig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.uspportal.portalconfig.entity.FeatureFlagEntity;
import com.ruijie.uspportal.portalconfig.entity.FeatureFlagRuleEntity;
import com.ruijie.uspportal.portalconfig.entity.PortalParamEntity;
import com.ruijie.uspportal.portalconfig.entity.PortalParamHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortalConfigMapper extends BaseMapper<PortalParamEntity> {
}
