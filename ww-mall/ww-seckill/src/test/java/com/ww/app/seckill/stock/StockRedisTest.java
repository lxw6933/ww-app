package com.ww.app.seckill.stock;

import cn.hutool.core.lang.Assert;
import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.component.StockRedisComponent;
import com.ww.app.redis.key.StockRedisKeyBuilder;
import com.ww.app.redis.vo.ActivityStockVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-12-21 11:36
 * @description:
 */
@Slf4j
@SpringBootTest
public class StockRedisTest {

    @Resource
    private StockRedisComponent stockRedisComponent;

    @Resource
    private StockRedisKeyBuilder stockRedisKeyBuilder;

    @Test
    public void testStrRedisStock() {
        String strStockKey = stockRedisKeyBuilder.buildStockKey(null, null, "SPU000001", 1L);
        stockRedisComponent.initStrStock(strStockKey, 10);
        int strStock = stockRedisComponent.getStrStock(strStockKey);
        log.info("strStockKey [{}] 可用库存 [{}]", strStockKey, strStock);
        int decrementStock = 1;
        Assert.isTrue(stockRedisComponent.decrementStock(strStockKey, decrementStock), () -> new ApiException("库存不足"));
        strStock = stockRedisComponent.getStrStock(strStockKey);
        log.info("strStockKey [{}] 扣减库存 [{}] 后可用库存 [{}]", strStockKey, decrementStock, strStock);
        decrementStock = 10;
        Assert.isTrue(stockRedisComponent.decrementStock(strStockKey, decrementStock), () -> new ApiException("库存不足"));
        strStock = stockRedisComponent.getStrStock(strStockKey);
        log.info("strStockKey [{}] 扣减库存 [{}] 后可用库存 [{}]", strStockKey, decrementStock, strStock);
    }

    @Test
    public void testHashRedisStock() {
        String hashStockKey = stockRedisKeyBuilder.buildStockKey("XSG000001", null, "SPU000001", 1L);
        stockRedisComponent.initHashStock(hashStockKey, 10);
        ActivityStockVO hashStock = stockRedisComponent.getHashStock(hashStockKey);
        log.info("hashStockKey [{}] 可用库存 [{}]", hashStockKey, hashStock);
        ActivityStockVO errorStock = stockRedisComponent.getHashStock("errorKey");
        log.info("errorStockKey [{}] 可用库存 [{}]", "errorKey", errorStock);
        Assert.isTrue(stockRedisComponent.lockHashStock(hashStockKey, 1), () -> new ApiException("库存不足"));
        hashStock = stockRedisComponent.getHashStock(hashStockKey);
        log.info("hashStockKey [{}] 锁定库存后可用库存 [{}]", hashStockKey, hashStock);
        Assert.isTrue(stockRedisComponent.useHashStock(hashStockKey, 1), () -> new ApiException("库存不足"));
        hashStock = stockRedisComponent.getHashStock(hashStockKey);
        log.info("hashStockKey [{}] 使用库存后可用库存 [{}]", hashStockKey, hashStock);
        Assert.isTrue(stockRedisComponent.rollbackHashAfterStock(hashStockKey, 1), () -> new ApiException("库存不足"));
        hashStock = stockRedisComponent.getHashStock(hashStockKey);
        log.info("hashStockKey [{}] 售后回滚库存后可用库存 [{}]", hashStockKey, hashStock);
    }

}
