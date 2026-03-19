package com.ww.mall.promotion.controller.admin.group.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 拼团后台详情视图。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 后台客服查看拼团全貌与每个成员轨迹的聚合视图
 */
@Data
@Schema(description = "拼团后台详情视图")
public class GroupAdminDetailVO {

    @Schema(description = "拼团ID")
    private String groupId;

    @Schema(description = "活动ID")
    private String activityId;

    @Schema(description = "团状态")
    private String status;

    @Schema(description = "分享商品SPU ID")
    private Long spuId;

    @Schema(description = "团长用户ID")
    private Long leaderUserId;

    @Schema(description = "当前人数")
    private Integer currentSize;

    @Schema(description = "成团要求人数")
    private Integer requiredSize;

    @Schema(description = "剩余名额")
    private Integer remainingSlots;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "过期时间")
    private Date expireTime;

    @Schema(description = "完成时间")
    private Date completeTime;

    @Schema(description = "成员轨迹列表")
    private List<MemberTrajectoryVO> members;

    /**
     * 成员轨迹。
     */
    @Data
    @Schema(description = "拼团成员轨迹")
    public static class MemberTrajectoryVO {

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "订单ID")
        private String orderId;

        @Schema(description = "SKU ID")
        private Long skuId;

        @Schema(description = "是否团长")
        private Boolean leader;

        @Schema(description = "成员状态")
        private String memberStatus;

        @Schema(description = "加入时间")
        private Date joinTime;

        @Schema(description = "最近轨迹编码")
        private String latestTrajectory;

        @Schema(description = "最近轨迹时间")
        private Date latestTrajectoryTime;
    }
}
