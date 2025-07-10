package com.ww.app.seckill.component;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.ww.app.redis.component.stock.handler.IRedisStockHandler;
import com.ww.app.seckill.entity.StockExceptionData;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author ww
 * @create 2024-07-09- 09:02
 * @description:
 */
@Component
public class CommonStockHandler implements IRedisStockHandler {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public void handleFailRollbackStock(String hashKey, int number, int type) {
        StockExceptionData stockData = new StockExceptionData();
        stockData.setRedisKey(hashKey);
        stockData.setNumber(number);
        stockData.setType(type);
        stockData.setErrorDate(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        stockData.setRetryDate("");
        stockData.setValid(true);
        mongoTemplate.save(stockData);
    }
}
