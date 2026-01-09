package com.ww.mall.promotion.mq;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-12-08 18:10
 * @description: 拼团成功消息
 */
@Data
public class GroupSuccessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 拼团实例ID
     */
    private String groupId;

    /**
     * 活动ID
     */
    private String activityId;

    /**
     * 完成时间
     */
    private Date completeTime;

    /**
     * 成员订单列表
     */
    private List<MemberOrder> memberOrders;

    @Data
    public static class MemberOrder implements Serializable {
        private Long userId;
        private String orderId;
        private Boolean isLeader;
    }

}




