package com.ruijie.uspportal.integration.cs05;

import com.ruijie.uspportal.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/cs05")
public class Cs05SsoController {

    @PostMapping("/callback")
    public ApiResponse<Void> callback() {
        return ApiResponse.failure("当前版本暂未开放 SSO 登录回调，请先使用平台管理员账号登录");
    }
}
