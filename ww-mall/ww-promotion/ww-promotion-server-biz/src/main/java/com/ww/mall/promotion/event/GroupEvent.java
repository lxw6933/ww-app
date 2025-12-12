package com.ww.mall.promotion.event;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 拼团事件模型
 * 
 * @author ww
 * @create 2025-12-08
 * @description: 用于Disruptor异步处理拼团相关事件
 */
@Data
public class GroupEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    public enum EventType {
        /**
         * 拼团成功
         */
        GROUP_SUCCESS,
        /**
         * 拼团失败
         */
        GROUP_FAILED,
        /**
         * 保存拼团实例到MongoDB
         */
        SAVE_INSTANCE,
        /**
         * 保存拼团成员到MongoDB
         */
        SAVE_MEMBER
    }

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 拼团ID
     */
    private String groupId;

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
     * 订单信息（JSON字符串）
     */
    private String orderInfo;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 错误信息（用于失败事件）
     */
    private String errorMessage;

    /**
     * 扩展信息
     */
    private Map<String, Object> extInfo;

    public GroupEvent() {
        this.createTime = new Date();
        this.extInfo = new HashMap<>();
    }

    public GroupEvent(EventType eventType, String groupId) {
        this();
        this.eventType = eventType;
        this.groupId = groupId;
    }

    /**
     * 添加扩展信息
     */
    public GroupEvent addExtInfo(String key, Object value) {
        if (this.extInfo == null) {
            this.extInfo = new HashMap<>();
        }
        this.extInfo.put(key, value);
        return this;
    }

    /**
     * 获取扩展信息
     */
    public Object getExtInfo(String key) {
        return extInfo != null ? extInfo.get(key) : null;
    }
}

