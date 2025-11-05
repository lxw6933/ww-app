package com.ww.app.cart.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ww.app.common.utils.MoneyUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车项实体
 *
 * @author ww
 * @date 2023-07-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车项")
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "SPU ID")
    private Long spuId;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "是否选中")
    @Builder.Default
    private boolean checked = true;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "商品图片")
    private String image;

    @Schema(description = "SKU属性")
    private List<String> skuAttr;

    @Schema(description = "单价（分）")
    private Long price;

    @Schema(description = "数量")
    @Builder.Default
    private Integer count = 1;

    @Schema(description = "是否失效")
    @Builder.Default
    private boolean invalid = false;

    @Schema(description = "加入时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime addTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 获取总价（分）
     * 添加溢出检查，防止价格或数量过大导致溢出
     */
    public Long getTotalPrice() {
        if (price == null || count == null) {
            return 0L;
        }
        try {
            return Math.multiplyExact(price, count);
        } catch (ArithmeticException e) {
            // 发生溢出时返回 Long.MAX_VALUE
            return Long.MAX_VALUE;
        }
    }

    /**
     * 获取单价（元）
     */
    public String getPriceYuan() {
        if (price == null) {
            return "0.00";
        }
        return MoneyUtils.fenToYuanStr(price);
    }

    /**
     * 获取总价（元）
     */
    public String getTotalPriceYuan() {
        Long totalPrice = getTotalPrice();
        return MoneyUtils.fenToYuanStr(totalPrice);
    }
}
