package com.ruijie.authzengine.application.sdk.impl;

import com.ruijie.authzengine.application.sdk.model.PermissionSnapshotResult;
import com.ruijie.authzengine.application.sdk.model.SubjectScopedQuery;
import com.ruijie.authzengine.application.sdk.model.UserContextResult;
import com.ruijie.authzengine.application.service.AuthzQueryAppService;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAuthzQueryServiceTest {

    @Mock
    private AuthzQueryAppService authzQueryAppService;

    private DefaultAuthzQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new DefaultAuthzQueryService(authzQueryAppService);
    }

    @Test
    void shouldWrapPermissionSnapshotResult() {
        when(authzQueryAppService.queryPermissionSnapshot("T001", "CRM", "U10001", "SUB_USER"))
            .thenReturn(Arrays.asList("PERM_CONTRACT_READ", "PERM_CONTRACT_APPROVE"));

        PermissionSnapshotResult result = queryService.queryPermissionSnapshot(SubjectScopedQuery.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subjectId("U10001")
            .build());

        verify(authzQueryAppService).queryPermissionSnapshot("T001", "CRM", "U10001", "SUB_USER");
        assertThat(result.getPermCodes()).containsExactly("PERM_CONTRACT_READ", "PERM_CONTRACT_APPROVE");
        assertThat(result.getEvalTimeMs()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void shouldMapUserContextResult() {
        Map<String, java.util.List<String>> accessibleResources = new LinkedHashMap<>();
        accessibleResources.put("RES_UI_MENU", Collections.singletonList("MENU_HOME"));
        Map<String, Boolean> visibility = new LinkedHashMap<>();
        visibility.put("BTN_APPROVE", true);
        visibility.put("BTN_DELETE", false);
        AuthzQueryAppService.UserContextResult appResult = new AuthzQueryAppService.UserContextResult(
            Collections.singletonList("PERM_CONTRACT_READ"),
            accessibleResources,
            Collections.singletonList(new AuthzQueryAppService.MenuTreeNodeResult(
                "MENU_HOME",
                "首页",
                "/home",
                Collections.emptyList()
            )),
            visibility,
            15L
        );
        when(authzQueryAppService.queryUserContext("T001", "CRM", "U10001", "SUB_USER"))
            .thenReturn(appResult);

        UserContextResult result = queryService.queryUserContext(SubjectScopedQuery.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subjectId("U10001")
            .build());

        assertThat(result.getSubjectId()).isEqualTo("U10001");
        assertThat(result.getSubjectModel()).isEqualTo("SUB_USER");
        assertThat(result.getPermCodes()).containsExactly("PERM_CONTRACT_READ");
        assertThat(result.getAccessibleResources()).containsEntry("RES_UI_MENU", Collections.singletonList("MENU_HOME"));
        assertThat(result.getMenuTree()).hasSize(1);
        assertThat(result.getMenuTree().get(0).getMenuCode()).isEqualTo("MENU_HOME");
        assertThat(result.getVisibility().get("BTN_APPROVE").isVisible()).isTrue();
        assertThat(result.getVisibility().get("BTN_DELETE").isVisible()).isFalse();
        assertThat(result.getEvalTimeMs()).isEqualTo(15L);
    }
}