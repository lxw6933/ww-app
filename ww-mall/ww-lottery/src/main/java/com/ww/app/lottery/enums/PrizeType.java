package com.ww.app.lottery.enums;

import lombok.Getter;

/**
 * @author ww
 * @create 2025-06-06- 14:26
 * @description:
 */
@Getter
public enum PrizeType {

    PHYSICAL("实物奖品", 1),
    VIRTUAL("虚拟奖品", 2),
    COUPON("优惠券", 3),
    POINTS("积分", 4),
    EMPTY("谢谢参与", 5);

    private final String description;
    private final int code;

    PrizeType(String description, int code) {
        this.description = description;
        this.code = code;
    }

    public static PrizeType fromCode(int code) {
        for (PrizeType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid PrizeType code: " + code);
    }

}
