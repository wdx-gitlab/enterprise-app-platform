package com.ruijie.authzengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthResDerivationPermEntity;
import java.util.Collection;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 派生权限关联 Mapper。
 */
@Mapper
public interface AuthResDerivationPermMapper extends BaseMapper<AuthResDerivationPermEntity> {

	/**
	 * 查询逻辑删除的派生绑定，供同一业务键重建时直接恢复。
	 */
	@Select("SELECT * FROM authz_res_derivation_perm WHERE tenant_id = #{tenantId} AND app_code = #{appCode} AND res_type = #{resType} AND res_id = #{resId} AND perm_item_id = #{permItemId} AND is_deleted = 1 LIMIT 1")
	AuthResDerivationPermEntity findDeletedBinding(
		@Param("tenantId") String tenantId,
		@Param("appCode") String appCode,
		@Param("resType") String resType,
		@Param("resId") Long resId,
		@Param("permItemId") Long permItemId
	);

	/**
	 * 恢复逻辑删除的派生绑定并刷新排序信息。
	 */
	@Update("UPDATE authz_res_derivation_perm SET tenant_id = #{entity.tenantId}, app_code = #{entity.appCode}, res_type = #{entity.resType}, res_id = #{entity.resId}, perm_item_id = #{entity.permItemId}, sort_order = #{entity.sortOrder}, updated_by = #{entity.updatedBy}, updated_at = CURRENT_TIMESTAMP, is_deleted = 0 WHERE id = #{entity.id}")
	int reviveById(@Param("entity") AuthResDerivationPermEntity entity);

	/**
	 * 按主键执行物理删除，绕过 BaseEntity 的逻辑删除语义。
	 */
	@Delete("DELETE FROM authz_res_derivation_perm WHERE tenant_id = #{tenantId} AND app_code = #{appCode} AND id = #{bindingId}")
	int deletePhysicalById(
		@Param("tenantId") String tenantId,
		@Param("appCode") String appCode,
		@Param("bindingId") Long bindingId
	);

	/**
	 * 按主键集合批量执行物理删除，供资源/权限项级联清理使用。
	 */
	@Delete({
		"<script>",
		"DELETE FROM authz_res_derivation_perm",
		"WHERE tenant_id = #{tenantId}",
		"AND app_code = #{appCode}",
		"AND id IN",
		"<foreach collection='bindingIds' item='bindingId' open='(' separator=',' close=')'>",
		"#{bindingId}",
		"</foreach>",
		"</script>"
	})
	int deletePhysicalByIds(
		@Param("tenantId") String tenantId,
		@Param("appCode") String appCode,
		@Param("bindingIds") Collection<Long> bindingIds
	);
}