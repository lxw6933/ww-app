package com.ww.app.disruptor.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 处理结果 - 封装事件处理的结果
 *
 * @author ww-framework
 */
@Data
public class ProcessResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 异常信息
     */
    private Throwable throwable;

    /**
     * 处理耗时（毫秒）
     */
    private long processDuration;

    /**
     * 结果数据
     */
    private Object data;

    public ProcessResult() {
    }

    public ProcessResult(boolean success) {
        this.success = success;
    }

    public ProcessResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * 创建成功结果
     */
    public static ProcessResult success() {
        return new ProcessResult(true, "处理成功");
    }

    /**
     * 创建成功结果（带消息）
     */
    public static ProcessResult success(String message) {
        return new ProcessResult(true, message);
    }

    /**
     * 创建成功结果（带数据）
     */
    public static ProcessResult success(String message, Object data) {
        ProcessResult result = new ProcessResult(true, message);
        result.setData(data);
        return result;
    }

    /**
     * 创建失败结果
     */
    public static ProcessResult failure(String message) {
        return new ProcessResult(false, message);
    }

    /**
     * 创建失败结果（带异常）
     */
    public static ProcessResult failure(String message, Throwable throwable) {
        ProcessResult result = new ProcessResult(false, message);
        result.setThrowable(throwable);
        return result;
    }

    /**
     * 创建失败结果（带错误代码）
     */
    public static ProcessResult failure(String errorCode, String message, Throwable throwable) {
        ProcessResult result = new ProcessResult(false, message);
        result.setErrorCode(errorCode);
        result.setThrowable(throwable);
        return result;
    }

    // Getters and Setters

    @Override
    public String toString() {
        return "ProcessResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", processDuration=" + processDuration +
                '}';
    }
}
