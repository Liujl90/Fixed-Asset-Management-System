package com.fixedasset.common.exception;

/**
 * 可预期的业务异常。
 *
 * <p>用于表达状态冲突、引用约束、重复编码等用户可修正的错误。全局异常处理器会
 * 将其转换为 400 响应，不应使用它包装数据库连接失败等系统异常。</p>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(4000, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
