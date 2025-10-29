package com.ww.app.disruptor.persistence;

import com.ww.app.disruptor.model.Event;

import java.util.List;

/**
 * 持久化管理器接口 - 防止数据丢失
 * 
 * @author ww-framework
 */
public interface PersistenceManager<T> {
    
    /**
     * 启动持久化管理器
     */
    void start();
    
    /**
     * 停止持久化管理器
     */
    void stop();
    
    /**
     * 持久化事件
     * 
     * @param event 事件
     */
    void persist(Event<T> event);
    
    /**
     * 移除已处理的事件
     * 
     * @param eventId 事件ID
     */
    void remove(String eventId);
    
    /**
     * 恢复未处理的事件（用于重启后恢复）
     * 
     * @return 未处理的事件列表
     */
    List<Event<T>> recover();
    
    /**
     * 清理过期数据
     */
    void cleanup();
}
