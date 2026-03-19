package com.ww.mall.promotion.service.group.impl;

import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.engine.GroupCommandService;
import com.ww.mall.promotion.mq.GroupAfterSaleSuccessMessage;
import com.ww.mall.promotion.mq.GroupOrderPaidMessage;
import com.ww.mall.promotion.service.group.GroupTradeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ERROR;

/**
 * 拼团交易编排服务实现。
 * <p>
 * 新版实现不再维护同步 trade 锚点，而是直接把支付成功消息翻译成 Redis Lua 命令。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 拼团交易编排服务实现
 */
@Service
public class GroupTradeServiceImpl implements GroupTradeService {

    @Resource
    private GroupCommandService groupCommandService;

    @Override
    public GroupInstanceVO handleOrderPaid(GroupOrderPaidMessage message) {
        if (message == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        return groupCommandService.handleOrderPaid(message);
    }

    @Override
    public void handleAfterSaleSuccess(GroupAfterSaleSuccessMessage message) {
        if (message == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        groupCommandService.handleAfterSaleSuccess(message);
    }
}
