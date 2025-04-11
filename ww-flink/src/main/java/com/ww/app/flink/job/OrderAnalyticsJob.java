package com.ww.app.flink.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.app.flink.model.OrderEvent;
import com.ww.mall.kafka.listener.AbstractKafkaListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 订单分析作业
 * 实时处理订单事件流并进行分析
 */
@Slf4j
@Component
public class OrderAnalyticsJob extends AbstractKafkaListener implements ApplicationRunner {

    @Resource
    private StreamExecutionEnvironment env;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${ww.flink.job.order.enable:false}")
    private boolean jobEnabled;

    @Value("${ww.flink.job.order.topic:order_events}")
    private String orderTopic;

    @Value("${ww.flink.job.order.output-topic:order_analytics}")
    private String outputTopic;

    @Value("${ww.flink.job.order.alert-topic:order_alerts}")
    private String alertTopic;

    @Value("${ww.flink.job.order.group-id:order-analytics-group}")
    private String groupId;
    
    // 用于从Kafka接收消息的队列
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    /**
     * Kafka消息监听方法
     * 使用AbstractKafkaListener处理消息
     */
    @KafkaListener(topics = "#{@orderAnalyticsJob.orderTopic}", 
                   groupId = "#{@orderAnalyticsJob.groupId}", 
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        processRecord(record, ack);
    }
    
    /**
     * 实际处理消息的方法
     * 接收Kafka消息并将其放入队列
     */
    @Override
    protected void processMessage(String key, String value, ConsumerRecord<String, String> record) throws Exception {
        // 将消息添加到队列中，供Flink处理
        try {
            messageQueue.put(value);
        } catch (InterruptedException e) {
            log.error("将消息放入队列时被中断", e);
            Thread.currentThread().interrupt();
            throw e;
        }
    }
    
    /**
     * 判断是否应该重试
     */
    @Override
    protected boolean shouldRetry(Exception e) {
        // 自定义重试逻辑
        return !(e instanceof InterruptedException);
    }
    
    /**
     * 获取最大重试次数
     */
    @Override
    protected int getMaxRetries() {
        return 3;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!jobEnabled) {
            log.info("订单分析作业未启用");
            return;
        }

        log.info("启动订单分析作业");

        // 设置作业并行度
        env.setParallelism(1);
        
        // 创建自定义源从消息队列读取数据
        DataStream<String> orderJsonStream = env.addSource(new QueueSourceFunction(messageQueue))
                .name("Order Kafka Source");

        // 解析JSON为OrderEvent对象
        DataStream<OrderEvent> orderEventStream = orderJsonStream
                .map(new MapFunction<String, OrderEvent>() {
                    @Override
                    public OrderEvent map(String json) throws Exception {
                        return objectMapper.readValue(json, OrderEvent.class);
                    }
                })
                .name("Parse JSON");

        // 1. 成功支付的订单统计 - 每分钟统计一次
        orderEventStream
                .filter(event -> "PAY".equals(event.getStatus()))
                .keyBy(OrderEvent::getPaymentType)
                .window(TumblingProcessingTimeWindows.of(Time.minutes(1)))
                .process(new PaymentTypeWindowFunction())
                .name("Payment Type Analytics")
                .print();

        // 2. 交易额度异常监控 - 识别大额和小额异常订单
        orderEventStream
                .filter(event -> "PAY".equals(event.getStatus()))
                .keyBy(OrderEvent::getUserId)
                .process(new AnomalyDetectionFunction())
                .name("Anomaly Detection")
                .print();

        // 3. 销售量实时排行 - 商品分类销售量实时排行
        orderEventStream
                .filter(event -> "CREATE".equals(event.getStatus()))
                .keyBy(OrderEvent::getCategoryId)
                .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
                .process(new CategoryRankingWindowFunction())
                .name("Category Ranking")
                .print();

        // 4. 订单转化率统计 - 计算从创建到完成的转化率
        SingleOutputStreamOperator<Tuple2<String, Long>> orderStatusCount = orderEventStream
                .keyBy(event -> event.getStatus())
                .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
                .process(new OrderStatusCountWindowFunction())
                .name("Order Status Count");

        orderStatusCount.print();

        // 5. 地区订单分布 - 按地区统计订单分布
        orderEventStream
                .filter(event -> "CREATE".equals(event.getStatus()))
                .keyBy(OrderEvent::getRegionCode)
                .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
                .process(new RegionOrderDistributionWindowFunction())
                .name("Region Order Distribution")
                .print();
                
        // 6. 渠道效果分析 - 分析不同渠道的订单效果
        orderEventStream
                .filter(event -> "CREATE".equals(event.getStatus()))
                .keyBy(OrderEvent::getChannel)
                .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
                .process(new ChannelAnalyticsWindowFunction())
                .name("Channel Analytics")
                .print();

        // 7. 实时交易金额监控 - 每30秒统计交易总额
        orderEventStream
                .filter(event -> "PAY".equals(event.getStatus()))
                .keyBy(event -> "all")  // 全局分组
                .window(TumblingProcessingTimeWindows.of(Time.seconds(30)))
                .process(new TransactionAmountWindowFunction())
                .name("Transaction Amount Monitor")
                .print();
                
        // 执行作业
        env.execute("Order Analytics Job");
    }
    
    /**
     * 队列源函数，从队列中读取数据作为Flink源
     */
    private static class QueueSourceFunction implements SourceFunction<String> {
        private final BlockingQueue<String> queue;
        private volatile boolean isRunning = true;
        
        public QueueSourceFunction(BlockingQueue<String> queue) {
            this.queue = queue;
        }
        
        @Override
        public void run(SourceContext<String> ctx) throws Exception {
            while (isRunning) {
                String message = queue.take();
                synchronized (ctx.getCheckpointLock()) {
                    ctx.collect(message);
                }
            }
        }
        
        @Override
        public void cancel() {
            isRunning = false;
        }
    }

    /**
     * 按支付类型的窗口统计函数
     */
    public static class PaymentTypeWindowFunction extends ProcessWindowFunction<OrderEvent, String, String, TimeWindow> {
        @Override
        public void process(String paymentType, Context context, Iterable<OrderEvent> elements, Collector<String> out) {
            long count = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (OrderEvent event : elements) {
                count++;
                totalAmount = totalAmount.add(event.getAmount());
            }

            String result = String.format(
                    "支付方式: %s, 时间窗口: %s - %s, 订单数: %d, 总金额: %.2f",
                    paymentType,
                    context.window().getStart(),
                    context.window().getEnd(),
                    count,
                    totalAmount
            );

            out.collect(result);
        }
    }

    /**
     * 异常交易检测函数
     */
    public static class AnomalyDetectionFunction extends KeyedProcessFunction<Long, OrderEvent, String> {
        private transient ValueState<BigDecimal> userAvgAmountState;

        @Override
        public void open(Configuration parameters) {
            ValueStateDescriptor<BigDecimal> descriptor = new ValueStateDescriptor<>(
                    "user-avg-amount",
                    BigDecimal.class
            );
            userAvgAmountState = getRuntimeContext().getState(descriptor);
        }

        @Override
        public void processElement(OrderEvent event, Context ctx, Collector<String> out) throws Exception {
            BigDecimal currentAvg = userAvgAmountState.value();
            if (currentAvg == null) {
                userAvgAmountState.update(event.getAmount());
                return;
            }

            // 计算新的平均值
            BigDecimal newAvg = currentAvg.add(event.getAmount()).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            userAvgAmountState.update(newAvg);

            // 检测异常 - 如果当前交易额超过平均值的3倍或低于平均值的0.3倍
            BigDecimal ratio = event.getAmount().divide(currentAvg, 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(BigDecimal.valueOf(3)) > 0) {
                out.collect(String.format(
                        "异常大额交易警报 - 用户ID: %d, 订单ID: %s, 金额: %.2f, 平均金额: %.2f, 比率: %.2f",
                        event.getUserId(),
                        event.getOrderId(),
                        event.getAmount(),
                        currentAvg,
                        ratio
                ));
            } else if (ratio.compareTo(BigDecimal.valueOf(0.3)) < 0 && event.getAmount().compareTo(BigDecimal.valueOf(10)) > 0) {
                out.collect(String.format(
                        "异常小额交易警报 - 用户ID: %d, 订单ID: %s, 金额: %.2f, 平均金额: %.2f, 比率: %.2f",
                        event.getUserId(),
                        event.getOrderId(),
                        event.getAmount(),
                        currentAvg,
                        ratio
                ));
            }
        }
    }

    /**
     * 商品分类排名窗口函数
     */
    public static class CategoryRankingWindowFunction extends ProcessWindowFunction<OrderEvent, String, Integer, TimeWindow> {
        @Override
        public void process(Integer categoryId, Context context, Iterable<OrderEvent> elements, Collector<String> out) {
            long count = 0;
            int totalQuantity = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (OrderEvent event : elements) {
                count++;
                totalQuantity += event.getQuantity();
                totalAmount = totalAmount.add(event.getAmount());
            }

            String result = String.format(
                    "商品分类: %d, 时间窗口: %s - %s, 订单数: %d, 总数量: %d, 总金额: %.2f",
                    categoryId,
                    context.window().getStart(),
                    context.window().getEnd(),
                    count,
                    totalQuantity,
                    totalAmount
            );

            out.collect(result);
        }
    }

    /**
     * 订单状态计数窗口函数
     */
    public static class OrderStatusCountWindowFunction extends ProcessWindowFunction<OrderEvent, Tuple2<String, Long>, String, TimeWindow> {
        @Override
        public void process(String status, Context context, Iterable<OrderEvent> elements, Collector<Tuple2<String, Long>> out) {
            long count = 0;
            for (OrderEvent ignored : elements) {
                count++;
            }
            out.collect(new Tuple2<>(status, count));
        }
    }

    /**
     * 地区订单分布窗口函数
     */
    public static class RegionOrderDistributionWindowFunction extends ProcessWindowFunction<OrderEvent, String, String, TimeWindow> {
        @Override
        public void process(String regionCode, Context context, Iterable<OrderEvent> elements, Collector<String> out) {
            long count = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (OrderEvent event : elements) {
                count++;
                totalAmount = totalAmount.add(event.getAmount());
            }

            String result = String.format(
                    "地区: %s, 时间窗口: %s - %s, 订单数: %d, 总金额: %.2f",
                    regionCode,
                    context.window().getStart(),
                    context.window().getEnd(),
                    count,
                    totalAmount
            );

            out.collect(result);
        }
    }

    /**
     * 渠道分析窗口函数
     */
    public static class ChannelAnalyticsWindowFunction extends ProcessWindowFunction<OrderEvent, String, String, TimeWindow> {
        @Override
        public void process(String channel, Context context, Iterable<OrderEvent> elements, Collector<String> out) {
            long count = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            Map<Integer, Integer> categoryCount = new HashMap<>();

            for (OrderEvent event : elements) {
                count++;
                totalAmount = totalAmount.add(event.getAmount());
                // 统计各分类数量
                categoryCount.merge(event.getCategoryId(), 1, Integer::sum);
            }

            // 找出最受欢迎的分类
            int topCategory = -1;
            int topCount = 0;
            for (Map.Entry<Integer, Integer> entry : categoryCount.entrySet()) {
                if (entry.getValue() > topCount) {
                    topCount = entry.getValue();
                    topCategory = entry.getKey();
                }
            }

            String result = String.format(
                    "渠道: %s, 时间窗口: %s - %s, 订单数: %d, 总金额: %.2f, 最受欢迎分类: %d (数量: %d)",
                    channel,
                    context.window().getStart(),
                    context.window().getEnd(),
                    count,
                    totalAmount,
                    topCategory,
                    topCount
            );

            out.collect(result);
        }
    }

    /**
     * 交易金额监控窗口函数
     */
    public static class TransactionAmountWindowFunction extends ProcessWindowFunction<OrderEvent, String, String, TimeWindow> {
        @Override
        public void process(String key, Context context, Iterable<OrderEvent> elements, Collector<String> out) {
            long count = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            // 按支付方式统计
            Map<String, Tuple2<Long, BigDecimal>> paymentTypeStats = new HashMap<>();

            for (OrderEvent event : elements) {
                count++;
                totalAmount = totalAmount.add(event.getAmount());
                
                // 更新支付方式统计
                Tuple2<Long, BigDecimal> stats = paymentTypeStats.getOrDefault(
                        event.getPaymentType(), 
                        new Tuple2<>(0L, BigDecimal.ZERO)
                );
                paymentTypeStats.put(
                        event.getPaymentType(), 
                        new Tuple2<>(stats.f0 + 1, stats.f1.add(event.getAmount()))
                );
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format(
                    "交易统计 - 时间窗口: %s - %s, 订单数: %d, 总金额: %.2f\n",
                    context.window().getStart(),
                    context.window().getEnd(),
                    count,
                    totalAmount
            ));
            
            // 添加支付方式明细
            result.append("支付方式明细:\n");
            for (Map.Entry<String, Tuple2<Long, BigDecimal>> entry : paymentTypeStats.entrySet()) {
                result.append(String.format(
                        "  %s: 订单数 %d, 金额 %.2f\n",
                        entry.getKey(),
                        entry.getValue().f0,
                        entry.getValue().f1
                ));
            }

            out.collect(result.toString());
        }
    }
} 