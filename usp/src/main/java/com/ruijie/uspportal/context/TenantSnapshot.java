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
 * TenantSnapshot 类。
 */
public class TenantSnapshot {

    private Long tenantId;

    private String tenantCode;

    private String tenantName;

    private String tenantStatus;
}
