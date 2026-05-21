package com.ruijie.authzengine.shared.exception;

import lombok.Getter;

/**
 * 非业务系统异常。
 */
@Getter
public class SystemException extends RuntimeException {

    private final String code;

    public SystemException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public SystemException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }
}