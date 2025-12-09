package com.ww.mall.promotion.controller.app.group.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-12-08 17:20
 * @description: 拼团实例VO
 */
@Data
@Schema(description = "拼团实例VO")
public class GroupInstanceVO {

    @Schema(description = "拼团实例ID", example = "GROUP_1234567890_abc12345")
    private String id;

    @Schema(description = "活动ID", example = "ACT123456")
    private String activityId;

    @Schema(description = "团长用户ID", example = "10001")
    private Long leaderUserId;

    @Schema(description = "拼团状态：OPEN-进行中，SUCCESS-成功，FAILED-失败", example = "OPEN")
    private String status;

    @Schema(description = "需要人数", example = "2")
    private Integer requiredSize;

    @Schema(description = "当前人数", example = "1")
    private Integer currentSize;

    @Schema(description = "剩余名额", example = "1")
    private Integer remainingSlots;

    @Schema(description = "过期时间", example = "2025-12-09 12:00:00")
    private Date expireTime;

    @Schema(description = "完成时间", example = "2025-12-08 15:30:00")
    private Date completeTime;

    @Schema(description = "拼团价格", example = "99.00")
    private BigDecimal groupPrice;

    @Schema(description = "商品SPU ID", example = "1001")
    private Long spuId;

    @Schema(description = "商品SKU ID", example = "2001")
    private Long skuId;

    @Schema(description = "成员列表")
    private List<MemberInfo> members;

    @Data
    @Schema(description = "拼团成员信息")
    public static class MemberInfo {
        @Schema(description = "用户ID", example = "10001")
        private Long userId;
        
        @Schema(description = "订单ID", example = "ORDER20251208123456")
        private String orderId;
        
        @Schema(description = "加入时间", example = "2025-12-08 12:00:00")
        private Date joinTime;
        
        @Schema(description = "是否团长", example = "true")
        private Boolean isLeader;
    }

}
