package com.ww.mall.mongodb.repository;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2024-07-01- 10:40
 * @description: 库存回滚异常数据
 */
@Data
@Document(collection = "v2_stock_exception_data")
public class StockExceptionData {

    @Id
    private String id;

    private String redisKey;

    private Integer number;

    /**
     * 0：回滚锁定库存
     */
    private Integer type;

    /**
     * 异常日期
     */
    private String errorDate;

    /**
     * 重试日期
     */
    private String retryDate;

    /**
     * 0：重试成功
     * 1：异常，待重试
     */
    private Boolean valid;

}
