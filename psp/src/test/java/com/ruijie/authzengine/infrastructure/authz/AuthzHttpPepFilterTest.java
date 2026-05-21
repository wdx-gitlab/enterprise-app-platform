package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.application.service.AuthzFacade;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.DecisionType;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.model.governance.permission.ResourceDerivationPermission;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.repository.DerivationPermissionRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.domain.spi.AuthzSubjectProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthzHttpPepFilter 单元测试。
 */
class AuthzHttpPepFilterTest {

    private AuthzFacade authzFacade;
    private AuthzSubjectProvider subjectProvider;
    private ResourceRepository resourceRepository;
    private PermissionRepository permissionRepository;
        private DerivationPermissionRepository derivationPermissionRepository;
    private AuthzHttpPepFilter filter;

    @BeforeEach
    void setUp() {
        authzFacade = Mockito.mock(AuthzFacade.class);
        subjectProvider = Mockito.mock(AuthzSubjectProvider.class);
        resourceRepository = Mockito.mock(ResourceRepository.class);
        permissionRepository = Mockito.mock(PermissionRepository.class);
        derivationPermissionRepository = Mockito.mock(DerivationPermissionRepository.class);
        filter = new AuthzHttpPepFilter(
                authzFacade,
                subjectProvider,
                resourceRepository,
                permissionRepository,
                derivationPermissionRepository,
                "1",
                "SBG-SALARY",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "ALLOW");
    }

    @Test
    void 应按ApiCode匹配标准API权限项() throws Exception {
        SysResApi api = SysResApi.builder()
                .id(123L)
                .apiCode("API-SALARY-CALC-LOG-LIST")
                .httpMethod("GET")
                .uriPattern("/sbg-salary-service/salary/calculation-logs")
                .status("ENABLED")
                .build();
        AuthPermissionItem permItem = AuthPermissionItem.builder()
                .permCode("SBG-SALARY:api:API-SALARY-CALC-LOG-LIST:READ")
                .resModelCode("RES_API")
                .resId("API-SALARY-CALC-LOG-LIST")
                .actCode("READ")
                .build();
        when(resourceRepository.listApis("1", "SBG-SALARY"))
                .thenReturn(Collections.singletonList(api));
        when(permissionRepository.findPermissionItemsByResModelCode("1", "SBG-SALARY", "RES_API"))
                .thenReturn(Collections.singletonList(permItem));
        when(subjectProvider.getCurrentUserId()).thenReturn("U100");
        when(authzFacade.checkByPermCode("1", "SBG-SALARY", "U100", "SBG-SALARY:api:API-SALARY-CALC-LOG-LIST:READ", null))
                .thenReturn(AuthzDecision.builder().decision(DecisionType.NOT_PERMIT).reason("denied").build());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sbg-salary-service/salary/calculation-logs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(authzFacade).checkByPermCode("1", "SBG-SALARY", "U100", "SBG-SALARY:api:API-SALARY-CALC-LOG-LIST:READ", null);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void 应优先命中精确路径并使用当前登录用户鉴权() throws Exception {
        SysResApi wildcardApi = SysResApi.builder()
                .id(120L)
                .apiCode("API-SALARY-WILDCARD")
                .httpMethod("GET")
                .uriPattern("/sbg-salary-service/salary/**")
                .status("ENABLED")
                .build();
        SysResApi exactApi = SysResApi.builder()
                .id(121L)
                .apiCode("API-SALARY-EXACT")
                .httpMethod("GET")
                .uriPattern("/sbg-salary-service/salary/calculation-logs")
                .status("ENABLED")
                .build();
        AuthPermissionItem wildcardPerm = AuthPermissionItem.builder()
                .id(10L)
                .permCode("SBG-SALARY:api:API-SALARY-WILDCARD:READ")
                .resModelCode("RES_API")
                .resId("API-SALARY-WILDCARD")
                .actCode("READ")
                .build();
        AuthPermissionItem exactPerm = AuthPermissionItem.builder()
                .id(11L)
                .permCode("SBG-SALARY:api:API-SALARY-EXACT:READ")
                .resModelCode("RES_API")
                .resId("API-SALARY-EXACT")
                .actCode("READ")
                .build();

        when(resourceRepository.listApis("1", "SBG-SALARY"))
                .thenReturn(Arrays.asList(wildcardApi, exactApi));
        when(permissionRepository.findPermissionItemsByResModelCode("1", "SBG-SALARY", "RES_API"))
                .thenReturn(Arrays.asList(wildcardPerm, exactPerm));
        when(subjectProvider.getCurrentUserId()).thenReturn("U100");
        when(authzFacade.checkByPermCode("1", "SBG-SALARY", "U100", "SBG-SALARY:api:API-SALARY-EXACT:READ", null))
                .thenReturn(AuthzDecision.permit(Collections.singletonList("SBG-SALARY:api:API-SALARY-EXACT:READ")));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sbg-salary-service/salary/calculation-logs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(authzFacade).checkByPermCode("1", "SBG-SALARY", "U100", "SBG-SALARY:api:API-SALARY-EXACT:READ", null);
        verify(authzFacade, never()).checkByPermCode("1", "SBG-SALARY", "U100", "SBG-SALARY:api:API-SALARY-WILDCARD:READ", null);
        verify(chain).doFilter(request, response);
    }

    @Test
    void 应在同优先级路由多命中时拒绝放行() throws Exception {
        SysResApi apiA = SysResApi.builder()
                .id(201L)
                .apiCode("API-A")
                .httpMethod("GET")
                .uriPattern("/sbg-salary-service/salary/calculation-logs")
                .status("ENABLED")
                .build();
        SysResApi apiB = SysResApi.builder()
                .id(202L)
                .apiCode("API-B")
                .httpMethod("GET")
                .uriPattern("/sbg-salary-service/salary/calculation-logs")
                .status("ENABLED")
                .build();

        when(resourceRepository.listApis("1", "SBG-SALARY"))
                .thenReturn(Arrays.asList(apiA, apiB));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sbg-salary-service/salary/calculation-logs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(409, response.getStatus());
        verify(authzFacade, never()).checkByPermCode(any(), any(), any(), any(), any());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void 应在API同时配置直接与间接授权时拒绝放行() throws Exception {
        SysResApi api = SysResApi.builder()
                .id(123L)
                .apiCode("API-SALARY-CALC-LOG-LIST")
                .httpMethod("GET")
                .uriPattern("/sbg-salary-service/salary/calculation-logs")
                .status("ENABLED")
                .build();
        AuthPermissionItem apiPermItem = AuthPermissionItem.builder()
                .id(10L)
                .permCode("SBG-SALARY:api:API-SALARY-CALC-LOG-LIST:READ")
                .resModelCode("RES_API")
                .resId("API-SALARY-CALC-LOG-LIST")
                .actCode("READ")
                .build();
        AuthPermissionItem boPermItem = AuthPermissionItem.builder()
                .id(20L)
                .permCode("SBG-SALARY:bo:cacllog:READ")
                .resModelCode("RES_DATA_BO")
                .resId("900")
                .actCode("READ")
                .build();
        ResourceDerivationPermission binding = ResourceDerivationPermission.builder()
                .id(300L)
                .tenantId("1")
                .appCode("SBG-SALARY")
                .resType("RES_API")
                .resId(123L)
                .permItemId(20L)
                .build();

        when(resourceRepository.listApis("1", "SBG-SALARY"))
                .thenReturn(Collections.singletonList(api));
        when(permissionRepository.findPermissionItemsByResModelCode("1", "SBG-SALARY", "RES_API"))
                .thenReturn(Collections.singletonList(apiPermItem));
        when(derivationPermissionRepository.listBindingsByResource("1", "SBG-SALARY", "RES_API", 123L))
                .thenReturn(Collections.singletonList(binding));
        when(permissionRepository.findPermissionItemsByIds("1", "SBG-SALARY", Collections.singletonList(20L)))
                .thenReturn(Collections.singletonList(boPermItem));
        when(subjectProvider.getCurrentUserId()).thenReturn("U100");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sbg-salary-service/salary/calculation-logs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(409, response.getStatus());
        verify(authzFacade, never()).checkByPermCode(any(), any(), any(), any(), any());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void 应在多命中候选跨直接与间接授权模式时拒绝放行() throws Exception {
        SysResApi wildcardApi = SysResApi.builder()
                .id(301L)
                .apiCode("API-SALARY-WILDCARD")
                .httpMethod("GET")
                .uriPattern("/sbg-salary-service/salary/**")
                .status("ENABLED")
                .build();
        SysResApi exactApi = SysResApi.builder()
                .id(302L)
                .apiCode("API-SALARY-EXACT")
                .httpMethod("GET")
                .uriPattern("/sbg-salary-service/salary/calculation-logs")
                .status("ENABLED")
                .build();
        AuthPermissionItem exactPerm = AuthPermissionItem.builder()
                .id(31L)
                .permCode("SBG-SALARY:api:API-SALARY-EXACT:READ")
                .resModelCode("RES_API")
                .resId("API-SALARY-EXACT")
                .actCode("READ")
                .build();
        AuthPermissionItem wildcardBoPerm = AuthPermissionItem.builder()
                .id(32L)
                .permCode("SBG-SALARY:bo:cacllog:READ")
                .resModelCode("RES_DATA_BO")
                .resId("900")
                .actCode("READ")
                .build();
        ResourceDerivationPermission wildcardBinding = ResourceDerivationPermission.builder()
                .id(330L)
                .tenantId("1")
                .appCode("SBG-SALARY")
                .resType("RES_API")
                .resId(301L)
                .permItemId(32L)
                .build();

        when(resourceRepository.listApis("1", "SBG-SALARY"))
                .thenReturn(Arrays.asList(wildcardApi, exactApi));
        when(permissionRepository.findPermissionItemsByResModelCode("1", "SBG-SALARY", "RES_API"))
                .thenReturn(Collections.singletonList(exactPerm));
        when(derivationPermissionRepository.listBindingsByResource("1", "SBG-SALARY", "RES_API", 301L))
                .thenReturn(Collections.singletonList(wildcardBinding));
        when(derivationPermissionRepository.listBindingsByResource("1", "SBG-SALARY", "RES_API", 302L))
                .thenReturn(Collections.emptyList());
        when(permissionRepository.findPermissionItemsByIds("1", "SBG-SALARY", Collections.singletonList(32L)))
                .thenReturn(Collections.singletonList(wildcardBoPerm));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sbg-salary-service/salary/calculation-logs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(409, response.getStatus());
        verify(authzFacade, never()).checkByPermCode(any(), any(), any(), any(), any());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void 应在仅存在API派生BO权限时按BO权限鉴权并透传义务() throws Exception {
        SysResApi api = SysResApi.builder()
                .id(123L)
                .apiCode("API-SALARY-CALC-LOG-LIST")
                .httpMethod("GET")
                .uriPattern("/sbg-salary-service/salary/calculation-logs")
                .status("ENABLED")
                .build();
        AuthPermissionItem boPermItem = AuthPermissionItem.builder()
                .id(20L)
                .permCode("SBG-SALARY:bo:cacllog:READ")
                .resModelCode("RES_DATA_BO")
                .resId("900")
                .actCode("READ")
                .build();
        ResourceDerivationPermission binding = ResourceDerivationPermission.builder()
                .id(300L)
                .tenantId("1")
                .appCode("SBG-SALARY")
                .resType("RES_API")
                .resId(123L)
                .permItemId(20L)
                .build();
        Map<String, Object> derivedObligations = new LinkedHashMap<>();
        List<Map<String, Object>> fieldControls = Collections.singletonList(new LinkedHashMap<String, Object>() {{
            put("fieldName", "calcType");
            put("columnName", "calc_type");
            put("action", "MASK");
            put("maskScript", "'***'");
        }});
        derivedObligations.put("fieldControls", fieldControls);

        when(resourceRepository.listApis("1", "SBG-SALARY"))
                .thenReturn(Collections.singletonList(api));
        when(permissionRepository.findPermissionItemsByResModelCode("1", "SBG-SALARY", "RES_API"))
                .thenReturn(Collections.emptyList());
        when(derivationPermissionRepository.listBindingsByResource("1", "SBG-SALARY", "RES_API", 123L))
                .thenReturn(Collections.singletonList(binding));
        when(permissionRepository.findPermissionItemsByIds("1", "SBG-SALARY", Collections.singletonList(20L)))
                .thenReturn(Collections.singletonList(boPermItem));
        when(subjectProvider.getCurrentUserId()).thenReturn("U100");
        when(authzFacade.checkByPermCode("1", "SBG-SALARY", "U100", "SBG-SALARY:bo:cacllog:READ", null))
                .thenReturn(AuthzDecision.permit(
                        Collections.singletonList("SBG-SALARY:bo:cacllog:READ"),
                        Collections.singletonList("2001"),
                        Collections.emptyList(),
                        Collections.singletonList("cacllog_mask"),
                        derivedObligations));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sbg-salary-service/salary/calculation-logs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(authzFacade).checkByPermCode("1", "SBG-SALARY", "U100", "SBG-SALARY:bo:cacllog:READ", null);
        verify(chain).doFilter(request, response);
        AuthzDecision mergedDecision = (AuthzDecision) request.getAttribute("authzDecision");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mergedFieldControls = (List<Map<String, Object>>) mergedDecision.getObligations().get("fieldControls");
        assertEquals("MASK", mergedFieldControls.get(0).get("action"));
        assertEquals("calcType", mergedFieldControls.get(0).get("fieldName"));
    }
}