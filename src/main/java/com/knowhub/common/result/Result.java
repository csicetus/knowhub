package com.knowhub.common.result;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功，带数据返回
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    // 成功，不带数据
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    // 失败，带自定义 code 和 message
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
