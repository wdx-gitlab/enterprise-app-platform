package com.ruijie.dapengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.dapengine.entity.MetadataConfigEntity;

/**
 * dap_sys_metadata_config 表 MyBatis-Plus Mapper 接口。
 *
 * <p>继承 {@link BaseMapper} 获得标准 CRUD 操作。
 * 所有复杂查询通过 {@link com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper}
 * 或 {@link com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper} 在
 * {@code MetadataRepository} 中组装，不在此接口中定义额外方法。</p>
 *
 * <p>Bean 注册方式：由 {@code DapEngineAutoConfiguration} 通过 {@code MapperFactoryBean}
 * 手动注册，绑定到专属的 {@code dapSqlSessionFactory}。</p>
 */
public interface MetadataConfigMapper extends BaseMapper<MetadataConfigEntity> {
}
