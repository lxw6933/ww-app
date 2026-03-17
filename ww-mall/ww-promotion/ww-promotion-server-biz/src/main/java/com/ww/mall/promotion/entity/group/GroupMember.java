package com.ww.mall.promotion.entity.group;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-12-08 16:50
 * @description: 拼团成员记录
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document("group_member")
@CompoundIndexes({
        @CompoundIndex(name = "idx_group_member_group_user", def = "{'groupInstanceId': 1, 'userId': 1}", unique = true),
        @CompoundIndex(name = "idx_group_member_group_join_time", def = "{'groupInstanceId': 1, 'joinTime': 1}"),
        @CompoundIndex(name = "idx_group_member_user_activity", def = "{'userId': 1, 'activityId': 1, 'memberStatus': 1}")
})
public class GroupMember extends BaseDoc {

    /**
     * 拼团实例ID
     */
    private String groupInstanceId;

    /**
     * 活动ID
     */
    private String activityId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 是否团长：1-是，0-否
     */
    private Integer isLeader;

    /**
     * 加入时间
     */
    private Date joinTime;

    /**
     * 拼团价格
     */
    private BigDecimal groupPrice;

    /**
     * 商品SPU ID
     */
    private Long spuId;

    /**
     * 商品SKU ID
     */
    private Long skuId;

    /**
     * 兼容字段：1-正常，0-已退出。
     */
    private Integer status;

    /**
     * 成员生命周期状态。
     */
    private String memberStatus;

    /**
     * 售后单号。
     */
    private String afterSaleId;

    /**
     * 售后成功时间。
     */
    private Date afterSaleTime;

    /**
     * 名额归还时间。
     */
    private Date releaseTime;

    /**
     * 成团时间。
     */
    private Date successTime;

    /**
     * 最近一次轨迹编码。
     */
    private String latestTrajectory;

    /**
     * 最近一次轨迹时间。
     */
    private Date latestTrajectoryTime;

    /**
     * 成员轨迹。
     */
    private List<TrajectoryNode> trajectories;

    /**
     * 轨迹节点。
     */
    @Data
    public static class TrajectoryNode {

        /**
         * 轨迹编码。
         */
        private String code;

        /**
         * 轨迹说明。
         */
        private String description;

        /**
         * 轨迹来源。
         */
        private String source;

        /**
         * 轨迹时间。
         */
        private Date eventTime;

        /**
         * 备注。
         */
        private String remark;
    }

    /**
     * 构建根据拼团实例ID和状态查询
     */
    public static Query buildGroupInstanceIdAndStatusQuery(String groupInstanceId, Integer status) {
        return new Query().addCriteria(
                Criteria.where("groupInstanceId").is(groupInstanceId)
                        .and("status").is(status)
        );
    }

    /**
     * 构建根据用户ID和状态查询
     */
    public static Query buildUserIdAndStatusQuery(Long userId, Integer status) {
        return new Query().addCriteria(
                Criteria.where("userId").is(userId)
                        .and("status").is(status)
        );
    }

    /**
     * 构建根据拼团实例ID和用户ID查询
     */
    public static Query buildGroupInstanceIdAndUserIdQuery(String groupInstanceId, Long userId) {
        return new Query().addCriteria(
                Criteria.where("groupInstanceId").is(groupInstanceId)
                        .and("userId").is(userId)
        );
    }

    /**
     * 构建根据订单ID查询
     */
    public static Query buildOrderIdQuery(String orderId) {
        return new Query().addCriteria(Criteria.where("orderId").is(orderId));
    }

    /**
     * 构建根据拼团实例ID查询所有成员
     */
    public static Query buildGroupInstanceIdQuery(String groupInstanceId) {
        return new Query().addCriteria(Criteria.where("groupInstanceId").is(groupInstanceId))
                .with(Sort.by(Sort.Direction.ASC, "joinTime", "id"));
    }

    /**
     * 构建按用户和活动查询当前有效成员。
     */
    public static Query buildUserIdAndActivityIdQuery(Long userId, String activityId, List<String> activeStatuses) {
        return new Query().addCriteria(
                Criteria.where("userId").is(userId)
                        .and("activityId").is(activityId)
                        .and("memberStatus").in(activeStatuses)
        );
    }

    /**
     * 构建状态更新
     */
    public static Update buildStatusUpdate(Integer status) {
        return new Update().set("status", status);
    }

}
