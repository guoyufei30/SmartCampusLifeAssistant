package com.smartcampuslifeserver.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final String subCode;

    public BusinessException(int code, String message) {
        this(code, message, null);
    }

    public BusinessException(int code, String message, String subCode) {
        super(message);
        this.code = code;
        this.subCode = subCode;
    }
}
