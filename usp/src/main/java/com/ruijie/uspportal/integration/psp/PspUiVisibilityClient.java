package com.ruijie.uspportal.integration.psp;

import com.ruijie.uspportal.security.CurrentUserContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PspUiVisibilityClient {

    public Map<String, Boolean> batchCheck(Collection<String> permissionCodes) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.get();
        boolean admin = currentUser != null && Boolean.TRUE.equals(currentUser.getAdmin());
        for (String permissionCode : permissionCodes) {
            boolean visible = admin || !StringUtils.hasText(permissionCode);
            result.put(permissionCode, visible);
        }
        return result;
    }
}
