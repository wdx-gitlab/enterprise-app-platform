package com.ruijie.dapengine.autoconfigure;

import com.alibaba.druid.pool.DruidDataSource;

import javax.sql.DataSource;

/**
 * DAP Engine 平台库数据源包装类，用于与业务库 DataSource Bean 区分，避免 Spring 自动注入冲突。
 * Bean 名为 dapEngineDataSource。
 */
public class DapEngineDataSource {

    private final DruidDataSource dataSource;

    public DapEngineDataSource(DruidDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    /** 关闭连接池（由 Spring 生命周期管理） */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
