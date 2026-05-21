package com.ruijie.authzengine.autoconfigure;

import com.ruijie.authzengine.application.sdk.AuthzDataScopeService;
import com.ruijie.authzengine.application.sdk.AuthzQueryService;
import com.ruijie.authzengine.application.sdk.AuthzRuntimeService;
import com.ruijie.authzengine.application.sdk.impl.DefaultAuthzDataScopeService;
import com.ruijie.authzengine.application.sdk.impl.DefaultAuthzQueryService;
import com.ruijie.authzengine.application.sdk.impl.DefaultAuthzRuntimeService;
import com.ruijie.authzengine.application.service.AuthzContractAppService;
import com.ruijie.authzengine.application.service.AuthzDecisionAppService;
import com.ruijie.authzengine.application.service.AuthzFacade;
import com.ruijie.authzengine.application.service.AuthzQueryAppService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthzSdkServiceFactoryTest {

    private final AuthzEngineAutoConfiguration autoConfiguration = new AuthzEngineAutoConfiguration();

    @Test
    void shouldCreateRuntimeServiceBean() {
        AuthzRuntimeService service = autoConfiguration.authzRuntimeService(
            mock(AuthzDecisionAppService.class),
            mock(AuthzFacade.class)
        );

        assertThat(service).isInstanceOf(DefaultAuthzRuntimeService.class);
    }

    @Test
    void shouldCreateQueryServiceBean() {
        AuthzQueryService service = autoConfiguration.authzQueryService(mock(AuthzQueryAppService.class));

        assertThat(service).isInstanceOf(DefaultAuthzQueryService.class);
    }

    @Test
    void shouldCreateDataScopeServiceBean() {
        AuthzDataScopeService service = autoConfiguration.authzDataScopeService(mock(AuthzContractAppService.class));

        assertThat(service).isInstanceOf(DefaultAuthzDataScopeService.class);
    }
}