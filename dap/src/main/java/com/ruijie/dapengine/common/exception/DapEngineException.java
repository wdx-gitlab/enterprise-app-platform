package com.ruijie.dapengine.common.exception;

/**
 * DAP Engine 异常基类
 */
public class DapEngineException extends RuntimeException {

    private final String code;

    public DapEngineException(String code, String message) {
        super(message);
        this.code = code;
    }

    public DapEngineException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
