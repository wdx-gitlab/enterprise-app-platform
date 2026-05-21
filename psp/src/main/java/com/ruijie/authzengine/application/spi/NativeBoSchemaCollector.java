package com.ruijie.authzengine.application.spi;

import java.util.List;

/**
 * Native 模式 BO 元数据采集接口，由 infrastructure 层通过 JDBC {@code DatabaseMetaData} 实现。
 *
 * <p>适用于引擎与业务库处于同一数据源或可直连的场景。
 * 返回结果仅作为候选输入展示给管理员，<b>不能直接落库</b>，必须经过治理确认。
 *
 * <p>若表不存在或数据源不可用，可返回空列表，不得抛出非受检异常影响管理界面可用性。
 */
@FunctionalInterface
public interface NativeBoSchemaCollector {

    /**
     * 按物理表名采集表/列元数据。
     *
     * @param tableName 物理表名，不能为空
     * @return 结构化列信息列表；表不存在或采集失败时返回空列表
     */
    List<BoSchemaColumnInfo> fetchColumns(String tableName);
}
