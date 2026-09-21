package com.fixedasset.common.model;

/**
 * 全局统一响应结构。
 *
 * <p>前端 Axios 拦截器依赖 {@code code == 0} 判断请求成功，因此新增接口时应始终
 * 通过该对象包装返回值，避免直接返回 Entity 导致响应协议不一致。</p>
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "success", null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
