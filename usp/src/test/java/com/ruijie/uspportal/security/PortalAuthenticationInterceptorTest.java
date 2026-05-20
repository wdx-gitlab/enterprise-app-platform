package com.ruijie.uspportal.security;

import com.ruijie.uspportal.auth.integration.sso.SsoAuthenticationService;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PortalAuthenticationInterceptorTest {

    @Test
    public void shouldBypassAuthenticationForWhitelistedLoginOptionsWithContextPath() {
        SsoAuthenticationService ssoAuthenticationService = mock(SsoAuthenticationService.class);
        PortalAuthenticationInterceptor interceptor = new PortalAuthenticationInterceptor(
                ssoAuthenticationService,
                Arrays.asList("/usp-portal/api/auth/login", "/usp-portal/api/auth/login-options"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/usp-portal/api/auth/login-options");
        request.setContextPath("/usp-portal");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        verify(ssoAuthenticationService, never()).authenticate(request, response);
    }
}