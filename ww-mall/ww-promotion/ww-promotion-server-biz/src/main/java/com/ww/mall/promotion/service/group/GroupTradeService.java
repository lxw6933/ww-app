package com.ww.mall.promotion.service.group;

import com.ww.mall.promotion.controller.app.group.req.GroupPaymentCallbackRequest;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;

/**
 * 拼团交易编排服务。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 负责承接支付回调并驱动正式开团/参团
 */
public interface GroupTradeService {

    /**
     * 处理支付成功回调。
     *
     * @param request 支付回调请求
     * @return 拼团详情
     */
    GroupInstanceVO handlePaymentCallback(GroupPaymentCallbackRequest request);
}
