package com.ww.app.lottery.domain.result;

import com.ww.app.lottery.enums.PrizeType;
import lombok.Data;
import lombok.Getter;

import java.util.Date;

/**
 * @author ww
 * @create 2025-06-06- 14:29
 * @description:
 */
@Data
public class LotteryResult {

    private boolean success;        // 是否抽中
    private String prizeId;         // 奖品ID
    private String prizeName;       // 奖品名称
    private PrizeType prizeType;    // 奖品类型(使用枚举)
    private int prizeAmount;        // 奖品数量
    private String message;         // 结果消息
    private Date drawTime;          // 抽奖时间
    private ResultCode resultCode;  // 结果编码(使用枚举)

    @Getter
    public enum ResultCode {
        SUCCESS(200, "抽奖成功"),
        FAIL(400, "抽奖失败"),
        NO_INVENTORY(401, "库存不足"),
        ACTIVITY_NOT_STARTED(402, "活动未开始"),
        ACTIVITY_ENDED(403, "活动已结束"),
        ACTIVITY_NOT_FOUND(404, "活动不存在"),
        ACTIVITY_STATUS_ERROR(404, "活动已下架"),
        LIMIT_EXCEEDED(404, "抽奖次数超限"),
        SYSTEM_ERROR(500, "系统错误");

        private final int code;
        private final String message;

        ResultCode(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

}
