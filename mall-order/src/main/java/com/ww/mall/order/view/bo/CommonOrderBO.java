package com.ww.mall.order.view.bo;

import com.ww.mall.order.enums.PayType;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2023-08-11- 14:00
 * @description:
 */
@Data
public class CommonOrderBO {

    /**
     * 用户收货地址id
     */
    private Long userAddressId;

    /**
     * 支付类型不能为空
     */
    @NotNull(message = "支付类型不能为空")
    private PayType payType;

    /**
     * 分期数
     */
    private Integer stagesNum;

    /**
     * 是否使用积分抵扣
     */
    @NotNull(message = "是否使用积分不能为空")
    private Boolean useIntegral;

    /**
     * 用户自定义积分抵扣多少金额
     */
    private Integer userUseIntegralDiscountPrice;

    /**
     * 用户使用平台优惠券code
     */
    private String platformCouponCode;

    /**
     * 用户使用商家优惠券code
     */
    private List<String> userMerchantCouponCodeList;

    /**
     * 下单商品集合
     */
    @Valid
    @NotEmpty(message = "下单必须选择商品")
    private List<OrderProductBO> productList;

    /**
     * 用户购买的权益code
     */
    private String equityMemberCode;

}
