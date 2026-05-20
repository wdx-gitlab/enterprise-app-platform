package com.ruijie.uspportal.integration.dap;

import com.ruijie.uspportal.security.CurrentUserContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DapUserContextClient {

    public Map<String, Object> getCurrentUserContext() {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.get();
        Map<String, Object> context = new LinkedHashMap<>();
        if (currentUser != null) {
            context.put("userId", currentUser.getUserId());
            context.put("loginName", currentUser.getLoginName());
            context.put("displayName", currentUser.getDisplayName());
            context.put("tenantCode", currentUser.getTenantCode());
        }
        return context;
    }
}
