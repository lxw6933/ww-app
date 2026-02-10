package com.ww.app.redis.component.stock.entity;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 库存操作结果包装。
 */
@Getter
public final class StockResult {

    /**
     * 是否成功。
     */
    private final boolean success;

    /**
     * 结果码。
     */
    private final StockResultCode code;

    /**
     * Lua 脚本原始返回值。
     */
    private final Long scriptResult;

    /**
     * 诊断消息。
     */
    private final String message;

    private StockResult(boolean success, StockResultCode code, Long scriptResult, String message) {
        this.success = success;
        this.code = code;
        this.scriptResult = scriptResult;
        this.message = message;
    }

    /**
     * 创建成功结果。
     *
     * @param scriptResult Lua 脚本返回值
     * @return 成功结果
     */
    public static StockResult success(Long scriptResult) {
        return new StockResult(true, StockResultCode.SUCCESS, scriptResult, StockResultCode.SUCCESS.getMessage());
    }

    /**
     * 创建失败结果。
     *
     * @param code 失败码
     * @param message 失败消息
     * @return 失败结果
     */
    public static StockResult failure(StockResultCode code, String message) {
        return new StockResult(false, code, null, defaultMessage(code, message));
    }

    /**
     * 创建失败结果（含脚本返回值）。
     *
     * @param code 失败码
     * @param scriptResult Lua 脚本返回值
     * @param message 失败消息
     * @return 失败结果
     */
    public static StockResult failure(StockResultCode code, Long scriptResult, String message) {
        return new StockResult(false, code, scriptResult, defaultMessage(code, message));
    }

    /**
     * 当 message 为空时使用默认消息。
     *
     * @param code 结果码
     * @param message 输入消息
     * @return 解析后的消息
     */
    private static String defaultMessage(StockResultCode code, String message) {
        if (StrUtil.isBlank(message)) {
            return code.getMessage();
        }
        return message;
    }
}
