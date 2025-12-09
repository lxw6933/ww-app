package com.ww.mall.promotion.entity.group;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
 * @description: 拼团实例
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document("group_instance")
public class GroupInstance extends BaseDoc {

    /**
     * 活动ID
     */
    private String activityId;

    /**
     * 团长用户ID
     */
    private Long leaderUserId;

    /**
     * 拼团状态：OPEN-进行中，SUCCESS-成功，FAILED-失败
     */
    private String status;

    /**
     * 需要人数
     */
    private Integer requiredSize;

    /**
     * 当前人数
     */
    private Integer currentSize;

    /**
     * 剩余名额
     */
    private Integer remainingSlots;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 完成时间
     */
    private Date completeTime;

    /**
     * 失败时间
     */
    private Date failedTime;

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
     * 成员列表（冗余存储，便于查询）
     */
    private List<GroupMemberInfo> members;

    /**
     * 拼团成员信息（内部类）
     */
    @Data
    public static class GroupMemberInfo {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 订单ID
         */
        private String orderId;

        /**
         * 加入时间
         */
        private Date joinTime;

        /**
         * 是否团长
         */
        private Boolean isLeader;
    }

    /**
     * 构建根据ID查询
     */
    public static Query buildIdQuery(String id) {
        return BaseDoc.buildIdQuery(id);
    }

    /**
     * 构建根据活动ID和状态查询
     */
    public static Query buildActivityIdAndStatusQuery(String activityId, String status) {
        return new Query().addCriteria(
                Criteria.where("activityId").is(activityId)
                        .and("status").is(status)
        );
    }

    /**
     * 构建根据团长用户ID和状态查询
     */
    public static Query buildLeaderUserIdAndStatusQuery(Long leaderUserId, String status) {
        return new Query().addCriteria(
                Criteria.where("leaderUserId").is(leaderUserId)
                        .and("status").is(status)
        );
    }

    /**
     * 构建查询过期的拼团实例
     */
    public static Query buildExpiredQuery(String status, Date now) {
        return new Query().addCriteria(
                Criteria.where("status").is(status)
                        .and("expireTime").lt(now)
        );
    }

    /**
     * 构建根据状态查询
     */
    public static Query buildStatusQuery(String status) {
        return new Query().addCriteria(Criteria.where("status").is(status));
    }

    /**
     * 构建状态更新
     */
    public static Update buildStatusUpdate(String status) {
        Update update = new Update();
        update.set("status", status);
        if ("SUCCESS".equals(status)) {
            update.set("completeTime", new Date());
        } else if ("FAILED".equals(status)) {
            update.set("failedTime", new Date());
        }
        return update;
    }

    /**
     * 构建更新当前人数和剩余名额
     */
    public static Update buildSizeUpdate(Integer currentSize, Integer remainingSlots) {
        Update update = new Update();
        update.set("currentSize", currentSize);
        update.set("remainingSlots", remainingSlots);
        return update;
    }

}
