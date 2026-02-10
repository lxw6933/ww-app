package com.ww.app.redis.component.stock.entity;

import lombok.Getter;

/**
 * 库存操作结果码。
 */
@Getter
public enum StockResultCode {
    /**
     * 操作成功。
     */
    SUCCESS("success"),

    /**
     * 参数非法。
     */
    INVALID_ARGUMENT("invalid argument"),

    /**
     * 库存 key/hash 不存在或字段缺失。
     */
    STOCK_NOT_FOUND("stock not found"),

    /**
     * 库存不足。
     */
    INSUFFICIENT_STOCK("insufficient stock"),

    /**
     * Redis 执行失败或结果异常。
     */
    EXECUTION_ERROR("execution error");

    /**
     * 结果码默认消息。
     * -- GETTER --
     *  获取结果码默认消息。
     */
    private final String message;

    StockResultCode(String message) {
        this.message = message;
    }

}
