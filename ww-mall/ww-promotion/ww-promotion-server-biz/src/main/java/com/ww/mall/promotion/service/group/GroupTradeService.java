package com.ww.mall.promotion.service.group;

import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.mq.GroupOrderPaidMessage;

/**
 * 拼团交易编排服务。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 负责消费支付成功消息并驱动正式开团/参团
 */
public interface GroupTradeService {

    /**
     * 处理支付成功消息。
     *
     * @param message 支付成功消息
     * @return 拼团详情
     */
    GroupInstanceVO handleOrderPaid(GroupOrderPaidMessage message);

}
