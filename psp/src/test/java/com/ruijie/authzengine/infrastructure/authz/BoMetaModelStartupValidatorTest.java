package com.ruijie.authzengine.infrastructure.authz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.permission.ResourceDerivationPermission;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class BoMetaModelStartupValidatorTest {

    private MetaRepository metaRepository;
    private ResourceRepository resourceRepository;
    private PermissionRepository permissionRepository;
    private DerivationPermissionRepository derivationPermissionRepository;
    private BoMetaModelStartupValidator validator;

    @BeforeEach
    void setUp() {
        metaRepository = Mockito.mock(MetaRepository.class);
        resourceRepository = Mockito.mock(ResourceRepository.class);
        permissionRepository = Mockito.mock(PermissionRepository.class);
        derivationPermissionRepository = Mockito.mock(DerivationPermissionRepository.class);
        validator = new BoMetaModelStartupValidator(
            metaRepository,
            resourceRepository,
            permissionRepository,
            derivationPermissionRepository,
            new ObjectMapper());
    }

    @Test
    void 应对同优先级通配符多命中输出警告() {
        Map<String, java.util.List<String>> tenantApps = new LinkedHashMap<>();
        tenantApps.put("1", Collections.singletonList("APP"));
        when(metaRepository.listDistinctTenantApps()).thenReturn(tenantApps);

        SysResApi templateApi = SysResApi.builder()
            .id(1L)
            .apiCode("API-TEMPLATE")
            .httpMethod("GET")
            .uriPattern("/orders/{id}")
            .build();
        SysResApi wildcardApi = SysResApi.builder()
            .id(2L)
            .apiCode("API-WILDCARD")
            .httpMethod("GET")
            .uriPattern("/orders/{orderId}")
            .build();

        when(resourceRepository.listApis("1", "APP")).thenReturn(Arrays.asList(templateApi, wildcardApi));
        when(permissionRepository.findPermissionItemsByResModelCode("1", "APP", "RES_API"))
            .thenReturn(Arrays.asList(
                AuthPermissionItem.builder().resId("API-TEMPLATE").build(),
                AuthPermissionItem.builder().resId("API-WILDCARD").build()));
        when(derivationPermissionRepository.listBindingsByResource("1", "APP", "RES_API", 1L))
            .thenReturn(Collections.emptyList());
        when(derivationPermissionRepository.listBindingsByResource("1", "APP", "RES_API", 2L))
            .thenReturn(Collections.emptyList());

        assertEquals(1, validator.validateApiRouteUniqueness());
    }

    @Test
    void 应对跨直接与间接授权模式的重叠路由输出警告() {
        Map<String, java.util.List<String>> tenantApps = new LinkedHashMap<>();
        tenantApps.put("1", Collections.singletonList("APP"));
        when(metaRepository.listDistinctTenantApps()).thenReturn(tenantApps);

        SysResApi exactApi = SysResApi.builder()
            .id(10L)
            .apiCode("API-EXACT")
            .httpMethod("GET")
            .uriPattern("/orders/detail")
            .build();
        SysResApi wildcardApi = SysResApi.builder()
            .id(11L)
            .apiCode("API-WILDCARD")
            .httpMethod("GET")
            .uriPattern("/orders/**")
            .build();

        when(resourceRepository.listApis("1", "APP")).thenReturn(Arrays.asList(exactApi, wildcardApi));
        when(permissionRepository.findPermissionItemsByResModelCode("1", "APP", "RES_API"))
            .thenReturn(Collections.singletonList(AuthPermissionItem.builder().resId("API-EXACT").build()));
        when(derivationPermissionRepository.listBindingsByResource("1", "APP", "RES_API", 10L))
            .thenReturn(Collections.emptyList());
        when(derivationPermissionRepository.listBindingsByResource("1", "APP", "RES_API", 11L))
            .thenReturn(Collections.singletonList(ResourceDerivationPermission.builder().permItemId(100L).build()));

        assertEquals(1, validator.validateApiRouteUniqueness());
    }
}