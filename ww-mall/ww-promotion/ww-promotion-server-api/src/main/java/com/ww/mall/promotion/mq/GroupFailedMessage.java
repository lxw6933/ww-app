package com.ww.mall.promotion.mq;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ww
 * @create 2025-12-08 18:10
 * @description: 拼团失败消息
 */
@Data
public class GroupFailedMessage implements Serializable {

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
     * 失败时间
     */
    private Date failedTime;

    /**
     * 失败原因
     */
    private String reason;

}

