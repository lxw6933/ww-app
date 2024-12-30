package com.ww.app.order.view.bo;

import com.ww.app.order.enums.RechargeAccountType;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2023-08-11- 13:57
 * @description:
 */
@Data
public class OrderActivityProductBO {

    /**
     * 商品编号
     */
    @NotNull(message = "购买商品编号不能为空")
    private String spuCode;

    /**
     * 规格编号
     */
    @NotNull(message = "购买商品规格编号不能为空")
    private Long skuCode;

    /**
     * 购买数量
     */
    @Max(value = 99, message = "最大购买数量不能超过99")
    @Min(value = 1, message = "最小购买不能低于1")
    @NotNull(message = "购买数量不能为空")
    private Integer number;

    /**
     * 充值号码
     */
    private String rechargeNo;

    /**
     * 充值账号类型
     */
    private RechargeAccountType rechargeAccountType;

}
