package com.ruijie.uspportal;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;

/**
 * USP Portal 后端启动入口。
 *
 * <p>负责启动门户后端应用，并装配 Feign 客户端与核心 MyBatis Mapper 扫描配置。</p>
 */
@SpringBootApplication
@EnableFeignClients
@MapperScan(basePackages = {
    "com.ruijie.uspportal.auth.mapper",
    "com.ruijie.uspportal.tenant.mapper",
    "com.ruijie.uspportal.appregistry.mapper",
    "com.ruijie.uspportal.navigation.mapper",
    "com.ruijie.uspportal.workbench.mapper",
    "com.ruijie.uspportal.portalconfig.mapper"
})
public class Application {

    /**
     * 启动 USP Portal 后端应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
