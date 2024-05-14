package com.ww.mall.seckill.view.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2024-05-14- 15:51
 * @description:
 */
@Data
public class SecKillOrderReqBO {

    @NotBlank(message = "秒杀活动编码不能为空")
    private String activityCode;

    @NotNull(message = "秒杀商品id不能为空")
    private Long skuId;

}
