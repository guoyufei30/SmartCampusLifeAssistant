package com.smartcampuslifeserver.common.result;

import lombok.Data;

@Data
public class Result<T> {

    private int code;
    private String subCode;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return success(data, "操作成功");
    }

    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(int code, String message) {
        return error(code, message, null);
    }

    public static <T> Result<T> error(int code, String message, String subCode) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setSubCode(subCode);
        return result;
    }

    public static <T> Result<T> of(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}
