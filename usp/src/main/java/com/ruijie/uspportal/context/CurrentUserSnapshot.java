package com.ruijie.uspportal.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
/**
 * CurrentUserSnapshot 类。
 */
public class CurrentUserSnapshot {

    private String userId;

    private String loginName;

    private String displayName;

    private String tenantCode;

    private String sessionId;

    private String authMode;

    private Boolean admin;
}
