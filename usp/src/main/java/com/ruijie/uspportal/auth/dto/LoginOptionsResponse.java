package com.ruijie.uspportal.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginOptionsResponse {

    private Boolean internalLoginEnabled;

    private Boolean ssoLoginEnabled;

    private String defaultLoginMode;

    private String ssoButtonText;
}
