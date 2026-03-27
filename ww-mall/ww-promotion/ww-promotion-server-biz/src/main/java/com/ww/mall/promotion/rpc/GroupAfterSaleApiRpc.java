package com.ww.mall.promotion.rpc;

import com.ww.app.common.common.Result;
import com.ww.mall.promotion.dto.group.GroupAfterSaleRequestDTO;
import com.ww.mall.promotion.service.group.GroupAfterSaleService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 拼团售后 RPC 实现。
 *
 * @author ww
 * @create 2026-03-27
 * @description: 对订单域开放统一拼团售后入口，屏蔽 MQ 消费改造细节并保持远程调用语义稳定
 */
@RestController
@RequestMapping(GroupAfterSaleApi.PREFIX)
public class GroupAfterSaleApiRpc implements GroupAfterSaleApi {

    @Resource
    private GroupAfterSaleService groupAfterSaleService;

    @Override
    @PostMapping("/handle")
    public Result<Boolean> handleAfterSale(@RequestBody GroupAfterSaleRequestDTO request) {
        groupAfterSaleService.handleAfterSale(request);
        return Result.success(Boolean.TRUE);
    }
}
