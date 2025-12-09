package com.ww.mall.promotion.controller.app.group.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * @author ww
 * @create 2025-12-08 17:15
 * @description: 加入拼团请求
 */
@Data
@Schema(description = "加入拼团请求")
public class JoinGroupRequest {

    @Schema(description = "拼团实例ID", requiredMode = REQUIRED, example = "GROUP_1234567890_abc12345")
    @NotNull(message = "拼团实例ID不能为空")
    private String groupId;

    @Schema(description = "用户ID", requiredMode = REQUIRED, example = "10002")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "订单ID", requiredMode = REQUIRED, example = "ORDER20251208123457")
    @NotNull(message = "订单ID不能为空")
    private String orderId;

    @Schema(description = "订单信息（JSON字符串）", example = "{\"amount\":99.00,\"quantity\":1}")
    private String orderInfo;

}
