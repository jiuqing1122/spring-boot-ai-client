package com.example.aiclient.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final Integer code;

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
