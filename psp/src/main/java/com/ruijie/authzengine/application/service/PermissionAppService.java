package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 权限项治理应用服务。
 * <p>权限项是授权分配和委托的核心引用对象，删除前必须校验是否仍被引用。</p>
 */
@Slf4j
@Service
public class PermissionAppService {

    /** authz_permission_item 允许的资源模型编码，仅 RES_DATA_BO 和 RES_API。 */
    private static final Set<String> ALLOWED_RES_MODEL_CODES =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList("RES_DATA_BO", "RES_API")));

    private static final MetaRepository NO_OP_META_REPOSITORY = new MetaRepository() {
        @Override
        public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition saveAuthMetaModel(
            com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition definition
        ) {
            return definition;
        }

        @Override
        public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition saveBoMetaModel(
            com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition definition
        ) {
            return definition;
        }
    };

    private static final ResourceRepository NO_OP_RESOURCE_REPOSITORY = new ResourceRepository() {
    };

    private final PermissionRepository permissionRepository;

    private final PermissionCodeService permissionCodeService;

    public PermissionAppService(PermissionRepository permissionRepository) {
        this(permissionRepository, new PermissionCodeService(NO_OP_META_REPOSITORY, NO_OP_RESOURCE_REPOSITORY));
    }

    @Autowired
    public PermissionAppService(PermissionRepository permissionRepository, PermissionCodeService permissionCodeService) {
        this.permissionRepository = permissionRepository;
        this.permissionCodeService = permissionCodeService;
    }

    public PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return pagePermissionItems(tenantId, appCode, keyword, null, null, pageNo, pageSize);
    }

    /**
     * 分页查询权限项，支持按资源模型编码与资源标识过滤。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param keyword 关键词，可为空
     * @param resModelCode 资源模型编码，可为空
     * @param resId 资源标识，可为空
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @return 权限项分页结果
     */
    public PageResult<AuthPermissionItem> pagePermissionItems(
            String tenantId,
            String appCode,
            String keyword,
            String resModelCode,
            String resId,
            int pageNo,
            int pageSize) {
        return permissionRepository.pagePermissionItems(tenantId, appCode, keyword, resModelCode, resId, pageNo, pageSize);
    }

    public AuthPermissionItem getPermissionItem(String tenantId, String appCode, String permCode) {
        AuthPermissionItem item = permissionRepository.findPermissionItem(tenantId, appCode, permCode);
        if (item == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "权限项不存在");
        }
        return item;
    }

    /**
     * 创建权限项。若同一 permCode 已存在则等同于更新（仓储层 upsert）。
     */
    public AuthPermissionItem createPermissionItem(AuthPermissionItem item) {
        validateResModelCode(item.getResModelCode());
        PermissionCodeService.ResolvedPermissionCode resolved = permissionCodeService.resolvePermissionCode(
            item.getTenantId(),
            item.getAppCode(),
            item.getResModelCode(),
            item.getResId(),
            item.getActCode()
        );
        item.setPermCode(resolved.getPermCode());
        item.setResId(resolved.getNormalizedResId());
        item.setActCode(resolved.getActionCode());
        log.info("[权限项服务] 创建权限项: tenantId={}, appCode={}, permCode={}, actCode={}",
            item.getTenantId(), item.getAppCode(), item.getPermCode(), item.getActCode());
        AuthPermissionItem saved = permissionRepository.savePermissionItem(item);
        log.debug("[权限项服务] 权限项保存完成: id={}", saved.getId());
        return saved;
    }

    /**
     * 更新权限项。先确认记录存在，再写入 id/租户/应用/permCode 不改变。
     */
    public AuthPermissionItem updatePermissionItem(String tenantId, String appCode, String permCode, AuthPermissionItem item) {
        validateResModelCode(item.getResModelCode());
        AuthPermissionItem existing = getPermissionItem(tenantId, appCode, permCode);
        PermissionCodeService.ResolvedPermissionCode resolved = permissionCodeService.resolvePermissionCode(
            tenantId,
            appCode,
            item.getResModelCode(),
            item.getResId(),
            item.getActCode()
        );
        if (!permCode.equals(resolved.getPermCode())) {
            throw new BusinessException(ErrorCode.RELATION_RECREATE_REQUIRED, "资源类型、资源编码或动作编码变更必须删除后重建");
        }
        item.setId(existing.getId());
        item.setTenantId(tenantId);
        item.setAppCode(appCode);
        item.setPermCode(permCode);
        item.setResId(resolved.getNormalizedResId());
        item.setActCode(resolved.getActionCode());
        log.info("[权限项服务] 更新权限项: tenantId={}, appCode={}, permCode={}", tenantId, appCode, permCode);
        return permissionRepository.savePermissionItem(item);
    }

    /**
     * 校验资源模型编码合法性，只允许 RES_DATA_BO 和 RES_API。
     */
    private void validateResModelCode(String resModelCode) {
        if (!ALLOWED_RES_MODEL_CODES.contains(resModelCode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                "资源模型编码不合法，只允许 RES_DATA_BO 或 RES_API，当前值: " + resModelCode);
        }
    }

    /**
     * 删除权限项。
     * <p>检查过引用（分配表/委托表）后才执行删除，防止引用悬空。</p>
     */
    public void deletePermissionItem(String tenantId, String appCode, String permCode) {
        getPermissionItem(tenantId, appCode, permCode);
        if (permissionRepository.hasPermissionItemReference(tenantId, appCode, permCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "权限项仍被授权分配或委托引用，禁止删除");
        }
        log.info("[权限项服务] 删除权限项: tenantId={}, appCode={}, permCode={}", tenantId, appCode, permCode);
        permissionRepository.deletePermissionItem(tenantId, appCode, permCode);
    }
}
