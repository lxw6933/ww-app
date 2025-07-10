package com.ww.app.redis.component.stock.handler;

/**
 * @author ww
 * @create 2024-06-27- 09:23
 * @description:
 */
public interface IRedisStockHandler {

    /**
     * 回滚库存
     *
     * @param hashKey hashKey hashKey
     * @param number number number
     * @param type 回滚类型 0: 回滚lockStock
     */
    default void handleFailRollbackStock(String hashKey, int number, int type) {}

}
