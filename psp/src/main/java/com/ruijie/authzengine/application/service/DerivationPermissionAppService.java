package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.common.ResourceModelCode;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.ResourceDerivationPermission;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 派生权限治理应用服务。
 *
 * <p>负责管理 `authz_res_derivation_perm` 写链路的治理约束，避免将一对一绑定、
 * 直接/间接授权互斥等规则散落到 Controller、Mapper 或 SQL 中。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DerivationPermissionAppService {

    private static final Set<String> SUPPORTED_RES_TYPES = new LinkedHashSet<>(Arrays.asList(
        ResourceModelCode.RES_UI_PAGE.name(),
        ResourceModelCode.RES_UI_COMPONENT.name(),
        ResourceModelCode.RES_API.name()
    ));

    private static final Set<String> UI_ALLOWED_PERMISSION_MODELS = new LinkedHashSet<>(Arrays.asList(
        ResourceModelCode.RES_DATA_BO.name(),
        ResourceModelCode.RES_API.name()
    ));

    private final DerivationPermissionRepository derivationPermissionRepository;

    private final ResourceRepository resourceRepository;

    private final PermissionRepository permissionRepository;

    /**
     * 分页查询派生权限关联。
     *
     * @param tenantId 租户标识
     * @param appCode  应用标识
     * @param resType  资源类型过滤（可为空则查全部）
     * @param keyword  模糊关键词（可为空）
     * @param pageNo   页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<ResourceDerivationPermission> pageBindings(String tenantId, String appCode,
                                                                  String resType, String keyword,
                                                                  int pageNo, int pageSize) {
        return derivationPermissionRepository.pageBindings(tenantId, appCode, resType, keyword, pageNo, pageSize);
    }

    /**
     * 保存或更新派生权限关联。
     *
     * @param binding 派生权限关联定义
     * @return 已保存结果
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public ResourceDerivationPermission saveBinding(ResourceDerivationPermission binding) {
        ResourceDerivationPermission normalizedBinding = normalize(binding);
        validateBindingPayload(normalizedBinding);
        ensureResourceExists(normalizedBinding);
        AuthPermissionItem permissionItem = requirePermissionItem(normalizedBinding);
        validatePermissionScope(normalizedBinding, permissionItem);
        validateDuplicateBinding(normalizedBinding);
        if (ResourceModelCode.RES_API.name().equals(normalizedBinding.getResType())) {
            validateApiBinding(normalizedBinding, permissionItem);
        }
        ResourceDerivationPermission saved = derivationPermissionRepository.saveBinding(normalizedBinding);
        log.info("[派生权限服务] 保存派生绑定: tenantId={}, appCode={}, resType={}, resId={}, permItemId={}, bindingId={}",
            saved.getTenantId(), saved.getAppCode(), saved.getResType(), saved.getResId(), saved.getPermItemId(), saved.getId());
        return saved;
    }

    /**
     * 删除派生权限关联。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param bindingId 关联主键
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public void deleteBinding(String tenantId, String appCode, Long bindingId) {
        ResourceDerivationPermission existing = derivationPermissionRepository.findBinding(tenantId, appCode, bindingId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "派生权限关联不存在");
        }
        derivationPermissionRepository.deleteBinding(tenantId, appCode, bindingId);
        log.info("[派生权限服务] 删除派生绑定: tenantId={}, appCode={}, bindingId={}", tenantId, appCode, bindingId);
    }

    private ResourceDerivationPermission normalize(ResourceDerivationPermission binding) {
        if (binding == null) {
            return null;
        }
        return ResourceDerivationPermission.builder()
            .id(binding.getId())
            .tenantId(binding.getTenantId())
            .appCode(binding.getAppCode())
            .resType(StringUtils.hasText(binding.getResType()) ? binding.getResType().trim().toUpperCase() : null)
            .resId(binding.getResId())
            .permItemId(binding.getPermItemId())
            .sortOrder(binding.getSortOrder() == null ? 0 : binding.getSortOrder())
            .build();
    }

    private void validateBindingPayload(ResourceDerivationPermission binding) {
        if (binding == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "派生权限关联不能为空");
        }
        if (!StringUtils.hasText(binding.getTenantId()) || !StringUtils.hasText(binding.getAppCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "派生权限关联缺少 tenantId 或 appCode");
        }
        if (!StringUtils.hasText(binding.getResType()) || !SUPPORTED_RES_TYPES.contains(binding.getResType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "派生权限关联的资源类型不合法，仅支持页面、组件、API");
        }
        if (binding.getResId() == null || binding.getPermItemId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "派生权限关联缺少 resId 或 permItemId");
        }
    }

    private void ensureResourceExists(ResourceDerivationPermission binding) {
        if (ResourceModelCode.RES_UI_PAGE.name().equals(binding.getResType())) {
            if (resourceRepository.findPageById(binding.getTenantId(), binding.getAppCode(), binding.getResId()) == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "派生权限关联的页面资源不存在");
            }
            return;
        }
        if (ResourceModelCode.RES_UI_COMPONENT.name().equals(binding.getResType())) {
            if (resourceRepository.findComponentById(binding.getTenantId(), binding.getAppCode(), binding.getResId()) == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "派生权限关联的组件资源不存在");
            }
            return;
        }
        if (resourceRepository.findApiById(binding.getTenantId(), binding.getAppCode(), binding.getResId()) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "派生权限关联的 API 资源不存在");
        }
    }

    private AuthPermissionItem requirePermissionItem(ResourceDerivationPermission binding) {
        List<AuthPermissionItem> permissionItems = permissionRepository.findPermissionItemsByIds(
            binding.getTenantId(),
            binding.getAppCode(),
            Collections.singletonList(binding.getPermItemId())
        );
        if (permissionItems.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "派生权限关联的权限项不存在");
        }
        return permissionItems.get(0);
    }

    private void validatePermissionScope(ResourceDerivationPermission binding, AuthPermissionItem permissionItem) {
        String resModelCode = permissionItem == null ? null : permissionItem.getResModelCode();
        if (ResourceModelCode.RES_API.name().equals(binding.getResType())) {
            if (!ResourceModelCode.RES_DATA_BO.name().equals(resModelCode)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "API 派生仅允许绑定 BO 操作权限项");
            }
            return;
        }
        if (!UI_ALLOWED_PERMISSION_MODELS.contains(resModelCode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "页面或组件仅允许关联 API / BO 权限项");
        }
    }

    private void validateDuplicateBinding(ResourceDerivationPermission binding) {
        boolean duplicatedBinding = derivationPermissionRepository.listBindingsByResource(
                binding.getTenantId(), binding.getAppCode(), binding.getResType(), binding.getResId())
            .stream()
            .filter(Objects::nonNull)
            .anyMatch(item -> !Objects.equals(item.getId(), binding.getId())
                && Objects.equals(item.getPermItemId(), binding.getPermItemId()));
        if (duplicatedBinding) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "相同资源与权限项的派生关联已存在");
        }
    }

    private void validateApiBinding(ResourceDerivationPermission binding, AuthPermissionItem permissionItem) {
        SysResApi api = resourceRepository.findApiById(binding.getTenantId(), binding.getAppCode(), binding.getResId());
        if (api == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "派生权限关联的 API 资源不存在");
        }
        boolean duplicatedBinding = derivationPermissionRepository.listBindingsByResource(
                binding.getTenantId(), binding.getAppCode(), binding.getResType(), binding.getResId())
            .stream()
            .filter(Objects::nonNull)
            .anyMatch(item -> !Objects.equals(item.getId(), binding.getId()));
        if (duplicatedBinding) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "同一 API 只允许绑定一个 BO 权限项");
        }
        boolean directPermissionExists = permissionRepository.findPermissionItemsByResModelCode(
                binding.getTenantId(), binding.getAppCode(), ResourceModelCode.RES_API.name())
            .stream()
            .anyMatch(item -> api.getApiCode().equals(item.getResId()));
        if (directPermissionExists) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "API 已配置直接授权，禁止再配置间接授权");
        }
        if (!StringUtils.hasText(permissionItem.getResId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "BO 操作权限项缺少业务对象标识，无法建立 API 派生关系");
        }
    }
}