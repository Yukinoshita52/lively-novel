package com.livelynovel.common;

/**
 * 统一响应包装：{ code, data, message }。
 * 详见技术方案文档 §8.1。成功 code=0；失败 code 为业务错误码（§8.2）。
 *
 * @param <T> data 载荷类型
 */
public class Result<T> {

    private int code;
    private T data;
    private String message;

    public Result() {
    }

    public Result(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /** 成功响应。 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, data, "success");
    }

    /** 失败响应。 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, null, message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
