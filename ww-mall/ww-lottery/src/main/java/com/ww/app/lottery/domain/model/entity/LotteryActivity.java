package com.ww.app.lottery.domain.model.entity;

import com.ww.app.lottery.domain.result.LotteryResult;
import com.ww.app.lottery.enums.LotteryStrategyType;
import com.ww.app.lottery.infrastructure.exception.LotteryException;
import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author ww
 * @create 2025-06-09- 14:22
 * @description: 抽奖活动
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "lottery_activity")
public class LotteryActivity extends BaseDoc {

    /** 渠道id */
    private Long channelId;

    /** 活动编码 */
    private String activityCode;
    
    /** 活动名称 */
    private String activityName;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;
    
    /** 活动描述 */
    private String description;

    /** 活动状态 */
    private Boolean status;

    /** 抽奖策略类型 */
    private LotteryStrategyType lotteryStrategyType;

    /** 奖品列表配置 */
    private List<PrizeConfig> prizes;

    /** 奖品概率分布 */
    private List<ProbabilityRange> probabilityRanges;

    /** 活动规则 */
    private LotteryRuleConfig lotteryRuleConfig;

    /** 额外信息 */
    private Map<String, Object> extraInfo;

    public static Query buildActivityCodeQuery(String activityCode) {
        return new Query().addCriteria(Criteria.where("activityCode").is(activityCode));
    }

    public void validateActivity() {
        Date now = new Date();
        if (now.before(this.startTime)) {
            throw new LotteryException(LotteryResult.ResultCode.ACTIVITY_NOT_STARTED);
        }
        if (now.after(this.endTime)) {
            throw new LotteryException(LotteryResult.ResultCode.ACTIVITY_ENDED);
        }
        if (!this.status) {
            throw new LotteryException(LotteryResult.ResultCode.ACTIVITY_STATUS_ERROR);
        }
    }

    /**
     * 构建奖品轮盘概率区间
     *
     * @return 奖品轮盘概率区间
     */
    private List<ProbabilityRange> buildProbabilityRanges() {
        List<ProbabilityRange> probabilityRanges = new ArrayList<>();
        double currentStart = 0.0;

        for (PrizeConfig prize : this.prizes) {
            double end = currentStart + prize.getProbability();
            probabilityRanges.add(new ProbabilityRange(prize, currentStart, end));
            currentStart = end;
        }
        return probabilityRanges;
    }

    /**
     * 概率区间类
     */
    @Data
    public static class ProbabilityRange {
        PrizeConfig prize;
        double start;
        double end;

        ProbabilityRange(PrizeConfig prize, double start, double end) {
            this.prize = prize;
            this.start = start;
            this.end = end;
        }
    }

} 