package com.ww.mall.redis.vo;

import lombok.Data;

/**
 * @author ww
 * @create 2024-06-25- 18:23
 * @description:
 */
@Data
public class ActivityStockVO {

    private Integer totalStock;

    private Integer lockStock;

    private Integer useStock;

}
