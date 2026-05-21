package com.ruijie.authzengine.infrastructure.authz;

import com.ruijie.authzengine.shared.exception.AuthzIntegrationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AuthMetaResolverRouterTest {

    @Test
    void shouldTreatNoopHookAsNoopResolver() {
        AuthMetaResolverRouter router = AuthMetaResolverRouter.noop();

        Assertions.assertNull(router.resolve("JAVA_BEAN", "noopHook"));
    }

    @Test
    void shouldRejectUnsupportedAdapterType() {
        AuthMetaResolverRouter router = AuthMetaResolverRouter.noop();

        Assertions.assertThrows(AuthzIntegrationException.class, () -> router.resolve("HTTP", "subjectHook"));
    }

    @Test
    void shouldRejectMissingAdapterTypeWhenResolverIsConfigured() {
        AuthMetaResolverRouter router = AuthMetaResolverRouter.noop();

        Assertions.assertThrows(AuthzIntegrationException.class, () -> router.resolve(null, "subjectHook"));
    }
}