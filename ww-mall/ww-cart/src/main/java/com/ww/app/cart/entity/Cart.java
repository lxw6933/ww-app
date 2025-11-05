package com.ww.app.cart.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车实体
 *
 * @author ww
 * @date 2023-07-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车")
public class Cart {

    @Schema(description = "购物车明细")
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    @Schema(description = "商品总数量")
    private Integer countNum;

    @Schema(description = "商品类型数量")
    private Integer countType;

    @Schema(description = "总价（分）- 仅勾选商品")
    private Long totalAmount;

    @Schema(description = "选中商品数量")
    private Integer checkedCount;

    @Schema(description = "失效商品数量")
    private Integer invalidCount;

    /**
     * 重新计算购物车统计信息
     * 优化：使用并行流提升性能（当商品数量较多时）
     */
    public void recalcTotals() {
        if (cartItems == null || cartItems.isEmpty()) {
            this.countNum = 0;
            this.countType = 0;
            this.totalAmount = 0L;
            this.checkedCount = 0;
            this.invalidCount = 0;
            return;
        }

        if (cartItems.size() > 10) {
            this.countNum = cartItems.parallelStream()
                    .mapToInt(CartItem::getCount)
                    .sum();
            this.totalAmount = cartItems.parallelStream()
                    .filter(CartItem::isChecked)
                    .filter(item -> !item.isInvalid())
                    .mapToLong(CartItem::getTotalPrice)
                    .sum();
            this.checkedCount = (int) cartItems.parallelStream()
                    .filter(CartItem::isChecked)
                    .count();
            this.invalidCount = (int) cartItems.parallelStream()
                    .filter(CartItem::isInvalid)
                    .count();
        } else {
            this.countNum = cartItems.stream()
                    .mapToInt(CartItem::getCount)
                    .sum();
            this.totalAmount = cartItems.stream()
                    .filter(CartItem::isChecked)
                    .filter(item -> !item.isInvalid())
                    .mapToLong(CartItem::getTotalPrice)
                    .sum();
            this.checkedCount = (int) cartItems.stream()
                    .filter(CartItem::isChecked)
                    .count();
            this.invalidCount = (int) cartItems.stream()
                    .filter(CartItem::isInvalid)
                    .count();
        }

        this.countType = cartItems.size();
    }

    /**
     * 获取有效的购物车项
     */
    public List<CartItem> getValidItems() {
        if (cartItems == null) {
            return new ArrayList<>();
        }
        return cartItems.stream()
                .filter(item -> !item.isInvalid())
                .collect(Collectors.toList());
    }

    /**
     * 获取失效的购物车项
     */
    public List<CartItem> getInvalidItems() {
        if (cartItems == null) {
            return new ArrayList<>();
        }
        return cartItems.stream()
                .filter(CartItem::isInvalid)
                .collect(Collectors.toList());
    }

    /**
     * 获取选中的购物车项
     */
    public List<CartItem> getCheckedItems() {
        if (cartItems == null) {
            return new ArrayList<>();
        }
        return cartItems.stream()
                .filter(CartItem::isChecked)
                .filter(item -> !item.isInvalid())
                .collect(Collectors.toList());
    }
}
