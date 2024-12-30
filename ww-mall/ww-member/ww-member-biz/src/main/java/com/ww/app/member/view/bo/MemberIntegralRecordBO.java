package com.ww.app.member.view.bo;

import com.ww.app.member.enums.IntegralType;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 21:58
 **/
@Data
public class MemberIntegralRecordBO {

    /**
     * 会员id
     */
    @NotNull(message = "用户id不能为空")
    private Long memberId;

    /**
     * 渠道id
     */
    @NotNull(message = "channelId不能为空")
    private Long channelId;

    /**
     * 积分类型【新增、减少】
     */
    @NotNull(message = "积分类型不能为空")
    private IntegralType integralType;

    /**
     * 积分数量
     */
    @NotNull(message = "积分数量不能为空")
    private Integer integralNum;

    /**
     * 订单号
     */
    private String orderCode;

    /**
     * 支付流水
     */
    private String payJrn;

    /**
     * 是否已读
     */
    private Boolean read;

    /**
     * 创建时间
     */
    private String createTime;

}
