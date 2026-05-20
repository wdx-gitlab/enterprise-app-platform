package com.ruijie.dapengine.autoconfigure;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * DAP Engine 平台库 JdbcTemplate 包装类，绑定 DapEngineDataSource，供内部所有模块统一使用。
 * Bean 名为 dapEngineJdbcTemplate。
 */
public class DapEngineJdbcTemplate {

    private final JdbcTemplate jdbcTemplate;

    public DapEngineJdbcTemplate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}
