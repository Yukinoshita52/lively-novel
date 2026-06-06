package com.livelynovel.common.exception;

/**
 * 小说上传校验异常，携带业务错误码。
 */
public class NovelValidationException extends RuntimeException {

    private final int code;

    public NovelValidationException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
