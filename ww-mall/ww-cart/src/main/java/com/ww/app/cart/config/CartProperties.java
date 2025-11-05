package com.ww.app.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 购物车配置属性
 *
 * @author ww
 * @date 2025-11-05
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cart")
public class CartProperties {

    /**
     * 购物车最大商品种类数
     */
    private Integer maxCartNumber = 99;

    /**
     * 单次添加最大数量
     */
    private Integer maxAddNumber = 100;

    /**
     * 购物车过期天数
     */
    private Long ttlDays = 60L;

    /**
     * 是否启用库存校验
     */
    private Boolean enableStockCheck = false;

    /**
     * 是否启用价格校验
     */
    private Boolean enablePriceCheck = false;

    /**
     * Redis Key 过期时间刷新阈值（秒）
     * 当剩余过期时间小于此值时才重置过期时间
     * 默认 30 天（2592000 秒）
     */
    private Long expireRefreshThreshold = 2592000L;
}
