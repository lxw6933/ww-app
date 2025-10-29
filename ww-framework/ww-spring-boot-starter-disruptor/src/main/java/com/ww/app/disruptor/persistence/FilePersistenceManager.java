package com.ww.app.disruptor.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.disruptor.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 文件持久化管理器 - 防止机器重启数据丢失
 * 
 * @author ww-framework
 */
public class FilePersistenceManager<T> implements PersistenceManager<T> {
    
    private static final Logger log = LoggerFactory.getLogger(FilePersistenceManager.class);
    
    private final String dataDir;
    private final long retentionHours;
    private final ObjectMapper objectMapper;
    private final Class<T> payloadClass;
    private ScheduledExecutorService cleanupScheduler;
    private final Map<String, Event<T>> eventCache = new ConcurrentHashMap<>();
    
    public FilePersistenceManager(String dataDir, long retentionHours, Class<T> payloadClass) {
        this.dataDir = dataDir;
        this.retentionHours = retentionHours;
        this.payloadClass = payloadClass;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public void start() {
        try {
            // 创建数据目录
            Files.createDirectories(Paths.get(dataDir));
            
            // 启动清理定时任务（每小时执行一次）
            this.cleanupScheduler = ThreadUtil.initScheduledExecutorService("persistence-cleanup", 1);
            cleanupScheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.HOURS);
            
            log.info("文件持久化管理器启动成功，数据目录: {}", dataDir);
        } catch (Exception e) {
            log.error("文件持久化管理器启动失败", e);
            throw new RuntimeException("文件持久化管理器启动失败", e);
        }
    }
    
    @Override
    public void stop() {
        try {
            if (cleanupScheduler != null) {
                ThreadUtil.shutdown("持久化清理", this::cleanup, cleanupScheduler);
            }
            
            // 保存缓存中的事件
            for (Event<T> event : eventCache.values()) {
                persistToFile(event);
            }
            
            log.info("文件持久化管理器已停止");
        } catch (Exception e) {
            log.error("文件持久化管理器停止异常", e);
        }
    }
    
    @Override
    public void persist(Event<T> event) {
        try {
            // 先放入缓存
            eventCache.put(event.getEventId(), event);
            
            // 持久化到文件
            persistToFile(event);
            
        } catch (Exception e) {
            log.error("持久化事件失败: {}", event.getEventId(), e);
        }
    }
    
    @Override
    public void remove(String eventId) {
        try {
            // 从缓存移除
            eventCache.remove(eventId);
            
            // 删除文件
            Path filePath = Paths.get(dataDir, eventId + ".json");
            Files.deleteIfExists(filePath);
            
        } catch (Exception e) {
            log.error("删除持久化事件失败: {}", eventId, e);
        }
    }
    
    @Override
    public List<Event<T>> recover() {
        List<Event<T>> events = new ArrayList<>();
        
        try {
            Path dir = Paths.get(dataDir);
            if (!Files.exists(dir)) {
                return events;
            }
            
            // 读取所有json文件
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                for (Path file : stream) {
                    try {
                        Event<T> event = readEventFromFile(file);
                        if (event != null) {
                            events.add(event);
                            eventCache.put(event.getEventId(), event);
                        }
                    } catch (Exception e) {
                        log.error("恢复事件失败: {}", file.getFileName(), e);
                    }
                }
            }
            
            log.info("恢复了 {} 个未处理的事件", events.size());
            
        } catch (Exception e) {
            log.error("恢复事件失败", e);
        }
        
        return events;
    }
    
    @Override
    public void cleanup() {
        try {
            Path dir = Paths.get(dataDir);
            if (!Files.exists(dir)) {
                return;
            }
            
            LocalDateTime expiryTime = LocalDateTime.now().minusHours(retentionHours);
            int cleanedCount = 0;
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                for (Path file : stream) {
                    try {
                        // 检查文件修改时间
                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(file).toInstant(),
                            java.time.ZoneId.systemDefault()
                        );
                        
                        if (fileTime.isBefore(expiryTime)) {
                            Files.deleteIfExists(file);
                            cleanedCount++;
                        }
                    } catch (Exception e) {
                        log.error("清理文件失败: {}", file.getFileName(), e);
                    }
                }
            }
            
            if (cleanedCount > 0) {
                log.info("清理了 {} 个过期事件文件", cleanedCount);
            }
            
        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }
    
    /**
     * 持久化到文件
     */
    private void persistToFile(Event<T> event) throws IOException {
        Path filePath = Paths.get(dataDir, event.getEventId() + ".json");
        objectMapper.writeValue(filePath.toFile(), event);
    }
    
    /**
     * 从文件读取事件
     */
    private Event<T> readEventFromFile(Path filePath) throws IOException {
        // 注意：这里简化处理，实际需要根据泛型类型正确反序列化
        return objectMapper.readValue(filePath.toFile(), 
            objectMapper.getTypeFactory().constructParametricType(Event.class, payloadClass));
    }
}
