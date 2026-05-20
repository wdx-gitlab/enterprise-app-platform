package com.ruijie.dapengine.common.exception;

/**
 * 同步执行失败异常（如数据源连接失败、写入异常等）
 */
public class DapSyncException extends DapEngineException {

    public DapSyncException(String message) {
        super("DAP_SYNC_ERROR", message);
    }

    public DapSyncException(String message, Throwable cause) {
        super("DAP_SYNC_ERROR", message, cause);
    }
}
