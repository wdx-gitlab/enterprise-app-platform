package com.ruijie.uspportal.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = null;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
