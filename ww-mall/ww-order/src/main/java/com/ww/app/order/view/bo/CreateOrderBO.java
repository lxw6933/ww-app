package com.ww.app.order.view.bo;

import com.ww.app.order.enums.OrderSourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author ww
 * @create 2023-08-11- 14:05
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreateOrderBO extends CommonOrderBO {

    /**
     * 是否为购物车下单
     */
    private Boolean shopCart;

    /**
     * 来源id
     */
    private String sourceId;

    /**
     * 来源类型
     */
    private OrderSourceType sourceType;

    /**
     * 下单商家信息
     */
    private List<OrderMerchantBO> merchantInfoList;

}
