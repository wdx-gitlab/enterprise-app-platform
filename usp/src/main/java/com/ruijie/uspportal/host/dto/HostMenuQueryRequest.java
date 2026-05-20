package com.ruijie.uspportal.host.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 宿主直接查询 Portal 菜单时使用的请求对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostMenuQueryRequest {

    private String tenantCode;

    private String appCode;

    @Builder.Default
    private Boolean publishedOnly = Boolean.FALSE;
}