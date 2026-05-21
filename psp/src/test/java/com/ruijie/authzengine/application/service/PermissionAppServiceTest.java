package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.shared.exception.BusinessException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PermissionAppServiceTest {

    @Test
    void shouldGeneratePermissionCodeAndNormalizeApiResIdOnCreate() {
        InMemoryPermissionRepository repository = new InMemoryPermissionRepository();
        PermissionAppService appService = new PermissionAppService(
            repository,
            new PermissionCodeService(new StubMetaRepository(), new StubResourceRepository())
        );

        AuthPermissionItem saved = appService.createPermissionItem(AuthPermissionItem.builder()
            .tenantId("T001")
            .appCode("CRM")
            .resModelCode("RES_API")
            .resId("12")
            .actCode("read")
            .build());

        Assertions.assertEquals("CRM:api:contract.query:READ", saved.getPermCode());
        Assertions.assertEquals("contract.query", saved.getResId());
        Assertions.assertEquals("READ", saved.getActCode());
    }

    @Test
    void shouldRejectUpdatingPermissionItemWhenCoreIdentityChanges() {
        InMemoryPermissionRepository repository = new InMemoryPermissionRepository();
        PermissionAppService appService = new PermissionAppService(
            repository,
            new PermissionCodeService(new StubMetaRepository(), new StubResourceRepository())
        );
        repository.savePermissionItem(AuthPermissionItem.builder()
            .id(1L)
            .tenantId("T001")
            .appCode("CRM")
            .permCode("CRM:api:contract.query:READ")
            .resModelCode("RES_API")
            .resId("contract.query")
            .actCode("READ")
            .build());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> appService.updatePermissionItem("T001", "CRM", "CRM:api:contract.query:READ", AuthPermissionItem.builder()
                .resModelCode("RES_API")
                .resId("12")
                .actCode("LIST")
                .build()));

        Assertions.assertEquals("AUTHZ-409-RELATION", exception.getCode());
    }

    @Test
    void shouldRejectDeletingReferencedPermissionItem() {
        PermissionAppService appService = new PermissionAppService(new PermissionRepository() {
            @Override
            public AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem) {
                return permissionItem;
            }

            @Override
            public PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
                return PageResult.<AuthPermissionItem>builder().pageNo(pageNo).pageSize(pageSize).total(1).records(java.util.Collections.emptyList()).build();
            }

            @Override
            public AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode) {
                return AuthPermissionItem.builder().id(1L).tenantId(tenantId).appCode(appCode).permCode(permCode).build();
            }

            @Override
            public void deletePermissionItem(String tenantId, String appCode, String permCode) {
            }

            @Override
            public boolean hasPermissionItemReference(String tenantId, String appCode, String permCode) {
                return true;
            }
        });

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> appService.deletePermissionItem("T001", "CRM", "PERM-001"));
        Assertions.assertEquals("AUTHZ-409-DELETE", exception.getCode());
    }

    @Test
    void shouldForwardResourceFiltersWhenPagingPermissionItems() {
        AtomicReference<String> forwardedResModelCode = new AtomicReference<>();
        AtomicReference<String> forwardedResId = new AtomicReference<>();
        PermissionAppService appService = new PermissionAppService(new PermissionRepository() {
            @Override
            public AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem) {
                return permissionItem;
            }

            @Override
            public PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
                throw new AssertionError("应优先调用带资源过滤的新分页方法");
            }

            @Override
            public PageResult<AuthPermissionItem> pagePermissionItems(
                    String tenantId,
                    String appCode,
                    String keyword,
                    String resModelCode,
                    String resId,
                    int pageNo,
                    int pageSize) {
                forwardedResModelCode.set(resModelCode);
                forwardedResId.set(resId);
                return PageResult.<AuthPermissionItem>builder()
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .total(0)
                    .records(java.util.Collections.emptyList())
                    .build();
            }

            @Override
            public AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode) {
                return null;
            }

            @Override
            public void deletePermissionItem(String tenantId, String appCode, String permCode) {
            }

            @Override
            public boolean hasPermissionItemReference(String tenantId, String appCode, String permCode) {
                return false;
            }
        });

        appService.pagePermissionItems("T001", "CRM", "contract", "RES_DATA_BO", "1001", 1, 20);

        Assertions.assertEquals("RES_DATA_BO", forwardedResModelCode.get());
        Assertions.assertEquals("1001", forwardedResId.get());
    }

    private static class InMemoryPermissionRepository implements PermissionRepository {

        private AuthPermissionItem item;

        @Override
        public AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem) {
            this.item = permissionItem;
            return permissionItem;
        }

        @Override
        public PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return PageResult.<AuthPermissionItem>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(item == null ? 0 : 1)
                .records(item == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(item))
                .build();
        }

        @Override
        public AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode) {
            if (item == null || !permCode.equals(item.getPermCode())) {
                return null;
            }
            return item;
        }

        @Override
        public void deletePermissionItem(String tenantId, String appCode, String permCode) {
            item = null;
        }

        @Override
        public boolean hasPermissionItemReference(String tenantId, String appCode, String permCode) {
            return false;
        }
    }

    private static class StubMetaRepository implements MetaRepository {

        @Override
        public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
            return definition;
        }

        @Override
        public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
            return definition;
        }
    }

    private static class StubResourceRepository implements ResourceRepository {

        @Override
        public SysResApi findApiById(String tenantId, String appCode, Long apiId) {
            if (!Long.valueOf(12L).equals(apiId)) {
                return null;
            }
            return SysResApi.builder()
                .id(apiId)
                .tenantId(tenantId)
                .appCode(appCode)
                .apiCode("contract.query")
                .build();
        }
    }
}
