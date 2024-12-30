package com.ww.app.order.view.bo;

import com.ww.app.order.enums.ActivityType;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2023-08-11- 13:55
 * @description:
 */
@Data
public class OrderProductBO {

    /**
     * 活动类型
     */
    @NotNull(message = "活动类型不能为空")
    private ActivityType activityType;

    /**
     * 活动编码
     */
    private String activityCode;

    /**
     * 活动子编码
     */
    private Long activitySubCode;

    /**
     * 活动商品集合
     */
    @Valid
    @NotEmpty(message = "下单活动商品不能为空")
    private List<OrderActivityProductBO> activityProductList;

    /**
     * 加价购活动换购商品集合
     */
    private List<OrderActivityProductBO> replaceProductList;
}
