package com.ww.app.cart.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Schema(description = "按商家分组的商品列表（按添加时间排序）")
    private Map<Long, List<CartItem>> cartItemMerchantMap;

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

        int countNum = 0;
        int checkedCount = 0;
        int invalidCount = 0;
        long totalAmount = 0L;

        for (CartItem item : cartItems) {
            if (item == null) {
                continue;
            }
            Integer count = item.getCount();
            if (count != null) {
                countNum += count;
            }
            if (item.isInvalid()) {
                invalidCount++;
                continue;
            }
            if (item.isChecked()) {
                checkedCount++;
                totalAmount += item.getTotalPrice();
            }
        }

        this.countNum = countNum;
        this.countType = cartItems.size();
        this.totalAmount = totalAmount;
        this.checkedCount = checkedCount;
        this.invalidCount = invalidCount;
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

    /**
     * 按商家分组，并按商品添加时间排序
     * 返回 Map<商家ID, 商品列表>，商品列表按添加时间升序排序
     *
     * @return Map<Long, List<CartItem>> 按商家分组的商品列表
     */
    public Map<Long, List<CartItem>> getCartItemMerchantMap() {
        if (cartItems == null || cartItems.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return cartItems.stream()
                .filter(item -> item.getMerchantId() != null)
                .sorted(Comparator.comparing(
                        CartItem::getAddTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.groupingBy(
                        CartItem::getMerchantId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}
