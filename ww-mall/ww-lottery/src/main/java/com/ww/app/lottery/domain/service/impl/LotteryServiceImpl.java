package com.ww.app.lottery.domain.service.impl;

import cn.hutool.core.lang.Assert;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.lottery.domain.context.LotteryContext;
import com.ww.app.lottery.domain.model.entity.LotteryActivity;
import com.ww.app.lottery.domain.model.entity.LotteryRecord;
import com.ww.app.lottery.domain.result.LotteryResult;
import com.ww.app.lottery.domain.service.LotteryService;
import com.ww.app.lottery.domain.strategy.LotteryStrategy;
import com.ww.app.lottery.domain.strategy.LotteryStrategyFactory;
import com.ww.app.lottery.infrastructure.component.key.LotteryRedisKeyBuilder;
import com.ww.app.lottery.infrastructure.exception.LotteryException;
import com.ww.app.lottery.utils.LotteryCacheUtils;
import com.ww.app.redis.annotation.DistributedLock;
import com.ww.app.redis.component.StockRedisComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author ww
 * @create 2025-06-06- 18:07
 * @description:
 */
@Slf4j
@Service
public class LotteryServiceImpl implements LotteryService {

    @Resource
    private LotteryStrategyFactory lotteryStrategyFactory;

    @Resource
    private LotteryRedisKeyBuilder lotteryRedisKeyBuilder;

    @Resource
    private StockRedisComponent stockRedisComponent;

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public LotteryActivity getLotteryActivity(String activityCode) {
        return LotteryCacheUtils.getLotteryActivityCache(activityCode);
    }

    @Override
    @DistributedLock(enableUserLock = true)
    public LotteryResult doLottery(LotteryContext context) {
        try {
            LotteryActivity lotteryActivity = getLotteryActivity(context.getActivityCode());
            Assert.notNull(lotteryActivity, () -> new LotteryException(LotteryResult.ResultCode.ACTIVITY_NOT_FOUND));
            // 活动校验
            lotteryActivity.validateActivity();
            context.buildParam(LotteryContext.ACTIVITY_KEY, lotteryActivity);
            // TODO 用户抽奖次数校验

            // 获取抽奖活动策略
            LotteryStrategy lotteryStrategy = lotteryStrategyFactory.getLotteryStrategy(lotteryActivity.getLotteryStrategyType());
            // 抽奖
            LotteryResult result = lotteryStrategy.draw(context);
            // 抽奖结果处理
            processLotteryResult(result, context);
            return result;
        } catch (LotteryException e) {
            return buildFailedResult(e.getMessage(), LotteryResult.ResultCode.FAIL);
        } catch (Exception e) {
            log.error("抽奖系统异常", e);
            return buildFailedResult(GlobalResCodeConstants.SYSTEM_ERROR.getMsg(), LotteryResult.ResultCode.SYSTEM_ERROR);
        }
    }

    private void processLotteryResult(LotteryResult result, LotteryContext context) {
        // TODO 记录抽奖次数

        if (result.isSuccess()) {
            // 扣减库存
            String prizeStockKey = lotteryRedisKeyBuilder.buildLotteryStockPrefixKey(result.getPrizeId());
            boolean stockSuccess = stockRedisComponent.decrementStock(prizeStockKey, 1);
            Assert.isTrue(stockSuccess, () -> new LotteryException(LotteryResult.ResultCode.NO_INVENTORY));
            // 记录中奖信息
            LotteryRecord lotteryRecord = convertToRecord(result, context);
            mongoTemplate.save(lotteryRecord);
        }
        // TODO 异步处理结果

    }

    private LotteryRecord convertToRecord(LotteryResult result, LotteryContext context) {
        LotteryRecord lotteryRecord = new LotteryRecord();
        lotteryRecord.setUserId(context.getUserId());
        lotteryRecord.setActivityCode(context.getActivityCode());
        lotteryRecord.setLotteryId(result.getPrizeId());
        lotteryRecord.setLotteryNumber(result.getPrizeAmount());
        lotteryRecord.setPrizeType(result.getPrizeType());
        lotteryRecord.setReceive(false);
        return lotteryRecord;
    }

    private LotteryResult buildFailedResult(String message, LotteryResult.ResultCode code) {
        LotteryResult result = new LotteryResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setResultCode(code);
        result.setDrawTime(new Date());
        return result;
    }

}
