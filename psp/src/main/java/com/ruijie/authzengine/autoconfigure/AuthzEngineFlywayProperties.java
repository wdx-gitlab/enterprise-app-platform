package com.ruijie.authzengine.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * authz-engine 专属 Flyway 配置。
 */
@ConfigurationProperties(prefix = "authz.engine.flyway")
public class AuthzEngineFlywayProperties {

    private boolean enabled = true;

    private List<String> locations = new ArrayList<>(Collections.singletonList("classpath:db/authz-migration"));

    private String table = "authz_flyway_history";

    private boolean baselineOnMigrate = false;

    private String baselineVersion = "1";

    /**
     * 是否启用 authz-engine 专属 Flyway。
     *
     * @return true 表示启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置 Flyway 开关。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取迁移脚本位置。
     *
     * @return 迁移脚本位置列表
     */
    public List<String> getLocations() {
        return locations;
    }

    /**
     * 设置迁移脚本位置。
     *
     * @param locations 脚本位置列表
     */
    public void setLocations(List<String> locations) {
        this.locations = locations == null ? new ArrayList<>() : new ArrayList<>(locations);
    }

    /**
     * 获取 Flyway 版本表名。
     *
     * @return 版本表名
     */
    public String getTable() {
        return table;
    }

    /**
     * 设置 Flyway 版本表名。
     *
     * @param table 版本表名
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * 获取是否在迁移时自动创建基线。
     *
     * @return true 表示启用 baselineOnMigrate
     */
    public boolean isBaselineOnMigrate() {
        return baselineOnMigrate;
    }

    /**
     * 设置是否在迁移时自动创建基线。
     *
     * @param baselineOnMigrate 是否启用
     */
    public void setBaselineOnMigrate(boolean baselineOnMigrate) {
        this.baselineOnMigrate = baselineOnMigrate;
    }

    /**
     * 获取 baselineVersion，与 baselineOnMigrate 配合使用。
     *
     * @return 基线版本号
     */
    public String getBaselineVersion() {
        return baselineVersion;
    }

    /**
     * 设置 baselineVersion。
     *
     * @param baselineVersion 基线版本号
     */
    public void setBaselineVersion(String baselineVersion) {
        this.baselineVersion = baselineVersion;
    }
}