package com.ww.mall.seckill.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.constant.RedisChannelConstant;
import com.ww.mall.common.enums.SensitiveWordHandlerType;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.common.utils.IdUtil;
import com.ww.mall.common.utils.IpUtil;
import com.ww.mall.excel.MallExcelTemplate;
import com.ww.mall.excel.annotation.ExcelExportTimer;
import com.ww.mall.excel.annotation.ExcelImportTimer;
import com.ww.mall.excel.vo.ExcelResultVO;
import com.ww.mall.ip.Ip2regionSearcher;
import com.ww.mall.ip.common.IpInfo;
import com.ww.mall.member.member.bo.MemberLoginBO;
import com.ww.mall.rabbitmq.MallPublisher;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.queue.QueueConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import com.ww.mall.redis.MallRedisTemplate;
import com.ww.mall.seckill.component.CodeGeneratorService;
import com.ww.mall.seckill.component.IssueCodeService;
import com.ww.mall.seckill.entity.A;
import com.ww.mall.seckill.entity.B;
import com.ww.mall.seckill.entity.Demo;
import com.ww.mall.seckill.listener.DemoImportListener;
import com.ww.mall.seckill.model.DemoModel;
import com.ww.mall.seckill.node.executor.DemoFlowExecutor;
import com.ww.mall.seckill.service.DemoService;
import com.ww.mall.seckill.view.bo.SensitiveWordBO;
import com.ww.mall.sensitive.annotation.MallSensitiveWordHandler;
import com.ww.mall.third.sms.SmsApi;
import io.github.linpeilie.Converter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ww
 * @create 2024-05-14 23:24
 * @description:
 */
@Slf4j
@Service
public class DemoServiceImpl implements DemoService {

    @Resource
    private MallRedisTemplate mallRedisTemplate;

    @Resource
    private SmsApi smsApi;

    @Resource
    private ThreadPoolExecutor defaultThreadPoolExecutor;

    @Resource
    private MallPublisher mallPublisher;

    @Resource
    private MongoTemplate mongoTemplate;

    private final AtomicInteger num = new AtomicInteger(0);

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("testBoomFiler");
        bloomFilter.tryInit(1000000, 0.03);
        mallRedisTemplate.initHashStock("skuHashStock", 10);
        mallRedisTemplate.setHashStock("stock1", 10, 2, 2);
        mallRedisTemplate.setHashStock("stock2", 10, 7, 2);

        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            codes.add(i + Constant.SPLIT);
        }
        long start = System.currentTimeMillis();
        issueCodeService.addRedeemCodes("test", codes);
        long end = System.currentTimeMillis();
        log.info("耗时：{}", (end - start) / 1000);
    }

    @Resource
    private CodeGeneratorService codeGeneratorService;

    @Override
    public int generatorCode(String batchNo, int length, int totalCount) {
        return codeGeneratorService.doGeneratorCode(batchNo, length, totalCount);
    }

    @Resource
    private IssueCodeService issueCodeService;

    @Override
    public List<String> issueCode(String outOrderCode, int quantity) {

        if ("1".equals(outOrderCode)) {
            outOrderCode = IdUtil.generatorIdStr();
        }
        return issueCodeService.distributeCodes("test", outOrderCode, quantity);
    }

    @Override
    public void testEncryptReqData(MemberLoginBO memberLoginBO) {
        System.out.println("请求参数：" + memberLoginBO);
    }

    @Override
    public void testInsertMongo() {
        Demo demo = new Demo();
        demo.setEmpNo(0);
        demo.setSalary(0);
        demo.setFromDate(new Date());
        demo.setToDate(new Date());
        mongoTemplate.save(demo);
    }

    @Override
    public boolean testLuaScript(Integer type) {
        Map<String, Integer> map = new HashMap<>();
        map.put("stock1", 1);
        map.put("stock2", 1);
        switch (type) {
            case 1:
                return mallRedisTemplate.a("skuStock", 1);
            case 2:
                return mallRedisTemplate.decrementStock("skuStock", 1);
            case 3:
                return mallRedisTemplate.multipleLockHashStock(map);
            case 4:
                return mallRedisTemplate.batchUseHashStock(map);
            case 5:
                return mallRedisTemplate.batchRollbackHashStock(map);
            default:
                throw new ApiException("不支持类型");
        }
    }

    @Override
    public boolean secKillHashStock(Integer type) {
        switch (type) {
            case 1:
                // 锁定库存
                if (mallRedisTemplate.lockHashStock("skuHashStock", 1)) {
                    log.info("扣减成功");
                    return true;
                } else {
                    return false;
                }
            case 2:
                // 使用库存
                if (mallRedisTemplate.useHashStock("skuHashStock", 1)) {
                    log.info("使用成功");
                    return true;
                } else {
                    return false;
                }
            case 3:
                // 回滚库存
                if (mallRedisTemplate.rollbackHashStock("skuHashStock", 1)) {
                    log.info("回滚成功");
                    return true;
                } else {
                    return false;
                }
            default:
                throw new ApiException("不支持类型");
        }
    }

    @Override
    public boolean secKillOrder() {
        if (mallRedisTemplate.decrementStock("skuStock", 1)) {
            String orderDate = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN);
            String orderNo = IdUtil.generatorIdStr();
            int totalOrderNum = num.incrementAndGet();
            mallPublisher.publishMsg(ExchangeConstant.MALL_OMS_EXCHANGE, RouteKeyConstant.MALL_CREATE_ORDER_KEY, orderNo);
            log.info("下单总数[{}]订单[{}]下单成功[{}]", totalOrderNum, orderNo, orderDate);
        }
        return true;
    }

    @Override
    public void traceId() {
        // interface 日志
        log.info("interface start log");
        // thread pool日志
        for (int i = 0; i < 3; i++) {
            defaultThreadPoolExecutor.submit(() -> log.info("thread pool log"));
        }
        // mq日志
        mallPublisher.publishMsg(ExchangeConstant.MALL_MEMBER_EXCHANGE, RouteKeyConstant.MALL_MEMBER_REGISTER_KEY, 1);
        // feign日志
        smsApi.sendSms("15970191157", "9527");
        log.info("interface end log");
    }

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void msg() {
        log.info("seckill msg");
        rabbitTemplate.convertAndSend(QueueConstant.MALL_TEST_QUEUE, "1");
    }

    @Override
    public void cache(String msg) {
        mallRedisTemplate.publishMessage(RedisChannelConstant.MALL_SPU_CHANNEL, msg);
    }

    private int number = 0;

    @Override
    public void boomFilter(Integer type, Long ele) {
//        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("testBoomFiler");
//        switch (type) {
//            case 1:
//                // 新增
//                for (long i = number * 50000; i < (number + 1) * 50000; i++) {
//                    bloomFilter.add(i);
//                }
//                number++;
//                log.info("number: {}", number);
//                break;
//            case 2:
//                // 判断
//                log.info("是否包含：【{}】,【{}】", ele, bloomFilter.contains(ele));
//        }
        switch (type) {
            case 1:
                // 新增
                List<Integer> dataList = new ArrayList<>();
                for (int i = number * 50000; i < (number + 1) * 50000; i++) {
                    dataList.add(i);
                }
                mallRedisTemplate.initializeBitmap("bitMapTest", dataList);
                number++;
                log.info("number: {}", number);
                break;
            case 2:
                // 判断
                log.info("是否包含：[{}],[{}]", ele, redisTemplate.opsForValue().getBit("bitMapTest", ele));
        }
    }

    @Resource
    private DemoFlowExecutor demoFlowExecutor;

    @Override
    public void liteFlow() {
        demoFlowExecutor.testConfig();
    }

    @Resource
    private SensitiveWordBs sensitiveWordBs;

    @Override
    @MallSensitiveWordHandler(content = {"#content.word", "#content.content"}, handlerType = SensitiveWordHandlerType.REPLACE)
    public String sensitiveWord(SensitiveWordBO content) {
        return JSON.toJSONString(content);
    }

    @Resource
    private MallExcelTemplate mallExcelTemplate;

    private final ExecutorService executorService = Executors.newFixedThreadPool(20);

    @Override
    @ExcelImportTimer
    public void importData(MultipartFile file) {
        ExcelResultVO excelResult = new ExcelResultVO();
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            try {
                int num = i;
                tasks.add(() -> {
                    log.info("线程{}执行任务{}", Thread.currentThread().getName(), num);
                    mallExcelTemplate.readExcel(file, num, DemoModel.class, new DemoImportListener(excelResult));
                    return num;
                });
            } catch (Exception e) {
                throw new ApiException("导入数据异常");
            }
        }
        List<Future<Integer>> futures;
        try {
            futures = executorService.invokeAll(tasks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        futures.forEach(objectFuture -> {
            try {
                Integer task = objectFuture.get();
                System.out.println("任务【" + task + "】执行完成");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("==============导入完成=============");
        System.out.println(excelResult);
        System.out.println("==============导入完成=============");
    }

    @Override
    @ExcelExportTimer
    public void exportDate(HttpServletResponse response) {
        long totalCount = mongoTemplate.count(new Query(), Demo.class);
        int sheetNumber = 20;
        int sheetPageNumber = (int) (totalCount / sheetNumber);
        // 每个sheet都开启一个线程去写入
        ExecutorService executorService = Executors.newFixedThreadPool(sheetNumber);
        CountDownLatch countDownLatch = new CountDownLatch(sheetNumber);

        Map<String, List<Demo>> map = new HashMap<>();
        for (int i = 0; i < sheetNumber; i++) {
            int sheetIndex = i;
            executorService.submit(() -> {
                PageRequest pageRequest = PageRequest.of(sheetIndex, sheetPageNumber);
                // 构建聚合管道
                List<AggregationOperation> pipeline = Arrays.asList(
                        // 跳过前面的记录
                        Aggregation.skip((long) pageRequest.getPageNumber() * pageRequest.getPageSize()),
                        // 限制返回的记录数
                        Aggregation.limit(pageRequest.getPageSize())
                );
                // 执行聚合操作
                Aggregation aggregation = Aggregation.newAggregation(pipeline);
                List<Demo> resultList = mongoTemplate.aggregate(aggregation, "demo", Demo.class).getMappedResults();
                map.put("demo" + sheetIndex, resultList);
                countDownLatch.countDown();
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Set<String> fieldNames = new HashSet<>();
        fieldNames.add("empNo");
        try {
            mallExcelTemplate.exportExcelOfManySheet(response, map, "demo", fieldNames, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // https://github.com/alibaba/easyexcel/issues/2358
    }

    @Resource
    private Ip2regionSearcher ip2regionSearcher;

    public String ip2region(HttpServletRequest request) {
        IpInfo ipInfo = ip2regionSearcher.search(IpUtil.getIp(request));
        return ipInfo.toString();
    }

    @Resource
    private Converter converter;

    @Override
    public void testBeanCopy(int type) {
        A a = new A("jack", 23, false);
        for (int i = 0; i < 10000000; i++) {
            if (type == 0) {
                converter.convert(a, B.class);
            } else {
                BeanUtil.toBean(a, B.class);
            }
        }
    }

}
