package com.ruijie.dapengine.common.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 锐捷统一鉴权配置。
 */
@Data
@ConfigurationProperties(prefix = "spring.ruijie.auth.sys")
public class RuijieAuthProperties {

    /** 系统注册后的 sysId。 */
    private String id;

    /** 系统注册后分配的 accessKeySecret。 */
    private String accessKeySecret;
}

