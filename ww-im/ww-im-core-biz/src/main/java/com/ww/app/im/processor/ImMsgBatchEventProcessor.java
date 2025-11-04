package com.ww.app.im.processor;

import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import com.ww.app.im.component.ImHandlerComponent;
import com.ww.app.im.event.ImMsgEvent;
import com.ww.app.im.pool.ImMsgEventPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * IM消息批量事件处理器
 * 优化：处理完消息后回收ImMsgEvent对象到对象池
 * @author ww
 */
@Slf4j
@Component
public class ImMsgBatchEventProcessor implements BatchEventProcessor<ImMsgEvent> {

    @Resource
    private ImHandlerComponent imHandlerComponent;
    
    @Resource
    private ImMsgEventPool imMsgEventPool;

    @Override
    public ProcessResult processBatch(EventBatch<ImMsgEvent> batch) {
        long startTime = System.currentTimeMillis();
        List<Event<ImMsgEvent>> events = batch.getEvents();
        
        if (events.isEmpty()) {
            return ProcessResult.success();
        }
        
        log.debug("批量处理 {} 条IM消息", events.size());
        
        int successCount = 0;
        int failCount = 0;
        long totalDelay = 0;
        
        for (Event<ImMsgEvent> event : events) {
            ImMsgEvent data = null;
            try {
                data = event.getPayload();
                
                // 记录处理延迟
                long delay = data.getProcessDelay();
                totalDelay += delay;
                
                if (delay > 100) {
                    log.warn("消息处理延迟过高: {}ms, eventId={}", delay, event.getEventId());
                }
                
                // 处理消息
                imHandlerComponent.handle(data.getCtx(), data.getImMsg());
                successCount++;
                
            } catch (Exception e) {
                log.error("处理消息失败: eventId={}", event.getEventId(), e);
                failCount++;
                // 不中断批处理，继续处理下一条
            } finally {
                // 回收对象到池中，减少GC压力
                if (data != null) {
                    imMsgEventPool.recycle(data);
                }
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        long avgDelay = totalDelay / events.size();
        
        log.info("批处理完成: 总数={}, 成功={}, 失败={}, 耗时={}ms, 平均延迟={}ms", 
                events.size(), successCount, failCount, duration, avgDelay);
        
        // 只要有成功的就返回成功
        return successCount > 0 ? ProcessResult.success() : 
                ProcessResult.failure("所有消息处理失败");
    }
}
