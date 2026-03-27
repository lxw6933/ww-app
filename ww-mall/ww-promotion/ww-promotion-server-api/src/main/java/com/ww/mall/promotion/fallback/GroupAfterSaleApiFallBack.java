package com.ww.mall.promotion.fallback;

import com.ww.app.common.common.Result;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.mall.promotion.dto.group.GroupAfterSaleRequestDTO;
import com.ww.mall.promotion.rpc.GroupAfterSaleApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * 拼团售后 RPC 降级工厂。
 *
 * @author ww
 * @create 2026-03-27
 * @description: 当拼团服务不可用时，向调用方返回统一限流/降级结果，避免误判为售后已处理成功
 */
@Slf4j
public class GroupAfterSaleApiFallBack implements FallbackFactory<GroupAfterSaleApi> {

    @Override
    public GroupAfterSaleApi create(Throwable cause) {
        log.error("拼团服务【GroupAfterSaleApi】调用异常：{}", cause.getMessage());
        return request -> Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
    }
}
