package com.ww.app.disruptor.persistence;

import com.ww.app.disruptor.model.Event;
import lombok.Data;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 持久化管理器接口 - 防止数据丢失
 * 
 * <p>设计理念：
 * <ul>
 *   <li>支持同步和异步持久化策略</li>
 *   <li>批量操作提升IO效率</li>
 *   <li>提供数据恢复和清理能力</li>
 *   <li>保证数据一致性和可靠性</li>
 * </ul>
 * 
 * <p>性能优化：
 * <ul>
 *   <li>批量持久化减少IO次数</li>
 *   <li>异步持久化避免阻塞主流程</li>
 *   <li>使用WAL（Write-Ahead Log）提升性能</li>
 * </ul>
 * 
 * @param <T> 事件负载类型
 * @author ww-framework
 * @version 2.0
 */
public interface PersistenceManager<T> {
    
    /**
     * 启动持久化管理器
     * 
     * <p>初始化必要的资源，如：
     * <ul>
     *   <li>创建数据目录</li>
     *   <li>初始化线程池</li>
     *   <li>启动定时任务</li>
     *   <li>加载元数据</li>
     * </ul>
     * 
     * @throws PersistenceException 启动失败时抛出
     */
    void start();
    
    /**
     * 停止持久化管理器
     * 
     * <p>优雅关闭，确保：
     * <ul>
     *   <li>刷新所有待持久化数据</li>
     *   <li>关闭所有资源</li>
     *   <li>保存元数据</li>
     * </ul>
     */
    void stop();
    
    /**
     * 同步持久化单个事件
     * 
     * <p>适用场景：
     * <ul>
     *   <li>关键事件需要立即持久化</li>
     *   <li>ALWAYS持久化策略</li>
     * </ul>
     * 
     * @param event 待持久化的事件
     * @throws PersistenceException 持久化失败时抛出
     */
    void persist(Event<T> event);
    
    /**
     * 异步持久化单个事件
     * 
     * <p>适用场景：
     * <ul>
     *   <li>高吞吐量场景</li>
     *   <li>ASYNC持久化策略</li>
     * </ul>
     * 
     * @param event 待持久化的事件
     * @return CompletableFuture，可用于追踪持久化结果
     */
    CompletableFuture<Void> persistAsync(Event<T> event);
    
    /**
     * 批量同步持久化事件（性能优化）
     * 
     * <p>批量操作优势：
     * <ul>
     *   <li>减少IO次数（顺序写优化）</li>
     *   <li>降低文件系统开销</li>
     *   <li>提高整体吞吐量</li>
     * </ul>
     * 
     * @param events 待持久化的事件列表
     * @throws PersistenceException 持久化失败时抛出
     */
    void persistBatch(List<Event<T>> events);
    
    /**
     * 批量异步持久化事件
     * 
     * @param events 待持久化的事件列表
     * @return CompletableFuture，可用于追踪持久化结果
     */
    CompletableFuture<Void> persistBatchAsync(List<Event<T>> events);
    
    /**
     * 移除已处理的事件
     * 
     * <p>用于：
     * <ul>
     *   <li>事件处理成功后清理持久化数据</li>
     *   <li>避免磁盘空间浪费</li>
     * </ul>
     * 
     * @param eventId 事件ID
     */
    void remove(String eventId);
    
    /**
     * 批量移除已处理的事件（性能优化）
     * 
     * @param eventIds 事件ID列表
     */
    void removeBatch(List<String> eventIds);
    
    /**
     * 恢复未处理的事件（用于重启后恢复）
     * 
     * <p>恢复策略：
     * <ul>
     *   <li>扫描持久化存储</li>
     *   <li>过滤已完成的事件</li>
     *   <li>按时间戳排序</li>
     *   <li>返回待处理事件列表</li>
     * </ul>
     * 
     * @return 未处理的事件列表，按创建时间排序
     */
    List<Event<T>> recover();
    
    /**
     * 清理过期数据
     * 
     * <p>清理策略：
     * <ul>
     *   <li>基于时间的过期清理</li>
     *   <li>基于磁盘空间的清理</li>
     *   <li>增量清理避免阻塞</li>
     * </ul>
     */
    void cleanup();
    
    /**
     * 强制刷新所有待持久化数据到磁盘
     * 
     * <p>适用场景：
     * <ul>
     *   <li>关闭前确保数据不丢失</li>
     *   <li>周期性检查点</li>
     *   <li>手动触发数据同步</li>
     * </ul>
     */
    void flush();
    
    /**
     * 获取持久化统计信息
     * 
     * @return 统计信息对象
     */
    PersistenceStats getStats();
    
    /**
     * 持久化统计信息
     */
    @Data
    class PersistenceStats {
        /** 总持久化事件数 */
        private long totalPersisted;
        
        /** 总删除事件数 */
        private long totalRemoved;
        
        /** 当前存储的事件数 */
        private long currentStoredCount;
        
        /** 平均持久化耗时（毫秒） */
        private double avgPersistTime;
        
        /** 最大持久化耗时（毫秒） */
        private long maxPersistTime;
        
        /** 持久化失败次数 */
        private long failureCount;
        
        /** 磁盘使用量（字节） */
        private long diskUsage;

        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + "B";
            if (bytes < 1024 * 1024) return String.format("%.2fKB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2fMB", bytes / (1024.0 * 1024));
            return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 持久化异常
     */
    class PersistenceException extends RuntimeException {

        public PersistenceException(String message) {
            super(message);
        }
        
        public PersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
