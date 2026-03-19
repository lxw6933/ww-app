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

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2025-12-08 16:50
 * @description: 拼团成员记录
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document("group_member")
@CompoundIndexes({
        @CompoundIndex(name = "idx_group_member_group_user", def = "{'groupInstanceId': 1, 'userId': 1, 'joinTime': 1}"),
        @CompoundIndex(name = "idx_group_member_group_join_time", def = "{'groupInstanceId': 1, 'joinTime': 1}"),
        @CompoundIndex(name = "idx_group_member_user_join_time", def = "{'userId': 1, 'joinTime': -1}"),
        @CompoundIndex(name = "idx_group_member_order", def = "{'orderId': 1}", unique = true)
})
public class GroupMember extends BaseDoc {

    /**
     * 拼团实例ID
     */
    private String groupInstanceId;

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
     * 实际支付金额。
     * <p>
     * 新版状态机要求支付消息显式透传该值，退款补偿直接使用该字段。
     */
    private BigDecimal payAmount;

    /**
     * 商品SKU ID
     */
    private Long skuId;

    /**
     * 成员生命周期状态。
     */
    private String memberStatus;

    /**
     * 售后单号。
     */
    private String afterSaleId;

    /**
     * 最近一次轨迹编码。
     */
    private String latestTrajectory;

    /**
     * 最近一次轨迹时间。
     */
    private Date latestTrajectoryTime;

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
}
