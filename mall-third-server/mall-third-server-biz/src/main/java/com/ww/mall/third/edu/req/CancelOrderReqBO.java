package com.ww.mall.third.edu.req;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2023-03-28- 17:51
 * @description:
 */
@Data
public class CancelOrderReqBO {

    @JSONField(ordinal = 1)
    @NotNull(message = "edu订单编码不能为空")
    private String productOrderNo;

    @JSONField(ordinal = 2)
    private String cmd;

}
