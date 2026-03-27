package com.ww.mall.promotion.rpc;

import com.ww.app.common.common.Result;
import com.ww.mall.promotion.constants.ApiConstants;
import com.ww.mall.promotion.dto.group.GroupAfterSaleRequestDTO;
import com.ww.mall.promotion.fallback.GroupAfterSaleApiFallBack;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 拼团售后 RPC 接口。
 *
 * @author ww
 * @create 2026-03-27
 * @description: 对订单域暴露统一的拼团售后入口，兼容 OPEN 售后释放名额与支付后入团异常退款两类业务
 */
@Tag(name = "RPC 服务 - 拼团售后")
@FeignClient(value = ApiConstants.NAME, fallbackFactory = GroupAfterSaleApiFallBack.class)
public interface GroupAfterSaleApi {

    /**
     * 拼团售后 RPC 路径前缀。
     */
    String PREFIX = ApiConstants.PREFIX + "/after-sale";

    /**
     * 统一处理拼团售后。
     *
     * @param request 售后请求
     * @return true-受理成功
     */
    @PostMapping(PREFIX + "/handle")
    @Operation(summary = "处理拼团售后")
    Result<Boolean> handleAfterSale(@RequestBody GroupAfterSaleRequestDTO request);
}
