package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.BoMetaModelEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 业务对象元模型 Mapper。
 */
@Mapper
public interface BoMetaModelMapper extends BaseMapper<BoMetaModelEntity> {

    /**
     * 查询同一租户+应用+BO编码下已软删除的记录（用于恢复）。
     */
    @Select("SELECT * FROM authz_bo_meta_model WHERE tenant_id = #{tenantId} AND app_code = #{appCode} AND bo_code = #{boCode} AND is_deleted = 1 LIMIT 1")
    BoMetaModelEntity findDeletedByCode(
        @Param("tenantId") String tenantId,
        @Param("appCode") String appCode,
        @Param("boCode") String boCode
    );

    /**
     * 恢复逻辑删除的 BO 元模型并覆盖当前业务字段。
     */
    @Update("UPDATE authz_bo_meta_model SET tenant_id = #{entity.tenantId}, app_code = #{entity.appCode}, bo_code = #{entity.boCode}, bo_name = #{entity.boName}, schema_json = #{entity.schemaJson}, adapter_type = #{entity.adapterType}, resolver = #{entity.resolver}, updated_at = CURRENT_TIMESTAMP, is_deleted = 0 WHERE id = #{entity.id}")
    int reviveById(@Param("entity") BoMetaModelEntity entity);
}