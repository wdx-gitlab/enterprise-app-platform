package com.ruijie.uspportal.auth.service.impl;

import com.ruijie.uspportal.auth.service.SsoAccountMappingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DefaultSsoAccountMappingService implements SsoAccountMappingService {

    @Override
    public String resolveLoginName(String rawLoginName) {
        return StringUtils.hasText(rawLoginName) ? rawLoginName.trim() : rawLoginName;
    }
}
