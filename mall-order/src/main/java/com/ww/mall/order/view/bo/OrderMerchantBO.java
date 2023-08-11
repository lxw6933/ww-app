package com.ww.mall.order.view.bo;

import lombok.Data;

/**
 * @author ww
 * @create 2023-08-11- 14:13
 * @description:
 */
@Data
public class OrderMerchantBO {

    /**
     * 下单商家id
     */
    private Long merchantId;

    /**
     * 下单商家备注
     */
    private String remark;

    /**
     * 需要下单商家开票信息
     */
    private OrderInvoiceBO orderInvoiceBO;
}
