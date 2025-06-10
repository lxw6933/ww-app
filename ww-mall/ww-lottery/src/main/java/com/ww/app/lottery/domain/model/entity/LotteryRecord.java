package com.ww.app.lottery.domain.model.entity;

import com.ww.app.lottery.enums.PrizeType;
import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2025-06-09- 14:22
 * @description: 中奖记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "lottery_record")
public class LotteryRecord extends BaseDoc {

    /** 中奖用户id */
    private Long userId;

    /** 中奖活动编码 */
    private String activityCode;

    /** 中奖奖品id */
    private String lotteryId;

    /** 中奖奖品数量 */
    private int lotteryNumber;

    /** 奖品类型 */
    private PrizeType prizeType;

    /** 是否领取 */
    private Boolean receive;
}
