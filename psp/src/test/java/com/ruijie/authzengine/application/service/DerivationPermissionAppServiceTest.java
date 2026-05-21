package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.common.ResourceModelCode;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.permission.ResourceDerivationPermission;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.resource.SysResComponent;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.shared.exception.BusinessException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DerivationPermissionAppServiceTest {

    @Test
    void shouldRejectDuplicatePageBindingForSamePermissionItem() {
        InMemoryDerivationPermissionRepository derivationRepository = new InMemoryDerivationPermissionRepository();
        InMemoryResourceRepository resourceRepository = new InMemoryResourceRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        resourceRepository.pagesById.put(11L, SysResPage.builder()
            .id(11L)
            .tenantId("T001")
            .appCode("CRM")
            .pageCode("PAGE-CONTRACT-LIST")
            .pageName("合同列表")
            .build());
        permissionRepository.itemsById.put(101L, AuthPermissionItem.builder()
            .id(101L)
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:bo:CONTRACT:READ")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("READ")
            .build());
        derivationRepository.saveBinding(ResourceDerivationPermission.builder()
            .tenantId("T001")
            .appCode("CRM")
            .resType(ResourceModelCode.RES_UI_PAGE.name())
            .resId(11L)
            .permItemId(101L)
            .sortOrder(0)
            .build());
        DerivationPermissionAppService service = new DerivationPermissionAppService(
            derivationRepository, resourceRepository, permissionRepository);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> service.saveBinding(ResourceDerivationPermission.builder()
                .tenantId("T001")
                .appCode("CRM")
                .resType(ResourceModelCode.RES_UI_PAGE.name())
                .resId(11L)
                .permItemId(101L)
                .build()));

        Assertions.assertEquals("AUTHZ-409", exception.getCode());
    }

    @Test
    void shouldSavePageBindingWhenPermissionItemIsValid() {
        InMemoryDerivationPermissionRepository derivationRepository = new InMemoryDerivationPermissionRepository();
        InMemoryResourceRepository resourceRepository = new InMemoryResourceRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        resourceRepository.pagesById.put(11L, SysResPage.builder()
            .id(11L)
            .tenantId("T001")
            .appCode("CRM")
            .pageCode("PAGE-CONTRACT-LIST")
            .pageName("合同列表")
            .build());
        permissionRepository.itemsById.put(101L, AuthPermissionItem.builder()
            .id(101L)
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:bo:CONTRACT:READ")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("READ")
            .build());
        DerivationPermissionAppService service = new DerivationPermissionAppService(
            derivationRepository, resourceRepository, permissionRepository);

        ResourceDerivationPermission saved = service.saveBinding(ResourceDerivationPermission.builder()
            .tenantId("T001")
            .appCode("CRM")
            .resType(ResourceModelCode.RES_UI_PAGE.name())
            .resId(11L)
            .permItemId(101L)
            .build());

        Assertions.assertNotNull(saved.getId());
        Assertions.assertEquals(1, derivationRepository.bindings.size());
        Assertions.assertEquals(Integer.valueOf(0), saved.getSortOrder());
    }

    @Test
    void shouldRejectApiBindingWhenDirectPermissionExists() {
        InMemoryDerivationPermissionRepository derivationRepository = new InMemoryDerivationPermissionRepository();
        InMemoryResourceRepository resourceRepository = new InMemoryResourceRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        resourceRepository.apisById.put(21L, SysResApi.builder()
            .id(21L)
            .tenantId("T001")
            .appCode("CRM")
            .apiCode("API-CONTRACT-LIST")
            .apiName("合同查询")
            .httpMethod("GET")
            .uriPattern("/api/contracts")
            .build());
        permissionRepository.itemsById.put(201L, AuthPermissionItem.builder()
            .id(201L)
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:bo:CONTRACT:READ")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("READ")
            .build());
        permissionRepository.directApiPermissions.add(AuthPermissionItem.builder()
            .id(301L)
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:api:API-CONTRACT-LIST:READ")
            .resModelCode(ResourceModelCode.RES_API.name())
            .resId("API-CONTRACT-LIST")
            .actCode("READ")
            .build());
        DerivationPermissionAppService service = new DerivationPermissionAppService(
            derivationRepository, resourceRepository, permissionRepository);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> service.saveBinding(ResourceDerivationPermission.builder()
                .tenantId("T001")
                .appCode("CRM")
                .resType(ResourceModelCode.RES_API.name())
                .resId(21L)
                .permItemId(201L)
                .build()));

        Assertions.assertEquals("AUTHZ-409", exception.getCode());
    }

    @Test
    void shouldRejectSecondApiBindingForSameApi() {
        InMemoryDerivationPermissionRepository derivationRepository = new InMemoryDerivationPermissionRepository();
        InMemoryResourceRepository resourceRepository = new InMemoryResourceRepository();
        InMemoryPermissionRepository permissionRepository = new InMemoryPermissionRepository();
        resourceRepository.apisById.put(21L, SysResApi.builder()
            .id(21L)
            .tenantId("T001")
            .appCode("CRM")
            .apiCode("API-CONTRACT-LIST")
            .httpMethod("GET")
            .uriPattern("/api/contracts")
            .build());
        permissionRepository.itemsById.put(201L, AuthPermissionItem.builder()
            .id(201L)
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:bo:CONTRACT:READ")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("READ")
            .build());
        permissionRepository.itemsById.put(202L, AuthPermissionItem.builder()
            .id(202L)
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:bo:CONTRACT:EXPORT")
            .resModelCode(ResourceModelCode.RES_DATA_BO.name())
            .resId("1")
            .actCode("EXPORT")
            .build());
        derivationRepository.saveBinding(ResourceDerivationPermission.builder()
            .tenantId("T001")
            .appCode("CRM")
            .resType(ResourceModelCode.RES_API.name())
            .resId(21L)
            .permItemId(201L)
            .sortOrder(0)
            .build());
        DerivationPermissionAppService service = new DerivationPermissionAppService(
            derivationRepository, resourceRepository, permissionRepository);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> service.saveBinding(ResourceDerivationPermission.builder()
                .tenantId("T001")
                .appCode("CRM")
                .resType(ResourceModelCode.RES_API.name())
                .resId(21L)
                .permItemId(202L)
                .build()));

        Assertions.assertEquals("AUTHZ-409", exception.getCode());
    }

    private static final class InMemoryDerivationPermissionRepository implements DerivationPermissionRepository {

        private final AtomicLong idGenerator = new AtomicLong(1L);

        private final List<ResourceDerivationPermission> bindings = new ArrayList<>();

        @Override
        public ResourceDerivationPermission saveBinding(ResourceDerivationPermission binding) {
            ResourceDerivationPermission target = ResourceDerivationPermission.builder()
                .id(binding.getId() == null ? idGenerator.getAndIncrement() : binding.getId())
                .tenantId(binding.getTenantId())
                .appCode(binding.getAppCode())
                .resType(binding.getResType())
                .resId(binding.getResId())
                .permItemId(binding.getPermItemId())
                .sortOrder(binding.getSortOrder())
                .build();
            bindings.removeIf(item -> Objects.equals(item.getId(), target.getId()));
            bindings.add(target);
            return target;
        }

        @Override
        public ResourceDerivationPermission findBinding(String tenantId, String appCode, Long bindingId) {
            return bindings.stream()
                .filter(item -> tenantId.equals(item.getTenantId())
                    && appCode.equals(item.getAppCode())
                    && Objects.equals(bindingId, item.getId()))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<ResourceDerivationPermission> listBindingsByResource(String tenantId, String appCode, String resType, Long resId) {
            return bindings.stream()
                .filter(item -> tenantId.equals(item.getTenantId())
                    && appCode.equals(item.getAppCode())
                    && resType.equals(item.getResType())
                    && Objects.equals(resId, item.getResId()))
                .collect(Collectors.toList());
        }
    }

    private static final class InMemoryResourceRepository implements ResourceRepository {

        private final Map<Long, SysResPage> pagesById = new LinkedHashMap<>();

        private final Map<Long, SysResComponent> componentsById = new LinkedHashMap<>();

        private final Map<Long, SysResApi> apisById = new LinkedHashMap<>();

        @Override
        public SysResPage findPageById(String tenantId, String appCode, Long pageId) {
            return pagesById.get(pageId);
        }

        @Override
        public SysResComponent findComponentById(String tenantId, String appCode, Long componentId) {
            return componentsById.get(componentId);
        }

        @Override
        public SysResApi findApiById(String tenantId, String appCode, Long apiId) {
            return apisById.get(apiId);
        }
    }

    private static final class InMemoryPermissionRepository implements PermissionRepository {

        private final Map<Long, AuthPermissionItem> itemsById = new LinkedHashMap<>();

        private final List<AuthPermissionItem> directApiPermissions = new ArrayList<>();

        @Override
        public AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem) {
            itemsById.put(permissionItem.getId(), permissionItem);
            return permissionItem;
        }

        @Override
        public com.ruijie.authzengine.domain.model.governance.PageResult<AuthPermissionItem> pagePermissionItems(
                String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return com.ruijie.authzengine.domain.repository.ResourceRepository.emptyPage(pageNo, pageSize);
        }

        @Override
        public AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode) {
            return itemsById.values().stream()
                .filter(item -> permCode.equals(item.getPermCode()))
                .findFirst()
                .orElse(null);
        }

        @Override
        public void deletePermissionItem(String tenantId, String appCode, String permCode) {
        }

        @Override
        public boolean hasPermissionItemReference(String tenantId, String appCode, String permCode) {
            return false;
        }

        @Override
        public List<AuthPermissionItem> findPermissionItemsByIds(String tenantId, String appCode, List<Long> permItemIds) {
            return permItemIds.stream()
                .map(itemsById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        @Override
        public List<AuthPermissionItem> findPermissionItemsByResModelCode(String tenantId, String appCode, String resModelCode) {
            if (ResourceModelCode.RES_API.name().equals(resModelCode)) {
                return Collections.unmodifiableList(directApiPermissions);
            }
            return Collections.emptyList();
        }
    }
}