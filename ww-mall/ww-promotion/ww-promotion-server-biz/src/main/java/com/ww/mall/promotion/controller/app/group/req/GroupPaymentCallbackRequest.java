package com.ww.mall.promotion.controller.app.group.req;

import com.ww.mall.promotion.enums.GroupTradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.ORDER_ID_REQUIRED;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.PAY_TRANS_ID_REQUIRED;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.TRADE_TYPE_REQUIRED;
import static com.ww.mall.promotion.constants.GroupValidationMessageConstants.USER_ID_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 拼团支付回调请求。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 支付服务回调拼团服务时使用，回调成功后才正式开团或参团
 */
@Data
@Schema(description = "拼团支付回调请求")
public class GroupPaymentCallbackRequest {

    @Schema(description = "业务类型：START-开团，JOIN-参团", requiredMode = REQUIRED, example = "START")
    @NotNull(message = TRADE_TYPE_REQUIRED)
    private GroupTradeType tradeType;

    @Schema(description = "活动ID，开团回调必填", example = "ACT123456")
    private String activityId;

    @Schema(description = "拼团ID，参团回调必填", example = "67d79c3f31e8cb1aa8b2f111")
    private String groupId;

    @Schema(description = "用户ID", requiredMode = REQUIRED, example = "10001")
    @NotNull(message = USER_ID_REQUIRED)
    private Long userId;

    @Schema(description = "业务订单ID", requiredMode = REQUIRED, example = "ORDER202603170001")
    @NotNull(message = ORDER_ID_REQUIRED)
    private String orderId;

    @Schema(description = "支付流水ID", requiredMode = REQUIRED, example = "PAY202603170001")
    @NotNull(message = PAY_TRANS_ID_REQUIRED)
    private String payTransId;

    @Schema(description = "订单信息快照（JSON字符串）", example = "{\"amount\":99.00,\"quantity\":1}")
    private String orderInfo;

    @Schema(description = "链路追踪ID，未传时由服务端生成", example = "b9b75aef7d51471ca3834e77f7fa4c4c")
    private String traceId;
}
