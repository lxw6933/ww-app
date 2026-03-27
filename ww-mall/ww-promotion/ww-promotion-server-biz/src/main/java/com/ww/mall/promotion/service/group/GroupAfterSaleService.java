package com.ww.mall.promotion.service.group;

import com.ww.mall.promotion.dto.group.GroupAfterSaleRequestDTO;

/**
 * 拼团售后服务。
 *
 * @author ww
 * @create 2026-03-27
 * @description: 统一承接订单域发起的拼团售后处理，内部根据场景决定是否执行拼团售后脚本以及是否发送退款消息
 */
public interface GroupAfterSaleService {

    /**
     * 处理拼团售后请求。
     *
     * @param request 拼团售后请求
     */
    void handleAfterSale(GroupAfterSaleRequestDTO request);
}
