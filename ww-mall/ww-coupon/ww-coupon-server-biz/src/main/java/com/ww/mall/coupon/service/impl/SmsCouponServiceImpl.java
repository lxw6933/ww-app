package com.ww.mall.coupon.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.mongodb.client.result.UpdateResult;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.ww.app.common.annotation.TimeCost;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.CommonUtils;
import com.ww.app.excel.ExcelMinioTemplate;
import com.ww.app.mongodb.common.BaseDoc;
import com.ww.app.mongodb.handler.MongoBulkDataHandler;
import com.ww.app.mongodb.utils.MongoUtils;
import com.ww.app.redis.AppRedisTemplate;
import com.ww.app.redis.annotation.DistributedLock;
import com.ww.app.redis.annotation.Resubmission;
import com.ww.app.redis.component.RedissonComponent;
import com.ww.app.redis.component.stock.StockRedisComponent;
import com.ww.mall.coupon.component.CouponComponent;
import com.ww.mall.coupon.component.SmsCouponStatisticsComponent;
import com.ww.mall.coupon.component.key.CouponRedisKeyBuilder;
import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.constant.CouponLuaConstant;
import com.ww.mall.coupon.convert.CouponConvert;
import com.ww.mall.coupon.entity.*;
import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import com.ww.mall.coupon.enums.*;
import com.ww.mall.coupon.service.SmsCouponService;
import com.ww.mall.coupon.service.confirm.CouponEvaluator;
import com.ww.mall.coupon.service.strategy.CouponBucket;
import com.ww.mall.coupon.service.strategy.DefaultCouponSelectStrategy;
import com.ww.mall.coupon.service.strategy.SelectionContext;
import com.ww.mall.coupon.service.strategy.SelectionResult;
import com.ww.mall.coupon.utils.CouponCacheComponent;
import com.ww.mall.coupon.utils.CouponCodeGenerator;
import com.ww.mall.coupon.utils.CouponUtils;
import com.ww.mall.coupon.view.bo.*;
import com.ww.mall.coupon.view.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RScript;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import static com.ww.app.common.utils.CollectionUtils.*;
import static com.ww.mall.coupon.constant.LogRecordConstants.*;

/**
 * @author ww
 * @create 2023-07-25- 10:20
 * @description:
 */
@Slf4j
@Service
public class SmsCouponServiceImpl implements SmsCouponService {

    private static final int BATCH_NUMBER = 1000;

    @Resource
    private CouponCacheComponent couponCacheComponent;

    @Resource
    private SmsCouponService smsCouponService;

    @Resource
    private ExecutorService defaultThreadPoolExecutor;

    @Resource
    private CouponComponent couponComponent;

    @Resource
    private SmsCouponStatisticsComponent smsCouponStatisticsComponent;

    @Resource
    private CouponRedisKeyBuilder couponRedisKeyBuilder;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedissonComponent redissonComponent;

    @Resource
    private StockRedisComponent stockRedisComponent;

    @Resource
    private AppRedisTemplate appRedisTemplate;

    @Resource
    private DefaultCouponSelectStrategy defaultCouponSelectStrategy;

    @Resource
    private CouponEvaluator couponEvaluator;

    private String convertCouponCodeSha1;

    @PostConstruct
    public void init() {
        convertCouponCodeSha1 = redissonComponent.loadLuaScript(CouponLuaConstant.CONVERT_COUPON_CODE_LUA);
    }

    @Override
    public AppPageResult<SmsCouponPageVO> pageList(SmsCouponPageBO smsCouponPageBO) {
        return smsCouponPageBO.simplePageConvertResult(smsCouponActivity -> {
            SmsCouponPageVO vo = CouponConvert.INSTANCE.convert4(smsCouponActivity);
            int availableNumber = 0;
            switch (vo.getIssueType()) {
                case RECEIVE:
                case ADMIN_ISSUE:
                    availableNumber = stockRedisComponent.getStrStock(couponRedisKeyBuilder.buildCouponNumberKey(smsCouponActivity.getActivityCode()));
                    break;
                case API_ISSUE:
                case EXPORT_ISSUE:
                    Set<String> couponCodeKeys = getCouponCodeKeys(smsCouponActivity.getActivityCode());
                    for (String key : couponCodeKeys) {
                        RSet<Object> codeSet = redissonClient.getSet(key);
                        int size = codeSet.size();
                        if (size > 0 && !codeSet.contains(CouponConstant.DEFAULT_CODE)) {
                            availableNumber += size;
                        }
                    }
                    break;
                default:
            }
            vo.setAvailableNumber(availableNumber);
            // 领取、使用数据处理【多实例下一致性有延迟】
            int receiveNumber = smsCouponStatisticsComponent.getStatisticsReceiveMap().getOrDefault(vo.getActivityCode(), new LongAdder()).intValue();
            int useNumber = smsCouponStatisticsComponent.getStatisticsUseMap().getOrDefault(vo.getActivityCode(), new LongAdder()).intValue();
            vo.setReceiveNumber(vo.getReceiveNumber() + receiveNumber);
            vo.setUseNumber(vo.getUseNumber() + useNumber);
            return vo;
        });
    }

    @Override
    public List<SmsCouponCodeListVO> codeList(SmsCouponCodeListBO smsCouponCodeListBO) {
        List<SmsCouponCode> simpleQuerySizeResult = smsCouponCodeListBO.simpleQuerySizeResult(SmsCouponCode.buildCollectionName(smsCouponCodeListBO.getChannelId()));
        String smsCouponRecordCollectionName = SmsCouponRecord.buildCollectionName(smsCouponCodeListBO.getChannelId());
        if (CollectionUtils.isEmpty(simpleQuerySizeResult)) {
            return Collections.emptyList();
        }
        Map<String, SmsCouponRecord> recordMap = batchQueryCouponRecordMap(simpleQuerySizeResult, smsCouponRecordCollectionName);
        List<CouponStatusUpdate> statusUpdates = new ArrayList<>();
        List<SmsCouponCodeListVO> result = convertList(simpleQuerySizeResult,
                res -> convertSmsCouponCodeListVO(res, smsCouponCodeListBO.getCouponStatus(), recordMap, statusUpdates));
        // 异步批量更新状态
        asyncBatchUpdateCouponStatus(smsCouponRecordCollectionName, statusUpdates);
        return result;
    }

    @NotNull
    private SmsCouponCodeListVO convertSmsCouponCodeListVO(SmsCouponCode res, CouponStatus couponStatus,
                                                          Map<String, SmsCouponRecord> recordMap,
                                                          List<CouponStatusUpdate> statusUpdates) {
        SmsCouponCodeListVO vo = new SmsCouponCodeListVO();
        vo.setCode(res.getCode());
        vo.setBatchNo(res.getBatchNo());
        vo.setMemberId(res.getUserId());
        vo.setReceiveTime(vo.getMemberId() == null ? null : res.getUpdateTime());
        vo.setCouponStatus(vo.getMemberId() == null ? CouponStatus.WAIT : couponStatus);
        // 状态更新
        if (vo.getMemberId() != null) {
            SmsCouponRecord couponRecord = recordMap.get(vo.getCode());
            if (couponRecord == null) {
                return vo;
            }
            Date now = new Date();
            CouponStatus currentStatus = couponRecord.getCouponStatus();
            CouponStatus newStatus = currentStatus;
            if (CouponStatus.TO_TAKE_EFFECT.equals(currentStatus)) {
                if (couponRecord.getUseStartTime().before(now) && couponRecord.getUseEndTime().after(now)) {
                    newStatus = CouponStatus.IN_EFFECT;
                } else if (couponRecord.getUseEndTime().before(now)) {
                    newStatus = CouponStatus.EXPIRED;
                }
            } else if (CouponStatus.IN_EFFECT.equals(currentStatus)) {
                if (couponRecord.getUseEndTime().before(now)) {
                    newStatus = CouponStatus.EXPIRED;
                }
            }
            if (!currentStatus.equals(newStatus)) {
                statusUpdates.add(new CouponStatusUpdate(vo.getCode(), currentStatus, newStatus));
            }
            vo.setVerificationTime(couponRecord.getUpdateTime());
            vo.setCouponStatus(newStatus);
        }
        return vo;
    }

    @Resource
    private ExcelMinioTemplate excelMinioTemplate;

    @Override
    @TimeCost
    @Resubmission
    @LogRecord(type = SYSTEM_COUPON_TYPE, subType = SYSTEM_COUPON_EXPORT_SUB_TYPE, bizNo = "{{#smsCouponCodeListBO.activityCode}}", success = SYSTEM_COUPON_EXPORT_SUCCESS)
    public String exportCouponCode(SmsCouponCodeListBO smsCouponCodeListBO) {
        log.info("导出优惠券券码[{}]", JSON.toJSON(smsCouponCodeListBO));
        final String couponCodeCollectionName = SmsCouponCode.buildCollectionName(smsCouponCodeListBO.getChannelId());
        long totalCount = mongoTemplate.count(new Query().addCriteria(smsCouponCodeListBO.buildQuery()), SmsCouponCode.class, couponCodeCollectionName);
        if (totalCount == 0) {
            return null;
        }
        final String smsCouponRecordCollectionName = SmsCouponRecord.buildCollectionName(smsCouponCodeListBO.getChannelId());
        return excelMinioTemplate.exportDataToMinio("coupon-code-export", (int) totalCount, 5000, (pageNum, pageSize) -> {
            List<SmsCouponCode> resultList = smsCouponCodeListBO.getSimpleDataResult(couponCodeCollectionName, pageNum + 1, pageSize);
            if (CollectionUtils.isEmpty(resultList)) {
                return Collections.emptyList();
            }
            Map<String, SmsCouponRecord> recordMap = batchQueryCouponRecordMap(resultList, smsCouponRecordCollectionName);
            List<CouponStatusUpdate> statusUpdates = new ArrayList<>();
            List<SmsCouponCodeListVO> list = resultList.stream()
                    .map(res -> convertSmsCouponCodeListVO(res, smsCouponCodeListBO.getCouponStatus(), recordMap, statusUpdates))
                    .collect(Collectors.toList());
            // 异步批量更新状态
            asyncBatchUpdateCouponStatus(smsCouponRecordCollectionName, statusUpdates);
            return list;
        });
    }

    private Map<String, SmsCouponRecord> batchQueryCouponRecordMap(List<SmsCouponCode> couponCodeList, String collectionName) {
        List<String> codes = convertList(couponCodeList, SmsCouponCode::getCode);
        if (CollectionUtils.isEmpty(codes)) {
            return Collections.emptyMap();
        }
        List<SmsCouponRecord> records = mongoTemplate.find(SmsCouponRecord.buildCodeBatchQuery(codes), SmsCouponRecord.class, collectionName);
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }

        return convertMap(records, SmsCouponRecord::getCouponCode);
    }

    private void asyncBatchUpdateCouponStatus(String collectionName, List<CouponStatusUpdate> updates) {
        if (CollectionUtils.isEmpty(updates)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SmsCouponRecord.class, collectionName);
                for (CouponStatusUpdate update : updates) {
                    bulkOps.updateOne(SmsCouponRecord.buildCodeAndStatusQuery(update.code, update.oldStatus), SmsCouponRecord.buildStatusUpdate(update.newStatus));
                }
                bulkOps.execute();
            } catch (Exception e) {
                log.error("批量更新券码状态失败: size={}", updates.size(), e);
            }
        }, defaultThreadPoolExecutor);
    }

    private static class CouponStatusUpdate {
        private final String code;
        private final CouponStatus oldStatus;
        private final CouponStatus newStatus;

        private CouponStatusUpdate(String code, CouponStatus oldStatus, CouponStatus newStatus) {
            this.code = code;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }
    }

    @Override
    @Resubmission
    @LogRecord(type = SYSTEM_COUPON_TYPE, subType = SYSTEM_COUPON_CREATE_SUB_TYPE, bizNo = "{{#smsCouponActivity.activityCode}}", success = SYSTEM_COUPON_CREATE_SUCCESS)
    public boolean add(SmsCouponActivityAddBO smsCouponActivityAddBO) {
        // 生成优惠券记录 - 使用转换方法
        SmsCouponActivity smsCouponActivity = smsCouponActivityAddBO.convertSmsCouponActivity();
        smsCouponActivity = mongoTemplate.save(smsCouponActivity);
        
        // 生成优惠券数量记录
        switch (smsCouponActivity.getIssueType()) {
            case RECEIVE:
            case ADMIN_ISSUE:
                stockRedisComponent.initStrStock(couponRedisKeyBuilder.buildCouponNumberKey(smsCouponActivity.getActivityCode()), smsCouponActivity.getNumber());
                break;
            case API_ISSUE:
            case EXPORT_ISSUE:
                generateSmsCouponCode(smsCouponActivity, buildBatchNo(CouponConstant.DEFAULT_BATCH_NO), smsCouponActivity.getNumber());
                break;
            default:
        }
        // 记录操作日志上下文
        LogRecordContext.putVariable("smsCouponActivity", smsCouponActivity);
        return true;
    }

    @Override
    @Resubmission
    @LogRecord(type = SYSTEM_COUPON_TYPE, subType = SYSTEM_COUPON_UPDATE_SUB_TYPE, bizNo = "{{#smsCouponActivityEditBO.activityCode}}", success = SYSTEM_COUPON_UPDATE_SUCCESS)
    public boolean edit(SmsCouponActivityEditBO smsCouponActivityEditBO) {
        SmsCouponActivity smsCouponActivity = mongoTemplate.findOne(BaseCouponInfo.buildActivityCodeQuery(smsCouponActivityEditBO.getActivityCode(), smsCouponActivityEditBO.getChannelId()), SmsCouponActivity.class);
        Assert.notNull(smsCouponActivity, () -> new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY));
        // 判断是否开始
        UpdateResult updateResult;
        if (smsCouponActivity.getReceiveStartTime().before(new Date())) {
            // 已开始
            updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildActivityCodeQuery(smsCouponActivityEditBO.getActivityCode(), smsCouponActivityEditBO.getChannelId()), smsCouponActivityEditBO.buildInfoUpdate(), SmsCouponActivity.class);
        } else {
            // 未开始
            updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildActivityCodeQuery(smsCouponActivityEditBO.getActivityCode(), smsCouponActivityEditBO.getChannelId()), smsCouponActivityEditBO.buildWaitStartInfoUpdate(), SmsCouponActivity.class);
        }
        couponCacheComponent.updateSmsCouponActivityCache(smsCouponActivityEditBO.getActivityCode());
        // 记录操作日志上下文
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(smsCouponActivity, SmsCouponActivityEditBO.class));
        LogRecordContext.putVariable("smsCouponActivityName", smsCouponActivity.getName());
        return updateResult.getModifiedCount() == 1;
    }

    @Override
    public SmsCouponDetailVO info(String id) {
        SmsCouponActivity smsCouponActivity = mongoTemplate.findOne(BaseDoc.buildIdQuery(id), SmsCouponActivity.class);
        Assert.notNull(smsCouponActivity, () -> new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY));
        return CouponConvert.INSTANCE.convert3(smsCouponActivity);
    }

    @Override
    @Resubmission
    @LogRecord(type = SYSTEM_COUPON_TYPE, subType = SYSTEM_COUPON_STATUS_SUB_TYPE, bizNo = "{{#couponActivityStatusBO.activityCode}}", success = SYSTEM_COUPON_STATUS_SUCCESS)
    public boolean status(CouponActivityStatusBO couponActivityStatusBO) {
        UpdateResult updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildActivityCodeQuery(couponActivityStatusBO.getActivityCode()), BaseCouponInfo.buildActivityStatusUpdate(couponActivityStatusBO.getStatus()), SmsCouponActivity.class);
        couponCacheComponent.updateSmsCouponActivityCache(couponActivityStatusBO.getActivityCode());
        // 记录操作日志上下文
        LogRecordContext.putVariable("newStatus", couponActivityStatusBO.getStatus());
        LogRecordContext.putVariable("activityCode", couponActivityStatusBO.getActivityCode());
        return updateResult.getModifiedCount() == 1;
    }

    @Resource
    private MongoBulkDataHandler<SmsCouponCode> smsCouponCodeMongoBulkDataHandler;

    /**
     * 生成平台优惠券券码
     *
     * @param smsCouponActivity 优惠券活动
     * @param batchNo           批次号
     * @return 生成券码数量
     */
    private int generateSmsCouponCode(SmsCouponActivity smsCouponActivity, String batchNo, int codeNumber) {
        List<SmsCouponCode> smsCouponCodeDocs = new ArrayList<>();
        Set<String> smsCouponCodes = new HashSet<>();
        while (smsCouponCodes.size() < codeNumber) {
            String code = CouponCodeGenerator.generate();
            smsCouponCodes.add(code);
            smsCouponCodeDocs.add(new SmsCouponCode(smsCouponActivity.getActivityCode(), batchNo, code));
        }
        try {
            RSet<String> codeRSet = redissonClient.getSet(couponRedisKeyBuilder.buildCouponCodeKey(smsCouponActivity.getActivityCode(), batchNo));
            boolean success = codeRSet.addAll(smsCouponCodes);
            Assert.isTrue(success, () -> new ApiException(ErrorCodeConstants.COUPON_ERROR));
            // 是否插入mongodb 根据channelId 分code表
            int insertCount = smsCouponCodeMongoBulkDataHandler.bulkSave(smsCouponCodeDocs, SmsCouponCode.buildCollectionName(smsCouponActivity.getChannelId()));
            log.info("优惠券活动[{}]生成优惠券券码数量[{}]", smsCouponActivity.getActivityCode(), insertCount);
            return insertCount;
        } catch (Exception e) {
            log.error("生成优惠券券码异常", e);
            throw new ApiException(ErrorCodeConstants.COUPON_ERROR);
        }
    }

    @Override
    @DistributedLock(enableUserLock = true)
    public boolean receiveCoupon(String activityCode) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        log.info("用户[{}]领取优惠券活动[{}]", clientUser.getId(), activityCode);
        BaseCouponInfo baseCouponInfo = null;
        switch (CouponUtils.getCouponType(activityCode)) {
            case PLATFORM:
                SmsCouponActivity smsCouponActivity = getSmsCouponActivity(activityCode);
                if (!smsCouponActivity.getChannelId().equals(clientUser.getChannelId())) {
                    throw new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY);
                }
                baseCouponInfo = smsCouponActivity;
                break;
            case MERCHANT:
                MerchantCouponActivity merchantCouponActivity = getMerchantCouponActivity(activityCode);
                if (!merchantCouponActivity.getChannelIds().contains(clientUser.getChannelId())) {
                    throw new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY);
                }
                baseCouponInfo = merchantCouponActivity;
                break;
        }
        if (IssueType.EXPORT_ISSUE.equals(baseCouponInfo.getIssueType()) || IssueType.API_ISSUE.equals(baseCouponInfo.getIssueType())) {
            throw new ApiException(GlobalResCodeConstants.ILLEGAL_REQUEST);
        }
        // 活动校验
        validSmsCouponActivity(baseCouponInfo);
        // 用户优惠券领取次数限制校验
        checkUserReceiveLimit(clientUser.getId(), clientUser.getChannelId(), baseCouponInfo);
        // 库存校验
        Assert.isTrue(stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(activityCode), 1), () -> new ApiException(ErrorCodeConstants.COUPON_SALE_OUT));
        try {
            // 构建用户领取优惠券记录
            SmsCouponRecord smsCouponRecord = buildSmsCouponRecord(clientUser.getId(), baseCouponInfo);
            smsCouponRecord.setCouponType(CouponUtils.getCouponType(activityCode));
            smsCouponRecord.setCouponCode(StrUtil.EMPTY);
            mongoTemplate.save(smsCouponRecord, SmsCouponRecord.buildCollectionName(clientUser.getChannelId()));
            log.info("用户[{}]成功领取优惠券[{}]", clientUser.getId(), smsCouponRecord);
        } catch (Exception e) {
            log.error("用户[{}]领取优惠券[{}]异常，进行回滚活动库存", clientUser.getId(), activityCode);
            boolean flag = stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(activityCode), -1);
            log.error("回滚优惠券活动[{}]库存结果[{}]", activityCode, flag);
            return false;
        }
        // 统计领取数据
        smsCouponStatisticsComponent.statisticsCouponReceive(activityCode);
        return true;
    }

    @Resource
    private MongoBulkDataHandler<SmsCouponRecord> mongoBulkCouponRecordDataHandler;

    /**
     * 批量发放优惠券
     *
     * @param activityCode    活动编码
     * @param issueUserIdList 发放用户id集合
     * @return boolean
     */
    public boolean batchIssueCoupon(String activityCode, List<Long> issueUserIdList) {
        log.info("[批量发放优惠券]优惠券活动[{}]用户数量[{}]", activityCode, issueUserIdList.size());
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(activityCode);
        if (!IssueType.ADMIN_ISSUE.equals(smsCouponActivity.getIssueType())) {
            throw new ApiException(GlobalResCodeConstants.ILLEGAL_REQUEST);
        }
        // 活动校验
        validSmsCouponActivity(smsCouponActivity);
        // 用户优惠券领取次数限制校验
        List<Long> targetIssueUserIdList = new ArrayList<>();
        issueUserIdList.forEach(userId -> {
            try {
                checkUserReceiveLimit(userId, smsCouponActivity.getChannelId(), smsCouponActivity);
                targetIssueUserIdList.add(userId);
            } catch (Exception e) {
                log.error("[批量发放优惠券]优惠券活动[{}]用户[{}]已经发放优惠券", activityCode, userId);
            }
        });
        if (CollectionUtils.isEmpty(targetIssueUserIdList)) {
            return true;
        }
        // 库存校验
        Assert.isTrue(stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(activityCode), targetIssueUserIdList.size()), () -> new ApiException(ErrorCodeConstants.COUPON_STOCK_LESS));
        // 发放优惠券
        List<SmsCouponRecord> smsIssueCouponRecordList = new ArrayList<>();
        String collectionName = SmsCouponRecord.buildCollectionName(smsCouponActivity.getChannelId());
        int issueSuccessSize;
        try {
            targetIssueUserIdList.forEach(userId -> {
                // 构建用户领取优惠券记录
                SmsCouponRecord smsCouponRecord = buildSmsCouponRecord(userId, smsCouponActivity);
                smsCouponRecord.setCouponType(CouponUtils.getCouponType(activityCode));
                smsCouponRecord.setCouponCode(StrUtil.EMPTY);
                smsIssueCouponRecordList.add(smsCouponRecord);
            });
            issueSuccessSize = mongoBulkCouponRecordDataHandler.bulkSave(smsIssueCouponRecordList, collectionName);
            log.info("[批量发放优惠券][{}]发放成功数量[{}]", activityCode, issueSuccessSize);
        } catch (Exception e) {
            log.error("[批量发放优惠券][{}]异常", activityCode, e);
            return false;
        }
        // 统计批量发放数据
        smsCouponStatisticsComponent.statisticsCouponReceive(activityCode, issueSuccessSize);
        return true;
    }

    @Override
    @Resubmission
    public boolean convertCoupon(String couponCode) {
        Assert.isTrue(!CouponConstant.DEFAULT_CODE.equals(couponCode), () -> new ApiException(GlobalResCodeConstants.ILLEGAL_REQUEST));
        ClientUser clientUser = AuthorizationContext.getClientUser();
        log.info("用户[{}]使用券码[{}]兑换优惠券", clientUser.getId(), couponCode);
        // 查询是否存在券码
        SmsCouponCode smsCouponCode = mongoTemplate.findOne(SmsCouponCode.buildCodeQuery(couponCode), SmsCouponCode.class, SmsCouponCode.buildCollectionName(clientUser.getChannelId()));
        Assert.notNull(smsCouponCode, () -> new ApiException(ErrorCodeConstants.INVALID_CODE));
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(smsCouponCode.getActivityCode());
        // 活动校验
        validSmsCouponActivity(smsCouponActivity);
        // 用户优惠券领取次数限制校验
        checkUserReceiveLimit(clientUser.getId(), clientUser.getChannelId(), smsCouponActivity);
        // 进行兑换
        boolean remove = doConvertCouponCode(couponRedisKeyBuilder.buildCouponCodeKey(smsCouponCode.getActivityCode(), smsCouponCode.getBatchNo()), couponCode);
        Assert.isTrue(remove, () -> new ApiException(ErrorCodeConstants.CODE_USED));
        try {
            // 构建用户领取优惠券记录
            SmsCouponRecord smsCouponRecord = buildSmsCouponRecord(clientUser.getId(), smsCouponActivity);
            // 记录券码
            smsCouponRecord.setCouponCode(couponCode);
            // 目前只支持渠道券码
            smsCouponRecord.setCouponType(CouponType.PLATFORM);
            mongoTemplate.save(smsCouponRecord, SmsCouponRecord.buildCollectionName(clientUser.getChannelId()));
            log.info("用户[{}]使用券码[{}]成功兑换优惠券[{}]", clientUser.getId(), couponCode, smsCouponRecord);
            // 更新券码领取用户id
            CompletableFuture.runAsync(() -> {
                // 发送更新券码用户id消息
                try {
                    UpdateResult updateResult = mongoTemplate.updateFirst(SmsCouponCode.buildCodeQuery(couponCode),
                            SmsCouponCode.buildCodeUserIdUpdate(clientUser.getId()),
                            SmsCouponCode.class,
                            SmsCouponCode.buildCollectionName(clientUser.getChannelId()));
                    log.info("更新券码[{}]用户id[{}]结果[{}]", couponCode, clientUser.getId(), updateResult.getModifiedCount());
                } catch (Exception e) {
                    log.error("更新券码用户id异常", e);
                }
            }, defaultThreadPoolExecutor);
        } catch (Exception e) {
            log.error("用户[{}]兑换优惠券[{}]异常，进行回滚活动库存", clientUser.getId(), couponCode);
            RSet<String> codeRSet = redissonClient.getSet(couponRedisKeyBuilder.buildCouponCodeKey(smsCouponActivity.getActivityCode(), smsCouponCode.getBatchNo()));
            boolean flag = codeRSet.add(couponCode);
            log.error("回滚优惠券券码[{}]回滚结果[{}]", couponCode, flag);
            return false;
        }
        // 统计领取数据
        smsCouponStatisticsComponent.statisticsCouponReceive(smsCouponActivity.getActivityCode());
        return true;
    }

    @Override
    public String issueCouponCode(IssueCouponCodeBO issueCouponCodeBO) {
        log.info("[API发放]优惠券券码请求参数: {}", issueCouponCodeBO);
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(issueCouponCodeBO.getActivityCode(), issueCouponCodeBO.getChannelId());
        Assert.isTrue(IssueType.API_ISSUE.equals(smsCouponActivity.getIssueType()), () -> new ApiException(ErrorCodeConstants.COUPON_TYPE_EXCEPTION));
        // 校验活动
        validSmsCouponActivity(smsCouponActivity);
        // 判断发放区是否存在
        String issueCodeKey = couponRedisKeyBuilder.buildCouponBatchCodeIssueKey(issueCouponCodeBO.getActivityCode(), issueCouponCodeBO.getBatchNo());
        if (!appRedisTemplate.hasKey(issueCodeKey)) {
            // 不存在[加锁]放入发放区
            smsCouponService.joinIssueArea(issueCouponCodeBO.getActivityCode(), issueCouponCodeBO.getBatchNo(), issueCouponCodeBO.getChannelId());
        }
        // 从发放区进行发放券码
        RSet<String> issueCodeRSet = redissonClient.getSet(issueCodeKey);
        String issueCode = issueCodeRSet.removeRandom();
        Assert.notEmpty(issueCode, () -> new ApiException(ErrorCodeConstants.OUT_OF_ISSUE_STOCK));
        log.info("[API发放]优惠券券码: {} 发放单号: {}", issueCode, issueCouponCodeBO.getIssueOrderCode());
        return issueCode;
    }

    @Override
    @DistributedLock(operationKey = "issueArea")
    public void joinIssueArea(String activityCode, String batchNo, Long channelId) {
        String issueCodeKey = couponRedisKeyBuilder.buildCouponBatchCodeIssueKey(activityCode, batchNo);
        // double check
        if (appRedisTemplate.hasKey(issueCodeKey)) {
            log.info("活动[{}]批次[{}]已被其他线程加入发放区，直接发放券码", activityCode, batchNo);
            return;
        }
        // 判断是否已经发放完毕
        boolean exists = mongoTemplate.exists(SmsCouponIssueAreaRecord.buildActivityCodeBatchNoQuery(activityCode, batchNo), SmsCouponIssueAreaRecord.class);
        if (exists) {
            log.info("活动[{}]批次[{}]已全部发完", activityCode, batchNo);
            return;
        }
        // 查询批次下所有code
        List<SmsCouponCode> smsCouponCodes = mongoTemplate.find(SmsCouponCode.buildActivityCodeAndBatchNoQuery(activityCode, batchNo), SmsCouponCode.class, SmsCouponCode.buildCollectionName(channelId));
        if (CollectionUtil.isEmpty(smsCouponCodes)) {
            throw new ApiException("当前活动批次券码没有券码");
        }
        List<String> codes = smsCouponCodes.stream().map(SmsCouponCode::getCode).collect(Collectors.toList());
        List<List<String>> codePartitionList = Lists.partition(codes, BATCH_NUMBER);
        RSet<String> issueCodeRSet = redissonClient.getSet(issueCodeKey);
        codePartitionList.forEach(issueCodeRSet::addAll);
        int issueCodeNumber = issueCodeRSet.size();
        if (issueCodeNumber < 1) {
            throw new ApiException("当前活动批次发放券码异常，请联系客服人员");
        }
        if (issueCodeNumber != codes.size()) {
            log.error("活动[{}]批次[{}]券码加入发放区数量不一致", activityCode, batchNo);
        }
        log.info("活动[{}]批次[{}]券码数量[{}]加入发放区[{}]数量", activityCode, batchNo, codes.size(), issueCodeNumber);
        // 记录加入发放区记录
        SmsCouponIssueAreaRecord issueAreaRecord = new SmsCouponIssueAreaRecord();
        issueAreaRecord.setActivityCode(activityCode);
        issueAreaRecord.setBatchNo(batchNo);
        issueAreaRecord.setCodeNumber(codes.size());
        issueAreaRecord.setIssueAreaNumber(issueCodeNumber);
        mongoTemplate.save(issueAreaRecord);
    }

    @Override
    public List<String> queryActivityBatchNoList(SmsCouponActivityBatchNoBO batchNoBO) {
        Set<String> codeKeys = getCouponCodeKeys(batchNoBO.getActivityCode());
        return codeKeys.stream()
                .map(CommonUtils::extractIdFromKey)
                .collect(Collectors.toList());
    }

    @Override
    @DistributedLock(operationKey = "#addCouponCodeBO.activityCode")
    @LogRecord(type = SYSTEM_COUPON_TYPE, subType = SYSTEM_COUPON_ADD_CODE_SUB_TYPE, bizNo = "{{#addCouponCodeBO.activityCode}}", success = SYSTEM_COUPON_ADD_CODE_SUCCESS)
    public boolean addSmsCouponCode(SmsAddCouponCodeBO addCouponCodeBO) {
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(addCouponCodeBO.getActivityCode(), addCouponCodeBO.getChannelId());
        if (addCouponCodeBO.getNumber() + smsCouponActivity.getNumber() > CouponConstant.ACTIVITY_MAX_NUMBER) {
            throw new ApiException(ErrorCodeConstants.EXCEED_BATCH_MAX_NUMBER);
        }
        int generateCodeNumber = addCouponCodeBO.getNumber();
        switch (smsCouponActivity.getIssueType()) {
            case RECEIVE:
            case ADMIN_ISSUE:
                boolean add = stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(smsCouponActivity.getActivityCode()), -addCouponCodeBO.getNumber());
                if (!add) {
                    return false;
                }
                break;
            case API_ISSUE:
            case EXPORT_ISSUE:
                String batchNo = getCouponActivityNextBatchNo(smsCouponActivity.getActivityCode());
                generateCodeNumber = generateSmsCouponCode(smsCouponActivity, batchNo, addCouponCodeBO.getNumber());
                break;
            default:
        }
        try {
            UpdateResult updateResult = mongoTemplate.updateFirst(SmsCouponActivity.buildActivityCodeQuery(smsCouponActivity.getActivityCode()), SmsCouponActivity.buildActivityNumberUpdate(generateCodeNumber), SmsCouponActivity.class);
            log.info("优惠券活动[{}]新增优惠券券码数量[{}]结果[{}]", smsCouponActivity.getActivityCode(), generateCodeNumber, updateResult.getModifiedCount());
            if (updateResult.getModifiedCount() == 0) {
                if (IssueType.RECEIVE.equals(smsCouponActivity.getIssueType())) {
                    boolean rollback = stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(smsCouponActivity.getActivityCode()), addCouponCodeBO.getNumber());
                    if (rollback) {
                        return false;
                    } else {
                        throw new ApiException(ErrorCodeConstants.COUPON_STOCK_SUCCESS_BUT_SHOW_DATA_EXCEPTION);
                    }
                } else {
                    throw new ApiException(ErrorCodeConstants.COUPON_STOCK_SUCCESS_BUT_SHOW_DATA_EXCEPTION);
                }
            }
        } catch (Exception e) {
            log.error("优惠券活动[{}]更新活动数量[{}]失败", smsCouponActivity.getActivityCode(), generateCodeNumber, e);
        }
        couponCacheComponent.updateSmsCouponActivityCache(addCouponCodeBO.getActivityCode());
        // 记录操作日志上下文
        LogRecordContext.putVariable("num", addCouponCodeBO.getNumber());
        LogRecordContext.putVariable("activityCode", smsCouponActivity.getActivityCode());
        return true;
    }

    /**
     * 获取优惠券活动券码下一个批次号
     *
     * @param activityCode 优惠券活动编码
     * @return 下一个批次号
     */
    private String getCouponActivityNextBatchNo(String activityCode) {
        Set<String> codeKeys = getCouponCodeKeys(activityCode);
        // 获取批次号
        Iterator<String> iterator = codeKeys.iterator();
        int maxId = 0;
        while (iterator.hasNext()) {
            String key = iterator.next();
            // 提取 key 中的编号部分
            String batchNo = CommonUtils.extractIdFromKey(key);
            int id = Integer.parseInt(batchNo.split(StrUtil.DASHED)[1]);
            if (id > maxId) {
                maxId = id;
            }
        }
        log.info("获取优惠券活动最新批次号[{}]下一个批次号[{}]", maxId, maxId + 1);
        return buildBatchNo(maxId + 1);
    }

    private String buildBatchNo(int no) {
        return StrUtil.join(StrUtil.DASHED, DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN), no);
    }

    /**
     * 获取优惠券券码所有批次号key
     *
     * @param activityCode 优惠券活动编码
     * @return keys
     */
    private Set<String> getCouponCodeKeys(String activityCode) {
        Set<String> codeKeys = appRedisTemplate.scanKeys(couponRedisKeyBuilder.buildCouponCodeKey(activityCode, "*"));
        if (CollectionUtils.isEmpty(codeKeys)) {
            throw new ApiException(ErrorCodeConstants.DATA_ERROR);
        }
        log.info("获取优惠券活动已有批次key[{}]", codeKeys);
        return codeKeys;
    }

    public void memberCouponStatusHandle(ClientUser clientUser, SmsCouponRecord smsCouponRecord, Date now) {
        if (CouponStatus.OCCUPIED.equals(smsCouponRecord.getCouponStatus()) ||
                CouponStatus.USED.equals(smsCouponRecord.getCouponStatus()) ||
                CouponStatus.EXPIRED.equals(smsCouponRecord.getCouponStatus())) {
            return;
        }
        CouponStatus couponStatus;
        if (now.before(smsCouponRecord.getUseStartTime())) {
            couponStatus = CouponStatus.TO_TAKE_EFFECT;
        } else if (now.before(smsCouponRecord.getUseEndTime())) {
            couponStatus = CouponStatus.IN_EFFECT;
        } else {
            couponStatus = CouponStatus.EXPIRED;
        }
        if (!smsCouponRecord.getCouponStatus().equals(couponStatus)) {
            mongoTemplate.updateFirst(BaseDoc.buildIdQuery(smsCouponRecord.getId()), SmsCouponRecord.buildStatusUpdate(couponStatus), SmsCouponRecord.class, SmsCouponRecord.buildCollectionName(clientUser.getChannelId()));
            log.info("优惠券[{}]更新状态为[{}]", smsCouponRecord.getId(), couponStatus);
            smsCouponRecord.setCouponStatus(couponStatus);
        }
    }

    @Override
    public List<CouponActivityCenterVO> smsCouponActivityCenter(CouponActivityCenterBO bo) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        List<SmsCouponActivity> targetList = getSmsCouponActivityCursorList(SmsCouponActivity.buildSmsCouponCenterQuery(clientUser.getChannelId(), bo.getType()), bo.getEndIdCursorValue(), 10);
        if (CollectionUtils.isEmpty(targetList)) {
            return null;
        }
        return convertList(targetList, res -> {
            CouponActivityCenterVO vo = CouponConvert.INSTANCE.convert(res);
            int availableNumber = stockRedisComponent.getStrStock(couponRedisKeyBuilder.buildCouponNumberKey(res.getActivityCode()));
            // 获取当前优惠券领取数量
            int receiveNumber1 = res.getReceiveNumber();
            int receiveNumber2 = smsCouponStatisticsComponent.getStatisticsReceiveMap().getOrDefault(vo.getActivityCode(), new LongAdder()).intValue();
            // 计算比例（避免除零）
            int totalNumber = receiveNumber1 + receiveNumber2 + availableNumber;
            BigDecimal ratio = totalNumber > 0 ? BigDecimal.valueOf((double)(receiveNumber1 + receiveNumber2) / totalNumber) : BigDecimal.ZERO;
            vo.setRatio(ratio);
            return vo;
        });
    }

    @Override
    public List<MemberCouponCenterVO> memberCouponCenter(MemberCouponCenterBO bo) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        List<SmsCouponRecord> resultList = MongoUtils.descQueryByIdCursor(SmsCouponRecord.buildMemberCouponCenterQuery(clientUser.getId(), bo), bo.getEndIdCursorValue(), bo.getSize(), SmsCouponRecord.class, SmsCouponRecord.buildCollectionName(clientUser.getChannelId()));
        Date now = new Date();
        return convertList(resultList, res -> {
            // 状态实时更新
            memberCouponStatusHandle(clientUser, res, now);
            // 判断是否冻结[特殊异常情况下]
            if (CouponStatus.IN_EFFECT.equals(res.getCouponStatus())) {
                if (couponComponent.isFreezeCoupon(res.getId())) {
                    res.setCouponStatus(CouponStatus.OCCUPIED);
                }
            }
            MemberCouponCenterVO vo = CouponConvert.INSTANCE.convert(res);
            // 查询活动信息
            switch (res.getCouponType()) {
                case PLATFORM:
                    SmsCouponActivity smsCouponActivity = getSmsCouponActivity(res.getActivityCode());
                    vo.setName(smsCouponActivity.getName());
                    vo.setDesc(smsCouponActivity.getDesc());
                    vo.setApplyProductRangeType(smsCouponActivity.getApplyProductRangeType());
                    vo.setIdList(smsCouponActivity.getIdList());
                    break;
                case MERCHANT:
                    MerchantCouponActivity merchantCouponActivity = getMerchantCouponActivity(res.getActivityCode());
                    vo.setName(merchantCouponActivity.getName());
                    vo.setDesc(merchantCouponActivity.getDesc());
                    vo.setApplyProductRangeType(merchantCouponActivity.getApplyProductRangeType());
                    vo.setIdList(merchantCouponActivity.getIdList());
                    vo.setMerchantId(merchantCouponActivity.getMerchantId());
                    break;
                default:
            }
            return vo;
        });
    }

    @Override
    public void expireActivityRedisDataHandle() {
        List<String> removeKeyList = new ArrayList<>();
        String numberPrefixKey = couponRedisKeyBuilder.buildCouponNumberPrefixKey();
        Set<String> numberKeys = appRedisTemplate.scanKeys(numberPrefixKey + "*");
        if (CollectionUtil.isNotEmpty(numberKeys)) {
            numberKeys.forEach(numberKey -> {
                String activityCode = couponRedisKeyBuilder.extractActivityCode(numberKey, numberPrefixKey);
                handleExpireCouponActivity(numberKey, activityCode, removeKeyList);
            });
        }
        String codePrefixKey = couponRedisKeyBuilder.buildCouponCodePrefixKey();
        Set<String> codeKeys = appRedisTemplate.scanKeys(codePrefixKey + "*");
        if (CollectionUtil.isNotEmpty(codeKeys)) {
            codeKeys.forEach(codeKey -> {
                String activityCode = couponRedisKeyBuilder.extractActivityCode(codeKey, codePrefixKey);
                handleExpireCouponActivity(codeKey, activityCode, removeKeyList);
            });
        }
        if (!removeKeyList.isEmpty()) {
            log.info("活动已过期需要清理的redisKey:[{}]", removeKeyList);
            appRedisTemplate.batchRemoveKeys(removeKeyList, true);
        }
    }

    private void handleExpireCouponActivity(String key, String activityCode, List<String> removeKeyList) {
        if (StrUtil.isBlank(activityCode)) {
            return;
        }
        Date now = new Date();
        // 查询活动是否过期
        CouponType couponType = CouponUtils.getCouponType(activityCode);
        switch (couponType) {
            case PLATFORM:
                SmsCouponActivity smsCouponActivity = mongoTemplate.findOne(BaseCouponInfo.buildActivityCodeQuery(activityCode), SmsCouponActivity.class);
                if (smsCouponActivity != null && smsCouponActivity.getReceiveEndTime().before(now)) {
                    log.info("平台优惠券活动[{}]已过期", activityCode);
                    removeKeyList.add(key);
                }
                break;
            case MERCHANT:
                MerchantCouponActivity merchantCouponActivity = mongoTemplate.findOne(BaseCouponInfo.buildActivityCodeQuery(activityCode), MerchantCouponActivity.class);
                if (merchantCouponActivity != null && merchantCouponActivity.getReceiveEndTime().before(now)) {
                    log.info("商家优惠券活动[{}]已过期", activityCode);
                    removeKeyList.add(key);
                }
                break;
            default:
        }
    }

    /**
     * 校验优惠券活动
     *
     * @param baseCouponInfo 活动
     */
    private void validSmsCouponActivity(BaseCouponInfo baseCouponInfo) {
        Date now = new Date();
        // 活动是否正常
        if (Boolean.FALSE.equals(baseCouponInfo.getStatus())) {
            throw new ApiException(ErrorCodeConstants.COUPON_ACTIVITY_DOWN);
        }
        // 是否达到领取时间
        if (now.before(baseCouponInfo.getReceiveStartTime()) || now.after(baseCouponInfo.getReceiveEndTime())) {
            throw new ApiException(ErrorCodeConstants.COUPON_ACTIVITY_CANT_GET);
        }
    }

    /**
     * 校验活动用户领取限制
     *
     * @param userId         用户id
     * @param channelId      渠道id
     * @param baseCouponInfo 优惠券基本信息
     */
    private void checkUserReceiveLimit(Long userId, Long channelId, BaseCouponInfo baseCouponInfo) {
        long memberReceiveCount = mongoTemplate.count(SmsCouponRecord.buildMemberReceiveRecordQuery(userId, baseCouponInfo.getActivityCode(), baseCouponInfo.getLimitReceiveTimeType()), SmsCouponRecord.class, SmsCouponRecord.buildCollectionName(channelId));
        if (memberReceiveCount >= baseCouponInfo.getLimitReceiveNumber()) {
            throw new ApiException(ErrorCodeConstants.EXCEED_RECEIVE_LIMIT);
        }
    }

    /**
     * 构建用户优惠券记录
     *
     * @param userId         用户id
     * @param baseCouponInfo 优惠券基础信息
     * @return 用户优惠券记录
     */
    private SmsCouponRecord buildSmsCouponRecord(Long userId, BaseCouponInfo baseCouponInfo) {
        Date now = new Date();
        SmsCouponRecord smsCouponRecord = new SmsCouponRecord();
        smsCouponRecord.setMemberId(userId);
        smsCouponRecord.setActivityCode(baseCouponInfo.getActivityCode());
        smsCouponRecord.setCouponDiscountType(baseCouponInfo.getCouponDiscountType());
        smsCouponRecord.setAchieveAmount(baseCouponInfo.getAchieveAmount());
        smsCouponRecord.setDeductionAmount(baseCouponInfo.getDeductionAmount());
        // 优惠券有效期生效类型
        switch (baseCouponInfo.getEffectTimeType()) {
            case FIXED:
                smsCouponRecord.setUseStartTime(baseCouponInfo.getUseStartTime());
                smsCouponRecord.setUseEndTime(baseCouponInfo.getUseEndTime());
                break;
            case AFTER_RECEIVING:
                int receiveDay = baseCouponInfo.getReceiveDay();
                int effectNumber = baseCouponInfo.getEffectNumber();
                smsCouponRecord.setUseStartTime(DateUtil.offsetDay(now, receiveDay));
                switch (baseCouponInfo.getEffectTimeUnit()) {
                    case MINUTES:
                        smsCouponRecord.setUseEndTime(DateUtil.offsetMinute(smsCouponRecord.getUseStartTime(), effectNumber));
                        break;
                    case DAY:
                        smsCouponRecord.setUseEndTime(DateUtil.offsetDay(smsCouponRecord.getUseStartTime(), effectNumber));
                        break;
                    default:
                        throw new ApiException(ErrorCodeConstants.DATA_ERROR);
                }
                break;
            default:
                throw new ApiException(ErrorCodeConstants.DATA_ERROR);
        }
        if (now.before(smsCouponRecord.getUseStartTime())) {
            smsCouponRecord.setCouponStatus(CouponStatus.TO_TAKE_EFFECT);
        } else if (now.before(smsCouponRecord.getUseEndTime())) {
            smsCouponRecord.setCouponStatus(CouponStatus.IN_EFFECT);
        } else {
            smsCouponRecord.setCouponStatus(CouponStatus.EXPIRED);
        }
        return smsCouponRecord;
    }

    /**
     * C端获取优惠券活动信息
     *
     * @param activityCode 优惠券活动编码
     * @return 活动
     */
    private SmsCouponActivity getSmsCouponActivity(String activityCode) {
        // 查询优惠券活动
        return couponCacheComponent.getSmsCouponActivityCache(activityCode);
    }

    private SmsCouponActivity getSmsCouponActivity(String activityCode, Long channelId) {
        // 查询优惠券活动
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(activityCode);
        Assert.isTrue(smsCouponActivity.getChannelId().equals(channelId), () -> new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY));
        return smsCouponActivity;
    }

    private final List<String> fields = Arrays.asList("activityCode", "receiveNumber");

    private List<SmsCouponActivity> getSmsCouponActivityCursorList(Query query, String cursorIdValue, int size) {
        List<SmsCouponActivity> resultList = MongoUtils.descQueryByIdCursorForFields(query, cursorIdValue, size, fields, SmsCouponActivity.class);
        if (CollectionUtils.isEmpty(resultList)) {
            return null;
        }
        List<String> activityCodeList = resultList.stream().map(BaseCouponInfo::getActivityCode).collect(Collectors.toList());
        List<SmsCouponActivity> targetList = new ArrayList<>();
        Map<String, Integer> resMap = resultList.stream().collect(Collectors.toMap(SmsCouponActivity::getActivityCode, SmsCouponActivity::getReceiveNumber));

        activityCodeList.forEach(activityCode -> {
            try {
                SmsCouponActivity smsCouponActivity = getSmsCouponActivity(activityCode);
                Integer receiveNumber = resMap.get(activityCode);
                smsCouponActivity.setReceiveNumber(receiveNumber);
                targetList.add(smsCouponActivity);
            } catch (Exception e) {
                log.error("查询平台优惠券活动异常", e);
            }
        });
        return targetList;
    }

    /**
     * C端获取商家优惠券活动信息
     *
     * @param activityCode 优惠券活动编码
     * @return 活动
     */
    private MerchantCouponActivity getMerchantCouponActivity(String activityCode) {
        // 查询优惠券活动
        return couponCacheComponent.getMerchantCouponActivityCache(activityCode);
    }

    /**
     * 兑换优惠券券码
     *
     * @return boolean
     */
    private boolean doConvertCouponCode(String couponCodeKey, String couponCode) {
        Long res = redissonComponent.evalLuaBySha(
                convertCouponCodeSha1,   // 脚本的 SHA1 校验和
                RScript.ReturnType.INTEGER, // 返回值类型
                Collections.singletonList(couponCodeKey), // KEYS
                couponCode, CouponConstant.DEFAULT_CODE   // ARGV
        );
        return res != null && res > 0;
    }

    /**
     * 获取适用spu所有的平台优惠券活动
     *
     * @param channelId 渠道id
     * @param type      是否区分积分现金券
     * @param smsId     渠道商品id
     */
    public List<ProductCouponActivityVO> getSpuCouponActivityList(Long channelId, CouponConstant.Type type, Long smsId) {
        // 查询前30个适用商品平台优惠券活动【进行排序】
        List<SmsCouponActivity> targetList = getSmsCouponActivityCursorList(SmsCouponActivity.buildSpuQuery(channelId, type, smsId), null, 30);
        if (CollectionUtils.isEmpty(targetList)) {
            return null;
        }
        List<SmsCouponActivity> integralCouponActivityList = filterList(targetList, res -> res.getCouponDiscountType().equals(CouponDiscountType.INTEGRAL_DISCOUNT));
        List<SmsCouponActivity> cashCouponActivityList = filterList(targetList, res -> !res.getCouponDiscountType().equals(CouponDiscountType.INTEGRAL_DISCOUNT));

        List<ProductCouponActivityVO> sortedIntegralCouponActivityTagList = integralCouponActivityList.stream()
                .sorted(Comparator.comparing(SmsCouponActivity::getDeductionAmount).reversed())
                .map(CouponConvert.INSTANCE::convert2)
                .collect(Collectors.toList());

        List<ProductCouponActivityVO> sortedCashCouponActivityTagList = cashCouponActivityList.stream()
                .sorted((e1, e2) -> {
                    BigDecimal target1 = e1.getDeductionAmount();
                    BigDecimal target2 = e2.getDeductionAmount();
                    if (CouponDiscountType.FULL_DISCOUNT.equals(e1.getCouponDiscountType())) {
                        BigDecimal minPayAmount = e1.getAchieveAmount().multiply(e1.getDeductionAmount()).setScale(2, RoundingMode.HALF_UP);
                        target1 = e1.getAchieveAmount().subtract(minPayAmount);
                    }
                    if (CouponDiscountType.FULL_DISCOUNT.equals(e2.getCouponDiscountType())) {
                        BigDecimal minPayAmount = e2.getAchieveAmount().multiply(e2.getDeductionAmount()).setScale(2, RoundingMode.HALF_UP);
                        target2 = e2.getAchieveAmount().subtract(minPayAmount);
                    }
                    return target2.compareTo(target1);
                }).map(CouponConvert.INSTANCE::convert2)
                .collect(Collectors.toList());
        sortedIntegralCouponActivityTagList.addAll(sortedCashCouponActivityTagList);
        return sortedIntegralCouponActivityTagList;
    }

    /**
     * 获取确认下单用户优惠券信息
     *
     * @param orderBOList         下单商品BO
     * @param memberSmsCouponList 用户有效平台优惠券集合
     * @return ConfirmOrderCouponVO
     */
    public ConfirmOrderCouponVO getOrderMemberCouponList(List<MemberCouponCenterVO> memberSmsCouponList,
                                                         List<OrderMemberSmsCouponBO> orderBOList) {
        return getOrderMemberCouponList(memberSmsCouponList, orderBOList, Boolean.TRUE, null, Boolean.TRUE, null);
    }

    /**
     * 获取确认下单用户优惠券信息（支持指定券/不使用券/默认最优）
     *
     * @param orderBOList                   下单商品BO（方法内部可能基于平台券均摊直接修改）
     * @param memberSmsCouponList           用户有效优惠券集合（含平台券/商家券）
     * @param usePlatformCoupon             是否使用平台优惠券，false 表示平台不选券
     * @param selectedPlatformActivityCode  用户指定的平台优惠券活动编码（为空则默认最优）
     * @param useMerchantCoupon             是否使用商家优惠券，false 表示商家不选券
     * @param selectedMerchantActivityCodeMap 用户指定的商家优惠券活动编码（merchantId -> activityCode），
     *                                        仅传部分商家时，其它商家不默认选最优券
     * @return ConfirmOrderCouponVO
     */
    public ConfirmOrderCouponVO getOrderMemberCouponList(List<MemberCouponCenterVO> memberSmsCouponList,
                                                         List<OrderMemberSmsCouponBO> orderBOList,
                                                         Boolean usePlatformCoupon,
                                                         String selectedPlatformActivityCode,
                                                         Boolean useMerchantCoupon,
                                                         Map<Long, String> selectedMerchantActivityCodeMap) {
        if (CollectionUtils.isEmpty(memberSmsCouponList)) {
            return null;
        }
        // 一：可用优惠券【1.满足使用条件 2.不满足使用条件】
        List<OrderMemberCouponVO> availableIntegralCouponList = new ArrayList<>();
        List<OrderMemberCouponVO> availableCashCouponList = new ArrayList<>();
        // 二：不可用优惠券【1.未到使用周期范围内 2.下单商品没有适用优惠券】
        List<OrderMemberCouponVO> unAvailableIntegralCouponList = new ArrayList<>();
        List<OrderMemberCouponVO> unAvailableCashCouponList = new ArrayList<>();

        ConfirmOrderCouponVO result = new ConfirmOrderCouponVO();
        // 过滤掉参与活动不能使用优惠券的商品
        orderBOList = orderBOList.stream().filter(OrderMemberSmsCouponBO::isActivityUseCoupon).collect(Collectors.toList());
        // 平台优先，商家其次
        List<MemberCouponCenterVO> platformCouponList = filterList(memberSmsCouponList, res -> isPlatformCoupon(res.getCouponType()));
        List<MemberCouponCenterVO> merchantCouponList = filterList(memberSmsCouponList, res -> isMerchantCoupon(res.getCouponType()));

        // 基于原始订单金额计算平台可用/不可用列表
        CouponBucket platformBucket = couponEvaluator.buildBucket(platformCouponList, orderBOList);
        result.setPlatformAvailable(buildCouponGroup(platformBucket.getAvailableIntegralList(), platformBucket.getAvailableCashList()));

        // 默认选中：平台最优 + 各商家最优（商家券需基于平台券均摊后的订单重新计算）
        SelectionContext selectionContext = new SelectionContext(
                couponEvaluator.buildAllAvailableList(platformBucket),
                orderBOList,
                couponEvaluator.getCouponComparator(),
                // 2.选中平台优惠券后，基于平台券已抵扣后的商家优惠券真实可用性
                adjustedOrderBOList -> couponEvaluator.buildBucket(merchantCouponList, adjustedOrderBOList),
                usePlatformCoupon,
                selectedPlatformActivityCode,
                useMerchantCoupon,
                selectedMerchantActivityCodeMap
        );
        SelectionResult selectionResult = defaultCouponSelectStrategy.select(selectionContext);
        List<OrderMemberCouponVO> selectedCouponList = selectionResult.getSelectedCoupons();
        result.setSelectedCouponList(selectedCouponList);

        CouponBucket merchantBucket = selectionResult.getMerchantBucket();
        if (merchantBucket != null) {
            result.setMerchantAvailable(buildCouponGroup(merchantBucket.getAvailableIntegralList(), merchantBucket.getAvailableCashList()));
        } else {
            result.setMerchantAvailable(buildCouponGroup(Collections.emptyList(), Collections.emptyList()));
        }

        mergeBucketLists(platformBucket, availableIntegralCouponList, availableCashCouponList,
                unAvailableIntegralCouponList, unAvailableCashCouponList);
        if (merchantBucket != null) {
            mergeBucketLists(merchantBucket, availableIntegralCouponList, availableCashCouponList,
                    unAvailableIntegralCouponList, unAvailableCashCouponList);
        }

        result.setAvailable(buildCouponGroup(availableIntegralCouponList, availableCashCouponList));
        result.setUnavailable(buildCouponGroup(unAvailableIntegralCouponList, unAvailableCashCouponList));
        return result;
    }

    /**
     * 合并分桶结果到总列表
     */
    private void mergeBucketLists(CouponBucket bucket,
                                  List<OrderMemberCouponVO> availableIntegralCouponList,
                                  List<OrderMemberCouponVO> availableCashCouponList,
                                  List<OrderMemberCouponVO> unAvailableIntegralCouponList,
                                  List<OrderMemberCouponVO> unAvailableCashCouponList) {
        availableIntegralCouponList.addAll(bucket.getAvailableIntegralList());
        availableCashCouponList.addAll(bucket.getAvailableCashList());
        unAvailableIntegralCouponList.addAll(bucket.getUnAvailableIntegralList());
        unAvailableCashCouponList.addAll(bucket.getUnAvailableCashList());
    }

    /**
     * 组装优惠券分组（积分券/现金券）
     */
    private ConfirmOrderCouponVO.CouponGroupVO buildCouponGroup(List<OrderMemberCouponVO> integralList,
                                                                List<OrderMemberCouponVO> cashList) {
        ConfirmOrderCouponVO.CouponGroupVO group = new ConfirmOrderCouponVO.CouponGroupVO();
        group.setIntegral(integralList);
        group.setCash(cashList);
        return group;
    }

}
