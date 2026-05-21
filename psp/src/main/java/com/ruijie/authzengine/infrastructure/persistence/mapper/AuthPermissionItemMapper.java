package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthPermissionItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 权限项 Mapper。
 */
@Mapper
public interface AuthPermissionItemMapper extends BaseMapper<AuthPermissionItemEntity> {

	/**
	 * 查询按 permCode 逻辑删除的权限项，供恢复同一业务编码时复活墓碑记录。
	 */
	@Select("SELECT * FROM authz_permission_item WHERE tenant_id = #{tenantId} AND app_code = #{appCode} AND perm_code = #{permCode} AND is_deleted = 1 LIMIT 1")
	AuthPermissionItemEntity findDeletedByPermCode(
		@Param("tenantId") String tenantId,
		@Param("appCode") String appCode,
		@Param("permCode") String permCode
	);

	/**
	 * 查询按数据库唯一键逻辑删除的权限项，避免重建同一资源动作时撞唯一约束。
	 */
	@Select("SELECT * FROM authz_permission_item WHERE tenant_id = #{tenantId} AND app_code = #{appCode} AND res_model_code = #{resModelCode} AND res_id = #{resId} AND act_code = #{actCode} AND is_deleted = 1 LIMIT 1")
	AuthPermissionItemEntity findDeletedByUniqueKey(
		@Param("tenantId") String tenantId,
		@Param("appCode") String appCode,
		@Param("resModelCode") String resModelCode,
		@Param("resId") String resId,
		@Param("actCode") String actCode
	);

	/**
	 * 恢复逻辑删除的权限项并覆盖当前业务字段。
	 */
	@Update("UPDATE authz_permission_item SET tenant_id = #{entity.tenantId}, app_code = #{entity.appCode}, perm_code = #{entity.permCode}, res_model_code = #{entity.resModelCode}, res_id = #{entity.resId}, act_code = #{entity.actCode}, fail_strategy = #{entity.failStrategy}, updated_by = #{entity.updatedBy}, updated_at = CURRENT_TIMESTAMP, is_deleted = 0 WHERE id = #{entity.id}")
	int reviveById(@Param("entity") AuthPermissionItemEntity entity);
}