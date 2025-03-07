package com.ww.mall.coupon.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.UpdateResult;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.ActivityCodeUtils;
import com.ww.app.common.utils.CommonUtils;
import com.ww.app.mongodb.common.BaseDoc;
import com.ww.app.redis.AppRedisTemplate;
import com.ww.app.redis.annotation.DistributedLock;
import com.ww.app.redis.annotation.Resubmission;
import com.ww.app.redis.component.StockRedisComponent;
import com.ww.mall.coupon.component.key.CouponRedisKeyBuilder;
import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.constant.CouponLuaConstant;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.entity.SmsCouponCode;
import com.ww.mall.coupon.entity.SmsCouponRecord;
import com.ww.mall.coupon.eunms.CouponStatus;
import com.ww.mall.coupon.eunms.IssueType;
import com.ww.mall.coupon.service.SmsCouponService;
import com.ww.mall.coupon.utils.CouponUtils;
import com.ww.mall.coupon.view.bo.AddCouponCodeBO;
import com.ww.mall.coupon.view.bo.SmsCouponActivityAddBO;
import com.ww.mall.coupon.view.bo.SmsCouponCodeListBO;
import com.ww.mall.coupon.view.bo.SmsCouponPageBO;
import com.ww.mall.coupon.view.vo.SmsCouponCodeListVO;
import com.ww.mall.coupon.view.vo.SmsCouponPageVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RScript;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2023-07-25- 10:20
 * @description:
 */
@Slf4j
@Service
public class SmsCouponServiceImpl implements SmsCouponService {

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
                    availableNumber = stockRedisComponent.getStrStock(couponRedisKeyBuilder.buildCouponNumberKey(smsCouponActivity.getActivityCode()));
                    break;
                case DISTRIBUTE:
                    Set<String> couponCodeKeys = getCouponCodeKeys(smsCouponActivity);
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
            return vo;
        });
    }

    @Override
    public List<SmsCouponCodeListVO> codeList(SmsCouponCodeListBO smsCouponCodeListBO) {
        String smsCouponCodeCollectionName = CouponUtils.getSmsCouponCodeCollectionName(smsCouponCodeListBO.getChannelId());
        List<SmsCouponCode> simpleQuerySizeResult = smsCouponCodeListBO.simpleQuerySizeResult(smsCouponCodeCollectionName);
        return simpleQuerySizeResult.stream()
                .map(res -> {
                    SmsCouponCodeListVO vo = BeanUtil.toBean(res, SmsCouponCodeListVO.class);
                    String smsCouponRecordCollectionName = CouponUtils.getSmsCouponRecordCollectionName(smsCouponCodeListBO.getChannelId());
                    SmsCouponRecord couponRecord = mongoTemplate.findOne(SmsCouponRecord.buildCodeQuery(vo.getCode()), SmsCouponRecord.class, smsCouponRecordCollectionName);
                    if (couponRecord != null) {
                        vo.setMemberId(couponRecord.getMemberId());
                        vo.setReceiveTime(couponRecord.getCreateTime());
                        vo.setCouponStatus(couponRecord.getCouponStatus());
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean add(SmsCouponActivityAddBO smsCouponActivityAddBO) {
        // 生成优惠券记录
        SmsCouponActivity addSmsCouponActivityInfo = BeanUtil.toBean(smsCouponActivityAddBO, SmsCouponActivity.class);
        addSmsCouponActivityInfo.setActivityCode(ActivityCodeUtils.getSmsCouponCode());
        addSmsCouponActivityInfo.setStatus(false);
        // 默认，后续根据后台用户set
        addSmsCouponActivityInfo.setChannelId(1L);
        addSmsCouponActivityInfo.setReceiveNumber(0);
        addSmsCouponActivityInfo.setUseNumber(0);
        SmsCouponActivity smsCouponActivity = mongoTemplate.save(addSmsCouponActivityInfo);
        // 生成优惠券数量记录
        switch (smsCouponActivity.getIssueType()) {
            case RECEIVE:
                stockRedisComponent.initStrStock(couponRedisKeyBuilder.buildCouponNumberKey(smsCouponActivity.getActivityCode()), smsCouponActivity.getNumber());
                break;
            case DISTRIBUTE:
                generateSmsCouponCode(smsCouponActivity, CouponConstant.DEFAULT_BATCH_NO, smsCouponActivity.getNumber());
                break;
            default:
        }
        return true;
    }

    /**
     * 生成平台优惠券券码
     *
     * @param smsCouponActivity 优惠券活动
     * @param batchNo 批次号
     * @return 生成券码数量
     */
    private int generateSmsCouponCode(SmsCouponActivity smsCouponActivity, String batchNo, int codeNumber) {
        List<SmsCouponCode> smsCouponCodeDocs = new ArrayList<>();
        Set<String> smsCouponCodes = new HashSet<>();
        while (smsCouponCodes.size() < codeNumber) {
            String code = UUID.randomUUID().toString(true);
            smsCouponCodes.add(code);
            smsCouponCodeDocs.add(new SmsCouponCode(smsCouponActivity.getActivityCode(), smsCouponActivity.getChannelId(), batchNo, code));
        }
        try {
            RSet<String> codeRSet = redissonClient.getSet(couponRedisKeyBuilder.buildCouponCodeKey(smsCouponActivity.getActivityCode(), batchNo));
            codeRSet.addAll(smsCouponCodes);
            // 是否插入mongodb 根据channelId 分code表
            String smsCouponCodeCollectionName = CouponUtils.getSmsCouponCodeCollectionName(smsCouponActivity.getChannelId());
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SmsCouponCode.class, smsCouponCodeCollectionName);
            bulkOps.insert(smsCouponCodeDocs);
            BulkWriteResult bulkWriteResult = bulkOps.execute();
            log.info("优惠券活动[{}]生成优惠券券码数量[{}]", smsCouponActivity.getActivityCode(), bulkWriteResult.getInsertedCount());
            return bulkWriteResult.getInsertedCount();
        } catch (Exception e) {
            log.error("生成优惠券券码异常", e);
            throw new ApiException("生成优惠券券码异常");
        }
    }

    @Override
    public boolean receiveCoupon(String activityCode) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        log.info("用户[{}]领取优惠券活动[{}]", clientUser.getId(), activityCode);
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(activityCode);
        // 活动校验
        validSmsCouponActivity(clientUser, smsCouponActivity);
        if (!IssueType.RECEIVE.equals(smsCouponActivity.getIssueType())) {
            throw new ApiException("当前优惠券不能被领取");
        }
        // 库存校验
        Assert.isTrue(stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(smsCouponActivity.getActivityCode()), 1), () -> new ApiException("优惠券已被抢完"));
        // 构建用户领取优惠券记录
        SmsCouponRecord smsCouponRecord = buildSmsCouponRecord(clientUser, smsCouponActivity);
        String smsCouponRecordCollectionName = CouponUtils.getSmsCouponRecordCollectionName(clientUser.getChannelId());
        mongoTemplate.save(smsCouponRecord, smsCouponRecordCollectionName);
        log.info("用户[{}]成功领取优惠券[{}]", clientUser.getId(), smsCouponRecord);
        return true;
    }

    @Override
    @Resubmission
    public boolean convertCoupon(String couponCode) {
        Assert.isTrue(!CouponConstant.DEFAULT_CODE.equals(couponCode), () -> new ApiException("非法请求"));
        ClientUser clientUser = AuthorizationContext.getClientUser();
        log.info("用户[{}]使用券码[{}]兑换优惠券", clientUser.getId(), couponCode);
        // 查询是否存在券码
        String smsCouponCodeCollectionName = CouponUtils.getSmsCouponCodeCollectionName(clientUser.getChannelId());
        SmsCouponCode smsCouponCode = mongoTemplate.findOne(SmsCouponCode.buildCodeQuery(clientUser.getChannelId(), couponCode), SmsCouponCode.class, smsCouponCodeCollectionName);
        Assert.notNull(smsCouponCode, () -> new ApiException("券码无效"));
        // 校验活动·
        assert smsCouponCode != null;
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(smsCouponCode.getActivityCode());
        validSmsCouponActivity(clientUser, smsCouponActivity);
        // 进行兑换
        boolean remove = doConvertCouponCode(couponRedisKeyBuilder.buildCouponCodeKey(smsCouponCode.getActivityCode(), smsCouponCode.getBatchNo()), couponCode);
        Assert.isTrue(remove, () -> new ApiException("券码已使用，请勿重复兑换"));
        // 构建用户领取优惠券记录
        SmsCouponRecord smsCouponRecord = buildSmsCouponRecord(clientUser, smsCouponActivity);
        // 记录券码
        smsCouponRecord.setCouponCode(couponCode);
        String smsCouponRecordCollectionName = CouponUtils.getSmsCouponRecordCollectionName(clientUser.getChannelId());
        mongoTemplate.save(smsCouponRecord, smsCouponRecordCollectionName);
        log.info("用户[{}]使用券码[{}]成功兑换优惠券[{}]", clientUser.getId(), couponCode, smsCouponRecord);
        return true;
    }

    @Override
    @DistributedLock(operationKey = "#avtivityCode")
    public boolean addSmsCouponCode(AddCouponCodeBO addCouponCodeBO) {
        SmsCouponActivity smsCouponActivity = getSmsCouponActivity(addCouponCodeBO.getActivityCode());
        int generateCodeNumber = addCouponCodeBO.getNumber();
        switch (smsCouponActivity.getIssueType()) {
            case RECEIVE:
                boolean add = stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(smsCouponActivity.getActivityCode()), -addCouponCodeBO.getNumber());
                if (!add) {
                    return false;
                }
                break;
            case DISTRIBUTE:
                String batchNo = getCouponActivityNextBatchNo(smsCouponActivity);
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
                        throw new ApiException("优惠券数量已成功生成，优惠券数量展示数据维护失败");
                    }
                } else {
                    throw new ApiException("优惠券券码数量已成功生成，优惠券数量展示数据维护失败");
                }
            }
        } catch (Exception e) {
            log.error("优惠券活动[{}]更新活动数量[{}]失败", smsCouponActivity.getActivityCode(), generateCodeNumber, e);
        }
        return true;
    }

    /**
     * 获取优惠券活动券码下一个批次号
     *
     * @param smsCouponActivity 优惠券活动
     * @return 下一个批次号
     */
    private String getCouponActivityNextBatchNo(SmsCouponActivity smsCouponActivity) {
        Set<String> codeKeys = getCouponCodeKeys(smsCouponActivity);
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
     * @param smsCouponActivity 优惠券活动
     * @return keys
     */
    private Set<String> getCouponCodeKeys(SmsCouponActivity smsCouponActivity) {
        Set<String> codeKeys = appRedisTemplate.scanKeys(couponRedisKeyBuilder.buildCouponCodeKey(smsCouponActivity.getActivityCode(), "*"));
        if (CollectionUtils.isEmpty(codeKeys)) {
            throw new ApiException("获取活动批次异常");
        }
        log.info("获取优惠券活动已有批次key[{}]", codeKeys);
        return codeKeys;
    }

    @Override
    public void updateMemberCouponStatus(ClientUser clientUser) {
        String smsCouponRecordCollectionName = CouponUtils.getSmsCouponRecordCollectionName(clientUser.getChannelId());
        // 查询用户所有生效的优惠券
        List<SmsCouponRecord> smsCouponRecordList =
                mongoTemplate.find(SmsCouponRecord.buildMemberEffectCouponRecordQuery(clientUser.getId()), SmsCouponRecord.class, smsCouponRecordCollectionName);
        if (CollectionUtils.isEmpty(smsCouponRecordList)) {
            return;
        }
        Date now = new Date();
        smsCouponRecordList.forEach(smsCouponRecord -> {
            CouponStatus couponStatus;
            if (now.before(smsCouponRecord.getUseStartTime())) {
                couponStatus = CouponStatus.TO_TAKE_EFFECT;
            } else if (now.before(smsCouponRecord.getUseEndTime())) {
                couponStatus = CouponStatus.IN_EFFECT;
            } else {
                couponStatus = CouponStatus.EXPIRED;
            }
            if (!smsCouponRecord.getCouponStatus().equals(couponStatus)) {
                smsCouponRecord.setCouponStatus(couponStatus);
                mongoTemplate.updateFirst(BaseDoc.buildIdQuery(smsCouponRecord.getId()), SmsCouponRecord.buildStatusUpdate(couponStatus), SmsCouponRecord.class, smsCouponRecordCollectionName);
                log.info("用户[{}]优惠券[{}]更新状态为[{}]", clientUser.getId(), smsCouponRecord.getId(), couponStatus);
            }
        });
    }

    /**
     * 校验优惠券活动
     *
     * @param clientUser 用户
     * @param smsCouponActivity 活动
     */
    private void validSmsCouponActivity(ClientUser clientUser, SmsCouponActivity smsCouponActivity) {
        Date now = new Date();
        // 活动是否正常
        if (Boolean.FALSE.equals(smsCouponActivity.getStatus())) {
            throw new ApiException("优惠券已下架");
        }
        // 校验用户渠道是否一致
        if (!clientUser.getChannelId().equals(smsCouponActivity.getChannelId())) {
            throw new ApiException("用户所在渠道与优惠券不一致");
        }
        // 是否达到领取时间
        if (now.before(smsCouponActivity.getReceiveStartDate()) || now.after(smsCouponActivity.getReceiveEndDate())) {
            throw new ApiException("不在优惠券领取时间范围内，不能领取优惠券");
        }
        // 查询当前用户改优惠券的领取次数
        String smsCouponRecordCollectionName = CouponUtils.getSmsCouponRecordCollectionName(clientUser.getChannelId());
        long memberReceiveCount = mongoTemplate.count(SmsCouponRecord.buildMemberReceiveRecordQuery(clientUser.getId(), smsCouponActivity.getActivityCode(), smsCouponActivity.getLimitReceiveTimeType()), SmsCouponRecord.class, smsCouponRecordCollectionName);
        if (memberReceiveCount >= smsCouponActivity.getLimitReceiveNumber()) {
            throw new ApiException("超出优惠券领取限制");
        }
    }

    /**
     * 构建用户优惠券记录
     *
     * @param clientUser 用户
     * @param smsCouponActivity 活动
     * @return 用户优惠券记录
     */
    private SmsCouponRecord buildSmsCouponRecord(ClientUser clientUser, SmsCouponActivity smsCouponActivity) {
        Date now = new Date();
        SmsCouponRecord smsCouponRecord = new SmsCouponRecord();
        smsCouponRecord.setMemberId(clientUser.getId());
        smsCouponRecord.setActivityCode(smsCouponActivity.getActivityCode());
        smsCouponRecord.setCouponDiscountType(smsCouponActivity.getCouponDiscountType());
        smsCouponRecord.setAchieveAmount(smsCouponActivity.getAchieveAmount());
        smsCouponRecord.setDeductionAmount(smsCouponActivity.getDeductionAmount());
        // 优惠券有效期生效类型
        switch (smsCouponActivity.getEffectTimeType()) {
            case FIXED:
                smsCouponRecord.setUseStartTime(smsCouponActivity.getUseStartTime());
                smsCouponRecord.setUseEndTime(smsCouponActivity.getUseEndTime());
                break;
            case AFTER_RECEIVING:
                int receiveAfterDayEffect = smsCouponActivity.getReceiveAfterEffectDay();
                int effectDay = smsCouponActivity.getEffectDay();
                smsCouponRecord.setUseStartTime(DateUtil.offsetDay(now, receiveAfterDayEffect));
                smsCouponRecord.setUseEndTime(DateUtil.offsetDay(smsCouponRecord.getUseStartTime(), effectDay));
                break;
            default:
                throw new ApiException("优惠券活动异常");
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
     * 获取优惠券活动信息
     *
     * @param activityCode 优惠券活动编码
     * @return 活动
     */
    private SmsCouponActivity getSmsCouponActivity(String activityCode) {
        // 查询优惠券活动
        SmsCouponActivity smsCouponActivity = mongoTemplate.findOne(SmsCouponActivity.buildActivityCodeQuery(activityCode), SmsCouponActivity.class);
        Assert.notNull(smsCouponActivity, () -> new ApiException("优惠券不存在"));
        return smsCouponActivity;
    }

    /**
     * 兑换优惠券券码
     *
     * @return boolean
     */
    @SuppressWarnings("all")
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

}
