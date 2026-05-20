package com.ruijie.dapengine.common.exception;

/**
 * 参数或配置校验失败异常（如必填配置缺失、列名不合法等）
 */
public class DapValidationException extends DapEngineException {

    public DapValidationException(String message) {
        super("DAP_VALIDATION_ERROR", message);
    }

    public DapValidationException(String message, Throwable cause) {
        super("DAP_VALIDATION_ERROR", message, cause);
    }
}
