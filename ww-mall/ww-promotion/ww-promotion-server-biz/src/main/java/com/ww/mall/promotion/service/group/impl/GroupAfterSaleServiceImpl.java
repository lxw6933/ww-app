package com.ww.mall.promotion.service.group.impl;

import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.dto.group.GroupAfterSaleRequestDTO;
import com.ww.mall.promotion.engine.GroupCommandService;
import com.ww.mall.promotion.service.group.GroupAfterSaleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ERROR;

/**
 * 拼团售后服务实现。
 *
 * @author ww
 * @create 2026-03-27
 * @description: 负责把外部拼团售后请求委托给命令服务执行，统一保持参数校验与异常语义
 */
@Service
public class GroupAfterSaleServiceImpl implements GroupAfterSaleService {

    @Resource
    private GroupCommandService groupCommandService;

    @Override
    public void handleAfterSale(GroupAfterSaleRequestDTO request) {
        if (request == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        groupCommandService.handleAfterSale(request);
    }
}
