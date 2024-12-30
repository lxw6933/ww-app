package com.ww.app.third.edu.req;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2023-03-29- 11:52
 * @description:
 */
@Data
public class ConfirmOrderReqBO {

    @JSONField(ordinal = 5)
    private String cmd;

    @JSONField(ordinal = 4)
    @NotNull(message = "订单编码不能为空")
    private String productOrderNo;

    @JSONField(ordinal = 3)
    @NotNull(message = "商城系统的订单号不能为空")
    private String t2OrderNo;

    @JSONField(ordinal = 2)
    @NotNull(message = "产品唯⼀编码不能为空")
    private String mpn;

    @JSONField(ordinal = 1)
    @NotNull(message = "产品销售数量不能为空")
    private Integer mpnNum;

}
