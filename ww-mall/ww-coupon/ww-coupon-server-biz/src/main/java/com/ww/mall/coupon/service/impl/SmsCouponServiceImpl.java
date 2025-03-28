package com.ww.mall.coupon.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson.JSON;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.UpdateResult;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.CommonUtils;
import com.ww.app.common.utils.MoneyUtils;
import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.excel.ExcelTemplate;
import com.ww.app.excel.annotation.ExcelExportTimer;
import com.ww.app.minio.MinioTemplate;
import com.ww.app.mongodb.common.BaseDoc;
import com.ww.app.mongodb.handler.MongoBulkDataHandler;
import com.ww.app.mongodb.utils.MongoUtils;
import com.ww.app.redis.AppRedisTemplate;
import com.ww.app.redis.annotation.DistributedLock;
import com.ww.app.redis.annotation.Resubmission;
import com.ww.app.redis.component.StockRedisComponent;
import com.ww.mall.coupon.component.CouponComponent;
import com.ww.mall.coupon.component.SmsCouponStatisticsComponent;
import com.ww.mall.coupon.component.key.CouponRedisKeyBuilder;
import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.constant.CouponLuaConstant;
import com.ww.mall.coupon.entity.MerchantCouponActivity;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.entity.SmsCouponCode;
import com.ww.mall.coupon.entity.SmsCouponRecord;
import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import com.ww.mall.coupon.enums.CouponResCodeConstants;
import com.ww.mall.coupon.eunms.CouponDiscountType;
import com.ww.mall.coupon.eunms.CouponStatus;
import com.ww.mall.coupon.eunms.CouponType;
import com.ww.mall.coupon.eunms.IssueType;
import com.ww.mall.coupon.service.SmsCouponService;
import com.ww.mall.coupon.utils.CouponCacheUtils;
import com.ww.mall.coupon.utils.CouponUtils;
import com.ww.mall.coupon.view.bo.*;
import com.ww.mall.coupon.view.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RScript;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import static com.ww.app.common.utils.CollectionUtils.convertList;
import static com.ww.app.common.utils.CollectionUtils.filterList;

/**
 * @author ww
 * @create 2023-07-25- 10:20
 * @description:
 */
@Slf4j
@Service
public class SmsCouponServiceImpl implements SmsCouponService {

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
    private StockRedisComponent stockRedisComponent;

    @Resource
    private AppRedisTemplate appRedisTemplate;

    private String convertCouponCodeSha1;

    @PostConstruct
    public void init() {
        loadLuaScript();
    }

    private void loadLuaScript() {
        RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
        convertCouponCodeSha1 = rScript.scriptLoad(CouponLuaConstant.CONVERT_COUPON_CODE_LUA);
    }

    @Override
    public AppPageResult<SmsCouponPageVO> pageList(SmsCouponPageBO smsCouponPageBO) {
        return smsCouponPageBO.simplePageConvertResult(smsCouponActivity -> {
            SmsCouponPageVO vo = BeanUtil.toBean(smsCouponActivity, SmsCouponPageVO.class);
            int availableNumber = 0;
            switch (vo.getIssueType()) {
                case RECEIVE:
                case ADMIN_ISSUE:
                    availableNumber = stockRedisComponent.getStrStock(couponRedisKeyBuilder.buildCouponNumberKey(smsCouponActivity.getActivityCode()));
                    break;
                case EXPORT_ISSUE:
                    Set<String> couponCodeKeys = getCouponCodeKeys(smsCouponActivity.getActivityCode());
                    for (String key : couponCodeKeys) {
                        RSet<String> codeRSet = redissonClient.getSet(key);
                        if (!codeRSet.contains(CouponConstant.DEFAULT_CODE)) {
                            availableNumber += codeRSet.size();
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
            vo.setUsedNumber(vo.getUsedNumber() + useNumber);
            return vo;
        });
    }

    @Override
    public List<SmsCouponCodeListVO> codeList(SmsCouponCodeListBO smsCouponCodeListBO) {
        List<SmsCouponCode> simpleQuerySizeResult = smsCouponCodeListBO.simpleQuerySizeResult(SmsCouponCode.buildCollectionName(smsCouponCodeListBO.getChannelId()));
        String smsCouponRecordCollectionName = SmsCouponRecord.buildCollectionName(smsCouponCodeListBO.getChannelId());
        return convertList(simpleQuerySizeResult, res -> convertSmsCouponCodeListVO(res, smsCouponRecordCollectionName));
    }

    @NotNull
    private SmsCouponCodeListVO convertSmsCouponCodeListVO(SmsCouponCode res, String smsCouponRecordCollectionName) {
        SmsCouponCodeListVO vo = new SmsCouponCodeListVO();
        vo.setCode(res.getCode());
        vo.setBatchNo(res.getBatchNo());
        SmsCouponRecord couponRecord = mongoTemplate.findOne(SmsCouponRecord.buildCodeQuery(vo.getCode()), SmsCouponRecord.class, smsCouponRecordCollectionName);
        if (couponRecord != null) {
            vo.setMemberId(couponRecord.getMemberId());
            vo.setReceiveTime(couponRecord.getCreateTime());
            vo.setCouponStatus(couponRecord.getCouponStatus());
            if (CouponStatus.USED.equals(couponRecord.getCouponStatus())) {
                vo.setVerificationTime(couponRecord.getUpdateTime());
            }
        }
        return vo;
    }

    @Resource
    private MinioTemplate minioTemplate;

    @Resource
    private ExcelTemplate excelTemplate;

    private static final ExecutorService exportExecutorService = ThreadUtil.initFixedThreadPoolExecutor("coupon-export-thread", 20);

    @Override
    @Resubmission
    @ExcelExportTimer
    public String exportCouponCode(SmsCouponCodeListBO smsCouponCodeListBO) {
        log.info("导出优惠券券码[{}]", JSON.toJSON(smsCouponCodeListBO));
        String bucket = "coupon-code-export";
        String couponCodeCollectionName = SmsCouponCode.buildCollectionName(smsCouponCodeListBO.getChannelId());
        long totalCount = mongoTemplate.count(new Query().addCriteria(smsCouponCodeListBO.buildQuery()), SmsCouponCode.class, couponCodeCollectionName);
        if (totalCount == 0) {
            return null;
        }
        int perFileSize = 10000;
        int fileNumber = CommonUtils.getCircleNumber((int) totalCount, perFileSize);
        CountDownLatch countDownLatch = new CountDownLatch(fileNumber);

        List<File> exportFiles = new CopyOnWriteArrayList<>();
        File targetFile = null;
        String smsCouponRecordCollectionName = SmsCouponRecord.buildCollectionName(smsCouponCodeListBO.getChannelId());
        try {
            for (int i = 1; i <= fileNumber; i++) {
                final int fileIndex = i;
                CompletableFuture.runAsync(() -> {
                    List<SmsCouponCode> resultList = smsCouponCodeListBO.getSimpleDataResult(couponCodeCollectionName, fileIndex, perFileSize);
                    List<SmsCouponCodeListVO> couponCodeListVO = resultList.stream().map(res -> convertSmsCouponCodeListVO(res, smsCouponRecordCollectionName)).collect(Collectors.toList());
                    // 生成临时文件
                    File file = excelTemplate.exportExcelOfOneSheetToTempFile(couponCodeListVO, fileIndex + StrUtil.EMPTY, UUID.randomUUID().toString(true) + StrUtil.UNDERLINE + fileIndex);
                    exportFiles.add(file);
                    countDownLatch.countDown();
                }, exportExecutorService).exceptionally(e -> {
                    countDownLatch.countDown();
                    throw new RuntimeException("导出临时文件异常", e);
                });
            }
            countDownLatch.await();
            targetFile = ZipUtil.zip(FileUtil.createTempFile(UUID.randomUUID().toString(true), ".zip", true), true, exportFiles.toArray(new File[]{}));
            log.info("压缩文件完成");
            try (FileInputStream inputStream = new FileInputStream(targetFile)) {
                boolean upload = minioTemplate.upload(inputStream, bucket, targetFile.getName());
                log.info("导出压缩文件上传minio结果[{}]", upload);
                if (!upload) {
                    throw new ApiException("上传压缩文件失败");
                }
            }
            log.info("上传压缩文件至Minio完成");
            return minioTemplate.getFileUrl(bucket, targetFile.getName(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (targetFile != null) {
                boolean del = FileUtil.del(targetFile);
                log.info("导出临时压缩文件删除结果[{}]", del);
            }
            if (!exportFiles.isEmpty()) {
                exportFiles.forEach(res -> {
                    boolean del = FileUtil.del(res);
                    log.info("导出临时文件删除结果[{}]", del);
                });
            }
        }
    }

    @Override
    @Resubmission
    public boolean add(SmsCouponActivityAddBO smsCouponActivityAddBO) {
        // 生成优惠券记录
        SmsCouponActivity addSmsCouponActivityInfo = BeanUtil.toBean(smsCouponActivityAddBO, SmsCouponActivity.class);
        addSmsCouponActivityInfo.setActivityCode(CouponUtils.getSmsCouponCode());
        addSmsCouponActivityInfo.setStatus(false);
        addSmsCouponActivityInfo.setChannelId(smsCouponActivityAddBO.getChannelId());
        addSmsCouponActivityInfo.setReceiveNumber(0);
        addSmsCouponActivityInfo.setUseNumber(0);
        SmsCouponActivity smsCouponActivity = mongoTemplate.save(addSmsCouponActivityInfo);
        // 生成优惠券数量记录
        switch (smsCouponActivity.getIssueType()) {
            case RECEIVE:
            case ADMIN_ISSUE:
                stockRedisComponent.initStrStock(couponRedisKeyBuilder.buildCouponNumberKey(smsCouponActivity.getActivityCode()), smsCouponActivity.getNumber());
                break;
            case EXPORT_ISSUE:
                generateSmsCouponCode(smsCouponActivity, CouponConstant.DEFAULT_BATCH_NO, smsCouponActivity.getNumber());
                break;
            default:
        }
        return true;
    }

    @Override
    @Resubmission
    public boolean edit(SmsCouponActivityEditBO smsCouponActivityEditBO) {
        UpdateResult updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildActivityCodeQuery(smsCouponActivityEditBO.getActivityCode(), smsCouponActivityEditBO.getChannelId()), smsCouponActivityEditBO.buildInfoUpdate(), SmsCouponActivity.class);
        CouponCacheUtils.updateSmsCouponActivityCache(smsCouponActivityEditBO.getActivityCode());
        return updateResult.getModifiedCount() == 1;
    }

    @Override
    public SmsCouponDetailVO info(String id) {
        SmsCouponActivity smsCouponActivity = mongoTemplate.findOne(BaseDoc.buildIdQuery(id), SmsCouponActivity.class);
        Assert.notNull(smsCouponActivity, () -> new ApiException(CouponResCodeConstants.UN_FOUND_ACTIVITY));
        return BeanUtil.toBean(smsCouponActivity, SmsCouponDetailVO.class);
    }

    @Override
    @Resubmission
    public boolean status(SmsCouponActivityStatusBO smsCouponActivityStatusBO) {
        UpdateResult updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildActivityCodeQuery(smsCouponActivityStatusBO.getActivityCode(), smsCouponActivityStatusBO.getChannelId()), BaseCouponInfo.buildActivityStatusUpdate(smsCouponActivityStatusBO.getStatus()), SmsCouponActivity.class);
        CouponCacheUtils.updateSmsCouponActivityCache(smsCouponActivityStatusBO.getActivityCode());
        return updateResult.getModifiedCount() == 1;
    }

    private final static MD5 md5 = MD5.create();

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
            String uuid = UUID.randomUUID().toString(true);
            String code = md5.digestHex16(uuid);
            smsCouponCodes.add(code);
            smsCouponCodeDocs.add(new SmsCouponCode(smsCouponActivity.getActivityCode(), smsCouponActivity.getChannelId(), batchNo, code));
        }
        try {
            RSet<String> codeRSet = redissonClient.getSet(couponRedisKeyBuilder.buildCouponCodeKey(smsCouponActivity.getActivityCode(), batchNo));
            codeRSet.addAll(smsCouponCodes);
            // 是否插入mongodb 根据channelId 分code表
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SmsCouponCode.class, SmsCouponCode.buildCollectionName(smsCouponActivity.getChannelId()));
            bulkOps.insert(smsCouponCodeDocs);
            BulkWriteResult bulkWriteResult = bulkOps.execute();
            log.info("优惠券活动[{}]生成优惠券券码数量[{}]", smsCouponActivity.getActivityCode(), bulkWriteResult.getInsertedCount());
            return bulkWriteResult.getInsertedCount();
        } catch (Exception e) {
            log.error("生成优惠券券码异常", e);
            throw new ApiException(CouponResCodeConstants.COUPON_ERROR);
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
                    throw new ApiException(CouponResCodeConstants.UN_FOUND_ACTIVITY);
                }
                baseCouponInfo = smsCouponActivity;
                break;
            case MERCHANT:
                MerchantCouponActivity merchantCouponActivity = getMerchantCouponActivity(activityCode);
                if (!merchantCouponActivity.getChannelIds().contains(clientUser.getChannelId())) {
                    throw new ApiException(CouponResCodeConstants.UN_FOUND_ACTIVITY);
                }
                baseCouponInfo = merchantCouponActivity;
                break;
        }
        if (IssueType.EXPORT_ISSUE.equals(baseCouponInfo.getIssueType())) {
            throw new ApiException(GlobalResCodeConstants.ILLEGAL_REQUEST);
        }
        // 活动校验
        validSmsCouponActivity(baseCouponInfo);
        // 用户优惠券领取次数限制校验
        checkUserReceiveLimit(clientUser.getId(), clientUser.getChannelId(), baseCouponInfo);
        // 库存校验
        Assert.isTrue(stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(activityCode), 1), () -> new ApiException(CouponResCodeConstants.COUPON_SALE_OUT));
        try {
            // 构建用户领取优惠券记录
            SmsCouponRecord smsCouponRecord = buildSmsCouponRecord(clientUser.getId(), clientUser.getChannelId(), baseCouponInfo);
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
        Assert.isTrue(stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(activityCode), targetIssueUserIdList.size()), () -> new ApiException(CouponResCodeConstants.COUPON_STOCK_LESS));
        // 发放优惠券
        List<SmsCouponRecord> smsIssueCouponRecordList = new ArrayList<>();
        String collectionName = SmsCouponRecord.buildCollectionName(smsCouponActivity.getChannelId());
        int issueSuccessSize;
        try {
            targetIssueUserIdList.forEach(userId -> {
                // 构建用户领取优惠券记录
                SmsCouponRecord smsCouponRecord = buildSmsCouponRecord(userId, smsCouponActivity.getChannelId(), smsCouponActivity);
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
        SmsCouponCode smsCouponCode = mongoTemplate.findOne(SmsCouponCode.buildCodeQuery(clientUser.getChannelId(), couponCode), SmsCouponCode.class, SmsCouponCode.buildCollectionName(clientUser.getChannelId()));
        Assert.notNull(smsCouponCode, () -> new ApiException(CouponResCodeConstants.INVALID_CODE));
        assert smsCouponCode != null;
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(smsCouponCode.getActivityCode());
        // 活动校验
        validSmsCouponActivity(smsCouponActivity);
        // 用户优惠券领取次数限制校验
        checkUserReceiveLimit(clientUser.getId(), clientUser.getChannelId(), smsCouponActivity);
        // 进行兑换
        boolean remove = doConvertCouponCode(couponRedisKeyBuilder.buildCouponCodeKey(smsCouponCode.getActivityCode(), smsCouponCode.getBatchNo()), couponCode);
        Assert.isTrue(remove, () -> new ApiException(CouponResCodeConstants.CODE_USED));
        try {
            // 构建用户领取优惠券记录
            SmsCouponRecord smsCouponRecord = buildSmsCouponRecord(clientUser.getId(), clientUser.getChannelId(), smsCouponActivity);
            // 记录券码
            smsCouponRecord.setCouponCode(couponCode);
            // 目前只支持渠道券码
            smsCouponRecord.setCouponType(CouponType.PLATFORM);
            mongoTemplate.save(smsCouponRecord, SmsCouponRecord.buildCollectionName(clientUser.getChannelId()));
            log.info("用户[{}]使用券码[{}]成功兑换优惠券[{}]", clientUser.getId(), couponCode, smsCouponRecord);
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
    public List<String> queryActivityBatchNoList(SmsCouponActivityBatchNoBO batchNoBO) {
        Set<String> codeKeys = getCouponCodeKeys(batchNoBO.getActivityCode());
        return codeKeys.stream()
                .map(res -> String.valueOf(CommonUtils.extractIdFromKey(res)))
                .collect(Collectors.toList());
    }

    @Override
    @DistributedLock(operationKey = "#addCouponCodeBO.activityCode")
    public boolean addSmsCouponCode(AddCouponCodeBO addCouponCodeBO) {
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(addCouponCodeBO.getActivityCode(), addCouponCodeBO.getChannelId());
        if (addCouponCodeBO.getNumber() + smsCouponActivity.getNumber() > CouponConstant.ACTIVITY_MAX_NUMBER) {
            throw new ApiException(CouponResCodeConstants.EXCEED_BATCH_MAX_NUMBER);
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
                        throw new ApiException(CouponResCodeConstants.COUPON_STOCK_SUCCESS_BUT_SHOW_DATA_EXCEPTION);
                    }
                } else {
                    throw new ApiException(CouponResCodeConstants.COUPON_STOCK_SUCCESS_BUT_SHOW_DATA_EXCEPTION);
                }
            }
        } catch (Exception e) {
            log.error("优惠券活动[{}]更新活动数量[{}]失败", smsCouponActivity.getActivityCode(), generateCodeNumber, e);
        }
        CouponCacheUtils.updateSmsCouponActivityCache(addCouponCodeBO.getActivityCode());
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
            int id = CommonUtils.extractIdFromKey(key);
            if (id > maxId) {
                maxId = id;
            }
        }
        log.info("获取优惠券活动最新批次号[{}]下一个批次号[{}]", maxId, maxId + 1);
        return String.valueOf(maxId + 1);
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
            throw new ApiException(CouponResCodeConstants.DATA_ERROR);
        }
        log.info("获取优惠券活动已有批次key[{}]", codeKeys);
        return codeKeys;
    }

    public void memberCouponStatusHandle(SmsCouponRecord smsCouponRecord, Date now) {
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
            mongoTemplate.updateFirst(BaseDoc.buildIdQuery(smsCouponRecord.getId()), SmsCouponRecord.buildStatusUpdate(couponStatus), SmsCouponRecord.class, SmsCouponRecord.buildCollectionName(smsCouponRecord.getChannelId()));
            log.info("优惠券[{}]更新状态为[{}]", smsCouponRecord.getId(), couponStatus);
            smsCouponRecord.setCouponStatus(couponStatus);
        }
    }

    @Override
    public List<CouponActivityCenterVO> smsCouponActivityCenter(CouponActivityCenterBO bo) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        List<SmsCouponActivity> targetList = getSmsCouponActivityCursorList(BaseCouponInfo.buildCouponCenterQuery(clientUser.getChannelId(), bo.getType()), bo.getEndIdCursorValue(), 10);
        if (CollectionUtils.isEmpty(targetList)) {
            return null;
        }
        return convertList(targetList, res -> {
            CouponActivityCenterVO vo = BeanUtil.toBean(res, CouponActivityCenterVO.class);
            int availableNumber = stockRedisComponent.getStrStock(couponRedisKeyBuilder.buildCouponNumberKey(res.getActivityCode()));
            // 获取当前优惠券领取数量
            int receiveNumber1 = res.getReceiveNumber();
            int receiveNumber2 = smsCouponStatisticsComponent.getStatisticsReceiveMap().getOrDefault(vo.getActivityCode(), new LongAdder()).intValue();
            // 计算比例
            BigDecimal ratio = BigDecimal.valueOf((receiveNumber1 + receiveNumber2) / (receiveNumber1 + receiveNumber2 + availableNumber));
            vo.setRatio(ratio);
            return vo;
        });
    }

    @Override
    public List<MemberCouponCenterVO> memberCouponCenter(MemberCouponCenterBO bo) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        List<SmsCouponRecord> resultList = MongoUtils.pageByIdCursor(SmsCouponRecord.buildMemberCouponCenterQuery(clientUser.getId(), bo.getType(), bo.getStatus(), bo.getCouponType()), bo.getEndIdCursorValue(), bo.getSize(), SmsCouponRecord.class, SmsCouponRecord.buildCollectionName(clientUser.getChannelId()));
        Date now = new Date();
        return convertList(resultList, res -> {
            // 状态实时更新
            memberCouponStatusHandle(res, now);
            // 判断是否冻结[特殊异常情况下]
            if (CouponStatus.IN_EFFECT.equals(res.getCouponStatus())) {
                if (couponComponent.isFreezeCoupon(res.getId())) {
                    res.setCouponStatus(CouponStatus.OCCUPIED);
                }
            }
            MemberCouponCenterVO vo = BeanUtil.toBean(res, MemberCouponCenterVO.class);
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
                    break;
                default:
            }
            return vo;
        });
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
            throw new ApiException(CouponResCodeConstants.COUPON_ACTIVITY_DOWN);
        }
        // 是否达到领取时间
        if (now.before(baseCouponInfo.getReceiveStartTime()) || now.after(baseCouponInfo.getReceiveEndTime())) {
            throw new ApiException(CouponResCodeConstants.COUPON_ACTIVITY_CANT_GET);
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
            throw new ApiException(CouponResCodeConstants.EXCEED_RECEIVE_LIMIT);
        }
    }

    /**
     * 构建用户优惠券记录
     *
     * @param userId         用户id
     * @param channelId      渠道id
     * @param baseCouponInfo 优惠券基础信息
     * @return 用户优惠券记录
     */
    private SmsCouponRecord buildSmsCouponRecord(Long userId, Long channelId, BaseCouponInfo baseCouponInfo) {
        Date now = new Date();
        SmsCouponRecord smsCouponRecord = new SmsCouponRecord();
        smsCouponRecord.setMemberId(userId);
        smsCouponRecord.setChannelId(channelId);
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
                        throw new ApiException(CouponResCodeConstants.DATA_ERROR);
                }
                break;
            default:
                throw new ApiException(CouponResCodeConstants.DATA_ERROR);
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
        SmsCouponActivity smsCouponActivity = CouponCacheUtils.getSmsCouponActivityCache(activityCode);
        Assert.notNull(smsCouponActivity, () -> new ApiException(CouponResCodeConstants.UN_FOUND_ACTIVITY));
        return smsCouponActivity;
    }

    private SmsCouponActivity getSmsCouponActivity(String activityCode, Long channelId) {
        // 查询优惠券活动
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(activityCode);
        Assert.isTrue(smsCouponActivity.getChannelId().equals(channelId), () -> new ApiException(CouponResCodeConstants.UN_FOUND_ACTIVITY));
        return smsCouponActivity;
    }

    private List<SmsCouponActivity> getSmsCouponActivityCursorList(Query query, String cursorIdValue, int size) {
        List<SmsCouponActivity> resultList = MongoUtils.pageByIdCursorForFields(query, cursorIdValue, size, Collections.singletonList("activityCode"), SmsCouponActivity.class);
        if (CollectionUtils.isEmpty(resultList)) {
            return null;
        }
        List<String> activityCodeList = resultList.stream().map(BaseCouponInfo::getActivityCode).collect(Collectors.toList());
        List<SmsCouponActivity> targetList = new ArrayList<>();
        activityCodeList.forEach(activityCode -> {
            try {
                SmsCouponActivity smsCouponActivity = getSmsCouponActivity(activityCode);
                targetList.add(smsCouponActivity);
            } catch (Exception e) {
                log.error("查询平台优惠券活动异常", e);
            }
        });
        return targetList;
    }

    /**
     * C端获取优惠券活动信息
     *
     * @param activityCode 优惠券活动编码
     * @return 活动
     */
    private MerchantCouponActivity getMerchantCouponActivity(String activityCode) {
        // 查询优惠券活动
        MerchantCouponActivity merchantCouponActivity = mongoTemplate.findOne(MerchantCouponActivity.buildActivityCodeQuery(activityCode), MerchantCouponActivity.class);
        Assert.notNull(merchantCouponActivity, () -> new ApiException(CouponResCodeConstants.UN_FOUND_ACTIVITY));
        return merchantCouponActivity;
    }

    /**
     * 兑换优惠券券码
     *
     * @return boolean
     */
    private boolean doConvertCouponCode(String couponCodeKey, String couponCode) {
        RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
        // 执行脚本
        Long res = rScript.evalSha(
                RScript.Mode.READ_WRITE, // 脚本模式
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
                .map(res -> BeanUtil.toBean(res, ProductCouponActivityVO.class))
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
                }).map(res -> BeanUtil.toBean(res, ProductCouponActivityVO.class))
                .collect(Collectors.toList());
        sortedIntegralCouponActivityTagList.addAll(sortedCashCouponActivityTagList);
        return sortedCashCouponActivityTagList;
    }

    /**
     * 获取确认下单用户优惠券信息
     *
     * @param orderBOList         下单商品BO
     * @param memberSmsCouponList 用户有效平台优惠券集合
     * @return ConfirmOrderCouponVO
     */
    public ConfirmOrderCouponVO getOrderMemberCouponList(List<MemberCouponCenterVO> memberSmsCouponList, List<OrderMemberSmsCouponBO> orderBOList) {
        if (CollectionUtils.isEmpty(memberSmsCouponList)) {
            return null;
        }
        // 分类有效优惠券
        List<MemberCouponCenterVO> memberIntegralCouponList = filterList(memberSmsCouponList, res -> res.getCouponDiscountType().equals(CouponDiscountType.INTEGRAL_DISCOUNT));
        List<MemberCouponCenterVO> memberCashCouponList = filterList(memberSmsCouponList, res -> !res.getCouponDiscountType().equals(CouponDiscountType.INTEGRAL_DISCOUNT));
        // 一：可用优惠券【1.满足使用条件 2.不满足使用条件】
        List<OrderMemberCouponVO> availableIntegralCouponList = new ArrayList<>();
        List<OrderMemberCouponVO> availableCashCouponList = new ArrayList<>();
        // 二：不可用优惠券【1.未到使用周期范围内 2.下单商品没有适用优惠券】
        List<OrderMemberCouponVO> unAvailableIntegralCouponList = new ArrayList<>();
        List<OrderMemberCouponVO> unAvailableCashCouponList = new ArrayList<>();

        ConfirmOrderCouponVO result = new ConfirmOrderCouponVO();
        result.setAvailableIntegralCouponList(availableIntegralCouponList);
        result.setAvailableCashCouponList(availableCashCouponList);
        result.setUnAvailableIntegralCouponList(unAvailableIntegralCouponList);
        result.setUnAvailableCashCouponList(unAvailableCashCouponList);
        // 积分
        memberIntegralCouponList.forEach(res -> {
            OrderMemberCouponVO vo = BeanUtil.toBean(res, OrderMemberCouponVO.class);
            if (orderCouponInfoHandler(res, orderBOList, vo)) {
                availableIntegralCouponList.add(vo);
            } else {
                unAvailableIntegralCouponList.add(vo);
            }
        });
        // 现金
        memberCashCouponList.forEach(res -> {
            OrderMemberCouponVO vo = BeanUtil.toBean(res, OrderMemberCouponVO.class);
            if (orderCouponInfoHandler(res, orderBOList, vo)) {
                availableCashCouponList.add(vo);
            } else {
                unAvailableCashCouponList.add(vo);
            }
        });
        availableCashCouponList.sort(Comparator.comparing(OrderMemberCouponVO::getDiscountTotalAmount).reversed()
                .thenComparing(OrderMemberCouponVO::getUseEndTime)
                .thenComparing(OrderMemberCouponVO::getLackAmount));
        availableIntegralCouponList.sort(Comparator.comparing(OrderMemberCouponVO::getDiscountTotalAmount).reversed()
                .thenComparing(OrderMemberCouponVO::getUseEndTime)
                .thenComparing(OrderMemberCouponVO::getLackAmount));
        return result;
    }

    private boolean orderCouponInfoHandler(MemberCouponCenterVO res, List<OrderMemberSmsCouponBO> orderBOList, OrderMemberCouponVO vo) {
        if (res.getUseStartTime().after(new Date())) {
            vo.setDisabled(CouponConstant.Disabled.UN_REACHED_TIME);
            return false;
        }
        List<OrderMemberSmsCouponBO> targetList = null;
        switch (res.getApplyProductRangeType()) {
            case ALL:
                targetList = orderBOList;
                break;
            case SPECIFY_PRODUCT:
                targetList = filterList(orderBOList, e -> res.getIdList().contains(e.getSmsId()));
                break;
            case EXCLUDE_PRODUCT:
                targetList = filterList(orderBOList, e -> !res.getIdList().contains(e.getSmsId()));
                break;
            default:
        }
        if (CollectionUtils.isEmpty(targetList)) {
            vo.setDisabled(CouponConstant.Disabled.NO_PRODUCT);
            return false;
        } else {
            if (res.getCouponDiscountType().equals(CouponDiscountType.INTEGRAL_DISCOUNT)) {
                return orderIntegralCouponInfoHandler(res, targetList, vo);
            } else {
                return orderCashCouponInfoHandler(res, targetList, vo);
            }
        }
    }

    private boolean orderIntegralCouponInfoHandler(MemberCouponCenterVO res, List<OrderMemberSmsCouponBO> targetList, OrderMemberCouponVO vo) {
        int achieveIntegral = res.getAchieveAmount().intValue();
        int orderProductTotalIntegral = targetList.stream().map(OrderMemberSmsCouponBO::getRealIntegral).reduce(Integer::sum).orElse(0);
        if (orderProductTotalIntegral == 0) {
            vo.setDisabled(CouponConstant.Disabled.DISCOUNT_ZERO);
            return false;
        }
        if (achieveIntegral > orderProductTotalIntegral) {
            vo.setLackAmount(BigDecimal.valueOf(achieveIntegral - orderProductTotalIntegral));
        } else {
            vo.setDiscountTotalAmount(res.getDeductionAmount());
            // 价格均摊
            Map<Long, BigDecimal> allocateResult = allocateDiscount(targetList, vo.getDiscountTotalAmount(), true);
            vo.setAllocateResultMap(allocateResult);
        }
        return true;
    }

    private boolean orderCashCouponInfoHandler(MemberCouponCenterVO res, List<OrderMemberSmsCouponBO> targetList, OrderMemberCouponVO vo) {
        BigDecimal achieveAmount = res.getAchieveAmount();
        BigDecimal orderProductTotalAmount = targetList.stream().map(OrderMemberSmsCouponBO::getRealAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        if (orderProductTotalAmount.compareTo(BigDecimal.ZERO) == 0) {
            vo.setDisabled(CouponConstant.Disabled.DISCOUNT_ZERO);
            return false;
        }
        Map<Long, BigDecimal> allocateResult = null;
        switch (res.getCouponDiscountType()) {
            case DIRECT_REDUCTION:
                vo.setDiscountTotalAmount(res.getDeductionAmount().compareTo(orderProductTotalAmount) >= 0 ? orderProductTotalAmount : res.getDeductionAmount());
                // 价格均摊
                allocateResult = allocateDiscount(targetList, vo.getDiscountTotalAmount(), false);
                break;
            case FULL_REDUCTION:
                if (achieveAmount.compareTo(orderProductTotalAmount) > 0) {
                    vo.setLackAmount(achieveAmount.subtract(orderProductTotalAmount));
                } else {
                    vo.setDiscountTotalAmount(res.getDeductionAmount().compareTo(orderProductTotalAmount) >= 0 ? orderProductTotalAmount : res.getDeductionAmount());
                    // 价格均摊
                    allocateResult = allocateDiscount(targetList, vo.getDiscountTotalAmount(), false);
                }
                break;
            case FULL_DISCOUNT:
                if (achieveAmount.compareTo(orderProductTotalAmount) > 0) {
                    vo.setLackAmount(achieveAmount.subtract(orderProductTotalAmount));
                } else {
                    BigDecimal payAmount = orderProductTotalAmount.multiply(res.getDeductionAmount()).setScale(2, RoundingMode.HALF_UP);
                    vo.setDiscountTotalAmount(orderProductTotalAmount.subtract(payAmount));
                    // 价格均摊
                    allocateResult = allocateDiscount(targetList, vo.getDiscountTotalAmount(), false);
                }
                break;
            default:
        }
        vo.setAllocateResultMap(allocateResult);
        return true;
    }

    /**
     * 订单商品均摊优惠券优惠金额
     *
     * @param targetList 目标均摊商品
     * @param discountAmount 优惠总金额
     * @return Map<Long, BigDecimal>
     */
    private Map<Long, BigDecimal> allocateDiscount(List<OrderMemberSmsCouponBO> targetList, BigDecimal discountAmount, boolean integralType) {
        List<MoneyUtils.MoneyBO<Long>> moneyBOList = targetList.stream().map(orderBO -> {
            MoneyUtils.MoneyBO<Long> bo = new MoneyUtils.MoneyBO<>();
            bo.setId(orderBO.getSkuId());
            bo.setPrice(orderBO.getRealAmount().multiply(BigDecimal.valueOf(orderBO.getNumber())));
            return bo;
        }).collect(Collectors.toList());
        return integralType ? MoneyUtils.allocateIntDiscount(moneyBOList, discountAmount.intValue()) :
                MoneyUtils.allocateBigDecimalDiscount(moneyBOList, discountAmount);
    }

}
