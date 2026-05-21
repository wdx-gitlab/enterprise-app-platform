package com.ruijie.authzengine.application.service;

import org.springframework.util.StringUtils;

final class FieldMaskScriptRules {

    static final String LEGACY_BROKEN_MIDDLE_MASK_SCRIPT = "#originalValue.replaceAll('(?<=^.{3}).(?=.{3}$)', '*')";

    private FieldMaskScriptRules() {
    }

    static boolean isLegacyBrokenScript(String script) {
        return StringUtils.hasText(script)
            && LEGACY_BROKEN_MIDDLE_MASK_SCRIPT.equals(script.trim());
    }
}