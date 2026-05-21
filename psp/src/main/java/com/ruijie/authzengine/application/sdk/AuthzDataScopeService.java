package com.ruijie.authzengine.application.sdk;

import com.ruijie.authzengine.application.sdk.model.DataScopeResolveCommand;
import com.ruijie.authzengine.application.sdk.model.DataScopeResult;

/**
 * 面向业务系统的数据范围服务。
 */
public interface AuthzDataScopeService {

    /**
     * 解析数据范围合同或首版 SQL 片段。
     *
     * @param command data-scope 查询命令
     * @return 数据范围结果
     */
    DataScopeResult resolveDataScope(DataScopeResolveCommand command);
}