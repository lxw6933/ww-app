package com.ww.mall.coupon.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import com.mongodb.client.result.UpdateResult;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.app.mongodb.common.BaseDoc;
import com.ww.app.mongodb.utils.MongoUtils;
import com.ww.app.redis.annotation.DistributedLock;
import com.ww.app.redis.annotation.Resubmission;
import com.ww.app.redis.component.stock.StockRedisComponent;
import com.ww.mall.coupon.component.SmsCouponStatisticsComponent;
import com.ww.mall.coupon.component.key.CouponRedisKeyBuilder;
import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.convert.CouponConvert;
import com.ww.mall.coupon.entity.MerchantCouponActivity;
import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import com.ww.mall.coupon.eunms.ErrorCodeConstants;
import com.ww.mall.coupon.eunms.IssueType;
import com.ww.mall.coupon.service.MerchantCouponService;
import com.ww.mall.coupon.utils.CouponCacheComponent;
import com.ww.mall.coupon.view.bo.*;
import com.ww.mall.coupon.view.vo.CouponActivityCenterVO;
import com.ww.mall.coupon.view.vo.MerchantCouponDetailVO;
import com.ww.mall.coupon.view.vo.MerchantCouponPageVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import static com.ww.app.common.utils.CollectionUtils.convertList;
import static com.ww.mall.coupon.constant.LogRecordConstants.*;
import static com.ww.mall.coupon.eunms.ErrorCodeConstants.NOT_SUPPORT_ISSUE_TYPE;

/**
 * @author ww
 * @create 2026-01-08 9:11
 * @description:
 */
@Slf4j
@Service
public class MerchantCouponServiceImpl implements MerchantCouponService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private CouponCacheComponent couponCacheComponent;

    @Resource
    private CouponRedisKeyBuilder couponRedisKeyBuilder;

    @Resource
    private StockRedisComponent stockRedisComponent;

    @Resource
    private SmsCouponStatisticsComponent smsCouponStatisticsComponent;

    @Override
    public AppPageResult<MerchantCouponPageVO> pageList(MerchantCouponPageBO merchantCouponPageBO) {
        return merchantCouponPageBO.simplePageConvertResult(merchantCouponActivity -> {
            MerchantCouponPageVO vo = CouponConvert.INSTANCE.convertMerchantCouponActivityToPageVO(merchantCouponActivity);
            int availableNumber = 0;
            switch (vo.getIssueType()) {
                case RECEIVE:
                case ADMIN_ISSUE:
                    availableNumber = stockRedisComponent.getStrStock(couponRedisKeyBuilder.buildCouponNumberKey(merchantCouponActivity.getActivityCode()));
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
    @Resubmission
    @LogRecord(type = SYSTEM_COUPON_TYPE, subType = SYSTEM_COUPON_CREATE_SUB_TYPE, bizNo = "{{#merchantCouponActivity.activityCode}}", success = SYSTEM_MERCHANT_COUPON_CREATE_SUCCESS)
    public boolean add(MerchantCouponActivityAddBO merchantCouponActivityAddBO) {
        // 生成优惠券记录 - 使用转换方法
        MerchantCouponActivity merchantCouponActivity = merchantCouponActivityAddBO.convertMerchantCouponActivity();
        merchantCouponActivity = mongoTemplate.save(merchantCouponActivity);

        // 生成优惠券数量记录
        switch (merchantCouponActivity.getIssueType()) {
            case RECEIVE:
            case ADMIN_ISSUE:
                stockRedisComponent.initStrStock(couponRedisKeyBuilder.buildCouponNumberKey(merchantCouponActivity.getActivityCode()), merchantCouponActivity.getNumber());
                break;
            default:
                throw new ApiException(NOT_SUPPORT_ISSUE_TYPE);
        }
        try {
            UpdateResult updateResult = mongoTemplate.updateFirst(MerchantCouponActivity.buildActivityCodeQuery(merchantCouponActivity.getActivityCode()), MerchantCouponActivity.buildActivityNumberUpdate(merchantCouponActivity.getNumber()), MerchantCouponActivity.class);
            log.info("商家优惠券活动[{}]新增优惠券券码数量[{}]结果[{}]", merchantCouponActivity.getActivityCode(), merchantCouponActivity.getNumber(), updateResult.getModifiedCount());
            if (updateResult.getModifiedCount() == 0) {
                if (IssueType.RECEIVE.equals(merchantCouponActivity.getIssueType())) {
                    boolean rollback = stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(merchantCouponActivity.getActivityCode()), merchantCouponActivity.getNumber());
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
            log.error("商家优惠券活动[{}]更新活动数量[{}]失败", merchantCouponActivity.getActivityCode(), merchantCouponActivity.getNumber(), e);
        }
        // 记录操作日志上下文
        LogRecordContext.putVariable("merchantCouponActivity", merchantCouponActivity);
        return true;
    }

    @Override
    @Resubmission
    @LogRecord(type = SYSTEM_COUPON_TYPE, subType = SYSTEM_COUPON_UPDATE_SUB_TYPE, bizNo = "{{#merchantCouponActivityEditBO.activityCode}}", success = SYSTEM_MERCHANT_COUPON_UPDATE_SUCCESS)
    public boolean edit(MerchantCouponActivityEditBO merchantCouponActivityEditBO) {
        MerchantCouponActivity merchantCouponActivity = mongoTemplate.findOne(BaseCouponInfo.buildMerchantActivityCodeQuery(merchantCouponActivityEditBO.getActivityCode(), merchantCouponActivityEditBO.getMerchantId()), MerchantCouponActivity.class);
        Assert.notNull(merchantCouponActivity, () -> new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY));
        // 判断是否开始
        UpdateResult updateResult;
        if (merchantCouponActivity.getReceiveStartTime().before(new Date())) {
            // 未开始
            updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildMerchantActivityCodeQuery(merchantCouponActivityEditBO.getActivityCode(), merchantCouponActivityEditBO.getMerchantId()), merchantCouponActivityEditBO.buildWaitStartInfoUpdate(), MerchantCouponActivity.class);
        } else {
            // 已开始
            updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildMerchantActivityCodeQuery(merchantCouponActivityEditBO.getActivityCode(), merchantCouponActivityEditBO.getMerchantId()), merchantCouponActivityEditBO.buildInfoUpdate(), MerchantCouponActivity.class);
        }
        couponCacheComponent.updateMerchantCouponActivityCache(merchantCouponActivityEditBO.getActivityCode());
        // 记录操作日志上下文
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(merchantCouponActivity, MerchantCouponActivityEditBO.class));
        LogRecordContext.putVariable("merchantCouponActivityName", merchantCouponActivity.getName());
        return updateResult.getModifiedCount() == 1;
    }

    @Override
    public MerchantCouponDetailVO info(String id) {
        MerchantCouponActivity merchantCouponActivity = mongoTemplate.findOne(BaseDoc.buildIdQuery(id), MerchantCouponActivity.class);
        Assert.notNull(merchantCouponActivity, () -> new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY));
        return CouponConvert.INSTANCE.convertMerchantCouponActivityToDetailVO(merchantCouponActivity);
    }

    @Override
    @Resubmission
    @LogRecord(type = SYSTEM_COUPON_TYPE, subType = SYSTEM_COUPON_STATUS_SUB_TYPE, bizNo = "{{#couponActivityStatusBO.activityCode}}", success = SYSTEM_MERCHANT_COUPON_STATUS_SUCCESS)
    public boolean status(CouponActivityStatusBO couponActivityStatusBO) {
        UpdateResult updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildActivityCodeQuery(couponActivityStatusBO.getActivityCode()), BaseCouponInfo.buildActivityStatusUpdate(couponActivityStatusBO.getStatus()), MerchantCouponActivity.class);
        couponCacheComponent.updateMerchantCouponActivityCache(couponActivityStatusBO.getActivityCode());
        // 记录操作日志上下文
        LogRecordContext.putVariable("newStatus", couponActivityStatusBO.getStatus());
        LogRecordContext.putVariable("activityCode", couponActivityStatusBO.getActivityCode());
        return updateResult.getModifiedCount() == 1;
    }

    @Override
    @Resubmission
    @LogRecord(type = SYSTEM_COUPON_TYPE, subType = SYSTEM_COUPON_AUDIT_SUB_TYPE, bizNo = "{{#couponActivityStatusBO.activityCode}}", success = SYSTEM_MERCHANT_COUPON_AUDIT_SUCCESS)
    public boolean audit(MerchantCouponActivityAuditBO merchantCouponActivityAuditBO) {
        UpdateResult updateResult = mongoTemplate.updateFirst(MerchantCouponActivity.buildMerchantCouponAuditQuery(merchantCouponActivityAuditBO.getActivityCode()), MerchantCouponActivity.buildMerchantActivityAuditUpdate(merchantCouponActivityAuditBO.getAuditPass()), MerchantCouponActivity.class);
        // 记录操作日志上下文
        LogRecordContext.putVariable("newStatus", merchantCouponActivityAuditBO.getAuditPass());
        LogRecordContext.putVariable("activityCode", merchantCouponActivityAuditBO.getActivityCode());
        return updateResult.getModifiedCount() == 1;
    }

    @Override
    @DistributedLock(operationKey = "#addCouponCodeBO.activityCode")
    @LogRecord(type = SYSTEM_COUPON_TYPE, subType = SYSTEM_COUPON_ADD_CODE_SUB_TYPE, bizNo = "{{#addCouponCodeBO.activityCode}}", success = SYSTEM_COUPON_ADD_CODE_SUCCESS)
    public boolean addSmsCouponCode(MerchantAddCouponCodeBO addCouponCodeBO) {
        MerchantCouponActivity merchantCouponActivity = getMerchantCouponActivity(addCouponCodeBO.getActivityCode());
        if (addCouponCodeBO.getNumber() + merchantCouponActivity.getNumber() > CouponConstant.ACTIVITY_MAX_NUMBER) {
            throw new ApiException(ErrorCodeConstants.EXCEED_BATCH_MAX_NUMBER);
        }
        int generateCodeNumber = addCouponCodeBO.getNumber();
        switch (merchantCouponActivity.getIssueType()) {
            case RECEIVE:
            case ADMIN_ISSUE:
                boolean add = stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(merchantCouponActivity.getActivityCode()), -addCouponCodeBO.getNumber());
                if (!add) {
                    return false;
                }
                break;
            default:
                throw new ApiException(NOT_SUPPORT_ISSUE_TYPE);
        }
        try {
            UpdateResult updateResult = mongoTemplate.updateFirst(MerchantCouponActivity.buildActivityCodeQuery(merchantCouponActivity.getActivityCode()), MerchantCouponActivity.buildActivityNumberUpdate(generateCodeNumber), MerchantCouponActivity.class);
            log.info("商家优惠券活动[{}]新增优惠券券码数量[{}]结果[{}]", merchantCouponActivity.getActivityCode(), generateCodeNumber, updateResult.getModifiedCount());
            if (updateResult.getModifiedCount() == 0) {
                if (IssueType.RECEIVE.equals(merchantCouponActivity.getIssueType())) {
                    boolean rollback = stockRedisComponent.decrementStock(couponRedisKeyBuilder.buildCouponNumberKey(merchantCouponActivity.getActivityCode()), addCouponCodeBO.getNumber());
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
            log.error("商家优惠券活动[{}]更新活动数量[{}]失败", merchantCouponActivity.getActivityCode(), generateCodeNumber, e);
        }
        couponCacheComponent.updateMerchantCouponActivityCache(addCouponCodeBO.getActivityCode());
        // 记录操作日志上下文
        LogRecordContext.putVariable("num", addCouponCodeBO.getNumber());
        LogRecordContext.putVariable("activityCode", merchantCouponActivity.getActivityCode());
        return true;
    }

    @Override
    public List<CouponActivityCenterVO> merchantCouponActivityCenter(MerchantCouponActivityCenterBO bo) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        List<MerchantCouponActivity> targetList = getMerchantCouponActivityCursorList(MerchantCouponActivity.buildMerchantCouponCenterQuery(bo.getMerchantIdList(), clientUser.getChannelId(), bo.getType()), bo.getEndIdCursorValue(), 10);
        if (CollectionUtils.isEmpty(targetList)) {
            return null;
        }
        return convertList(targetList, res -> {
            CouponActivityCenterVO vo = CouponConvert.INSTANCE.convert(res);
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

    private List<MerchantCouponActivity> getMerchantCouponActivityCursorList(Query query, String cursorIdValue, int size) {
        List<MerchantCouponActivity> resultList = MongoUtils.descQueryByIdCursor(query, cursorIdValue, size, MerchantCouponActivity.class);
        if (CollectionUtils.isEmpty(resultList)) {
            return null;
        }
        List<String> activityCodeList = resultList.stream().map(BaseCouponInfo::getActivityCode).collect(Collectors.toList());
        List<MerchantCouponActivity> targetList = new ArrayList<>();
        Map<String, Integer> resMap = resultList.stream().collect(Collectors.toMap(MerchantCouponActivity::getActivityCode, MerchantCouponActivity::getReceiveNumber));

        activityCodeList.forEach(activityCode -> {
            try {
                MerchantCouponActivity smsCouponActivity = getMerchantCouponActivity(activityCode);
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
     * C端获取优惠券活动信息
     *
     * @param activityCode 优惠券活动编码
     * @return 活动
     */
    private MerchantCouponActivity getMerchantCouponActivity(String activityCode) {
        // 查询优惠券活动
        return couponCacheComponent.getMerchantCouponActivityCache(activityCode);
    }

}
