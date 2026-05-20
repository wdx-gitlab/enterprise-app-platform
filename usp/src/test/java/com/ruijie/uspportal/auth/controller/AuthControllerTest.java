package com.ruijie.uspportal.auth.controller;

import com.ruijie.uspportal.auth.dto.LoginOptionsResponse;
import com.ruijie.uspportal.auth.service.AuthService;
import com.ruijie.uspportal.web.USPGlobalExceptonHandler;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthControllerTest {

    private MockMvc mockMvc;

    private AuthService authService;

    @Before
    public void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new USPGlobalExceptonHandler())
                .build();
    }

    @Test
    public void shouldReturnLoginOptions() throws Exception {
        when(authService.getLoginOptions()).thenReturn(LoginOptionsResponse.builder()
                .internalLoginEnabled(true)
                .ssoLoginEnabled(false)
                .defaultLoginMode("INTERNAL")
                .ssoButtonText("SSO 单点登录")
                .build());

        mockMvc.perform(get("/api/auth/login-options"))
                .andExpect(status().isOk())
            .andExpect(content().string(containsString("INTERNAL")));
    }

    @Test
    public void shouldValidateLoginPayload() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("请输入登录")));
    }

    @Test
    public void shouldLogoutSuccessfully() throws Exception {
        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout").header("union_session_ticket", "session-id"))
                .andExpect(status().isOk())
            .andExpect(content().string(containsString("退出成功")));
    }
}