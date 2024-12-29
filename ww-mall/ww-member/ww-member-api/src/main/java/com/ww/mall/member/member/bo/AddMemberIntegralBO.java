package com.ww.mall.member.member.bo;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 11:28
 **/
@Data
public class AddMemberIntegralBO {

    @NotNull(message = "用户id不能为null")
    private Long memberId;

    @NotNull(message = "积分数量不能为null")
    private Integer integralNum;

    @NotNull(message = "积分类型不能为null[0：扣减 1： 新增]")
    private Boolean integralType;

    /**
     * 订单号
     */
    private String orderCode;

    /**
     * 支付流水
     */
    private String payJrn;

}
