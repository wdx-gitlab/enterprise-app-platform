package com.ruijie.authzengine.shared.exception;

/**
 * PIP 或外部 Hook 类依赖异常。
 */
public class AuthzIntegrationException extends RuntimeException {

    public AuthzIntegrationException(String message) {
        super(message);
    }

    public AuthzIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}