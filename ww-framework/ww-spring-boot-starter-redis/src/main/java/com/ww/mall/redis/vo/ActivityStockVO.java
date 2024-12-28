package com.ww.mall.redis.vo;

import lombok.Data;

/**
 * @author ww
 * @create 2024-06-25- 18:23
 * @description:
 */
@Data
public class ActivityStockVO {

    private int totalStock;

    private int lockStock;

    private int useStock;

}
