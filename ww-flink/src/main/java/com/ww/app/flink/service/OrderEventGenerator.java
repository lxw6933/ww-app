package com.ww.app.flink.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.app.flink.model.OrderEvent;
import com.ww.mall.kafka.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单事件生成器
 * 用于模拟生产环境的订单数据流
 */
@Slf4j
@Service
public class OrderEventGenerator {

    @Resource
    private KafkaService kafkaService;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${ww.flink.job.order.topic:order_events}")
    private String orderTopic;

    @Value("${ww.flink.job.order.generator.enable:false}")
    private boolean generatorEnabled;

    @Value("${ww.flink.job.order.generator.rate:100}")
    private int generationRate;

    // 用于生成唯一订单ID
    private final AtomicLong orderCounter = new AtomicLong(0);
    
    // 随机数生成器
    private final Random random = new Random();
    
    // 支付方式列表
    private final String[] paymentTypes = {"ALIPAY", "WECHAT", "CREDIT_CARD", "OTHER"};
    
    // 渠道列表
    private final String[] channels = {"APP", "WEB", "MINI_PROGRAM", "OTHER"};
    
    // 地区列表
    private final String[] regionCodes = {"110000", "310000", "440100", "330100", "320100", "510100", "420100", "500000"};
    
    // 状态转换映射，用于模拟订单状态流转
    private final Map<String, String[]> statusTransitions = new HashMap<>();
    
    // 用于存储已经创建的订单，支持状态变更
    private final Map<String, OrderEvent> activeOrders = new HashMap<>();

    @PostConstruct
    public void init() {
        if (generatorEnabled) {
            log.info("订单事件生成器已启用，将以每秒约{}个事件的速率生成订单事件", generationRate);
            
            // 初始化状态转换映射
            statusTransitions.put("CREATE", new String[]{"PAY", "CANCEL"});
            statusTransitions.put("PAY", new String[]{"DELIVER", "CANCEL"});
            statusTransitions.put("DELIVER", new String[]{"RECEIVE"});
            statusTransitions.put("RECEIVE", new String[]{"COMPLETE"});
            
            // 初始化生成一些订单
            for (int i = 0; i < 100; i++) {
                generateNewOrder();
            }
        } else {
            log.info("订单事件生成器已禁用");
        }
    }

    /**
     * 定时任务，生成新订单
     * 固定速率执行，适合高频生成
     */
    @Scheduled(fixedRate = 10) // 每10毫秒执行一次
    public void generateOrdersFixedRate() {
        if (!generatorEnabled) return;
        
        // 控制生成速率，抽样决定是否生成新订单
        if (random.nextInt(1000) < generationRate) {
            generateNewOrder();
        }
    }
    
    /**
     * 定时任务，更新已有订单状态
     * 固定延迟执行，适合状态变更
     */
    @Scheduled(fixedDelay = 500) // 每500毫秒执行一次
    public void updateExistingOrders() {
        if (!generatorEnabled || activeOrders.isEmpty()) return;
        
        // 随机选择一个活跃订单进行状态更新
        String[] orderIds = activeOrders.keySet().toArray(new String[0]);
        String orderId = orderIds[random.nextInt(orderIds.length)];
        updateOrderStatus(orderId);
    }
    
    /**
     * 生成新订单
     */
    private void generateNewOrder() {
        try {
            String orderId = "ORD-" + System.currentTimeMillis() + "-" + orderCounter.incrementAndGet();
            
            OrderEvent orderEvent = OrderEvent.builder()
                    .orderId(orderId)
                    .userId(10000L + random.nextInt(90000))
                    .status("CREATE")
                    .amount(BigDecimal.valueOf(random.nextDouble() * 10000).setScale(2, RoundingMode.HALF_UP))
                    .paymentType(paymentTypes[random.nextInt(paymentTypes.length)])
                    .channel(channels[random.nextInt(channels.length)])
                    .categoryId(random.nextInt(100) + 1)
                    .productId(100000L + random.nextInt(900000))
                    .quantity(random.nextInt(10) + 1)
                    .eventTime(LocalDateTime.now())
                    .regionCode(regionCodes[random.nextInt(regionCodes.length)])
                    .extraInfo("{\"ip\":\"" + generateRandomIp() + "\",\"device\":\"" + 
                            (random.nextBoolean() ? "iOS" : "Android") + "\",\"version\":\"" + 
                            random.nextInt(10) + "." + random.nextInt(10) + "\"}")
                    .build();
            
            // 将订单加入活跃订单集合
            activeOrders.put(orderId, orderEvent);
            
            // 发送订单事件到Kafka
            sendToKafka(orderEvent);
            
        } catch (Exception e) {
            log.error("生成订单事件失败", e);
        }
    }
    
    /**
     * 更新订单状态
     */
    private void updateOrderStatus(String orderId) {
        try {
            OrderEvent currentOrder = activeOrders.get(orderId);
            if (currentOrder == null) return;
            
            String currentStatus = currentOrder.getStatus();
            String[] nextPossibleStatus = statusTransitions.get(currentStatus);
            
            // 如果没有下一个可能的状态，则不更新
            if (nextPossibleStatus == null || nextPossibleStatus.length == 0) {
                // 如果是终态，从活跃订单中移除
                if ("COMPLETE".equals(currentStatus) || "CANCEL".equals(currentStatus)) {
                    activeOrders.remove(orderId);
                }
                return;
            }
            
            // 随机选择一个下一个状态
            String nextStatus = nextPossibleStatus[random.nextInt(nextPossibleStatus.length)];
            
            // 创建状态更新后的订单事件
            OrderEvent updatedOrder = OrderEvent.builder()
                    .orderId(currentOrder.getOrderId())
                    .userId(currentOrder.getUserId())
                    .status(nextStatus)
                    .amount(currentOrder.getAmount())
                    .paymentType(currentOrder.getPaymentType())
                    .channel(currentOrder.getChannel())
                    .categoryId(currentOrder.getCategoryId())
                    .productId(currentOrder.getProductId())
                    .quantity(currentOrder.getQuantity())
                    .eventTime(LocalDateTime.now())
                    .regionCode(currentOrder.getRegionCode())
                    .extraInfo(currentOrder.getExtraInfo())
                    .build();
            
            // 更新活跃订单集合
            activeOrders.put(orderId, updatedOrder);
            
            // 如果是终态，从活跃订单中移除
            if ("COMPLETE".equals(nextStatus) || "CANCEL".equals(nextStatus)) {
                activeOrders.remove(orderId);
            }
            
            // 发送订单状态更新事件到Kafka
            sendToKafka(updatedOrder);
            
        } catch (Exception e) {
            log.error("更新订单状态失败", e);
        }
    }
    
    /**
     * 发送订单事件到Kafka
     */
    private void sendToKafka(OrderEvent orderEvent) {
        try {
            String key = orderEvent.getOrderId();
            String value = objectMapper.writeValueAsString(orderEvent);
            
            // 使用KafkaService发送消息
            kafkaService.sendMessage(orderTopic, key, value);
            
            // 只记录部分事件日志，避免日志过多
            if (random.nextInt(100) < 5) {
                log.info("已发送订单事件: {} - {}", orderEvent.getOrderId(), orderEvent.getStatus());
            }
        } catch (Exception e) {
            log.error("发送订单事件到Kafka失败", e);
        }
    }
    
    /**
     * 生成随机IP地址
     */
    private String generateRandomIp() {
        return random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256);
    }
} 