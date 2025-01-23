package com.ww.app.coupon.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.exception.ApiException;
import com.ww.app.coupon.config.CouponProperties;
import com.ww.app.coupon.constant.LockConstant;
import com.ww.app.coupon.dao.CouponMapper;
import com.ww.app.coupon.entity.Coupon;
import com.ww.app.coupon.entity.mongo.MemberCoupon;
import com.ww.app.coupon.eunms.*;
import com.ww.app.coupon.service.CouponService;
import com.ww.app.coupon.view.bo.CouponPageBO;
import com.ww.app.coupon.view.vo.CouponPageVO;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.utils.IdUtil;
import com.ww.app.mybatis.common.AppPlusPageResult;
import com.ww.app.redis.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author ww
 * @create 2023-07-25- 10:20
 * @description:
 */
@Slf4j
@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private CouponProperties couponProperties;

    @Override
//    @Transactional(transactionManager = "mongoTransactionManager", rollbackFor = Exception.class)
//    @Transactional(rollbackFor = Exception.class)
//    @MallDistributedLock(prefixKey = "#couponPageBO.title")
    public Object demo(CouponPageBO couponPageBO) {
        System.out.println("test:" + couponProperties.getCouponId());
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
//        RLock wwLock = redissonClient.getLock("wwLock");
//        try {
//            boolean lock = wwLock.tryLock(3, TimeUnit.SECONDS);
//            if (!lock) {
//                throw new ApiException("请稍后再试");
//            }
//            log.info("线程【{}】获取到锁处理业务", Thread.currentThread().getName());
////            Coupon coupon = this.list().get(0);
////            coupon.setTitle("success");
////            this.save(coupon);
//            CouponRelationProduct couponRelationProduct = new CouponRelationProduct();
//            couponRelationProduct.setActivityCode("success");
//            couponRelationProduct.setSpuId(0L);
//            couponRelationProduct.setSkuId(0L);
//            couponRelationProduct.setChannelId(0L);
//            couponRelationProduct.setBrandId(0L);
//            couponRelationProduct.setCategoryId(0L);
//            couponRelationProduct.setGroupId(0L);
//            mongoTemplate.save(couponRelationProduct);
////            int i = 1 / 0;
////            coupon.setTitle("fail");
////            this.save(coupon);
//            CouponRelationProduct couponRelationProduct2 = new CouponRelationProduct();
//            couponRelationProduct2.setId("error");
//            couponRelationProduct2.setActivityCode("error");
//            couponRelationProduct2.setSpuId(10L);
//            couponRelationProduct2.setSkuId(10L);
//            couponRelationProduct2.setChannelId(10L);
//            couponRelationProduct2.setBrandId(10L);
//            couponRelationProduct2.setCategoryId(10L);
//            couponRelationProduct2.setGroupId(10L);
//            mongoTemplate.save(couponRelationProduct2);
//            return "success";
//        } catch (InterruptedException e) {
//            log.error("获取锁异常", e);
////            throw new ApiException("异常");
//        } catch (Exception e) {
//            log.error("业务异常", e);
////            throw new ApiException(e.getMessage());
//        } finally {
//            if (wwLock.isHeldByCurrentThread()) {
//                log.info("线程【{}】释放到锁", Thread.currentThread().getName());
//                wwLock.unlock();
//            }
//        }
        return "fail";
    }

    @Override
    public AppPageResult<CouponPageVO> pageList(CouponPageBO couponPageBO) {
        QueryWrapper<Coupon> couponQueryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(couponPageBO.getTitle())) {
            couponQueryWrapper.like("title", couponPageBO.getTitle());
        }
        if (couponPageBO.getCouponType() != null) {
            couponQueryWrapper.eq("coupon_type", couponPageBO.getCouponType());
        }
        if (couponPageBO.getCouponDiscountType() != null) {
            couponQueryWrapper.eq("coupon_discount_type", couponPageBO.getCouponDiscountType());
        }
        IPage<Coupon> page = new Page<>(couponPageBO.getPageNum(), couponPageBO.getPageSize());
        this.page(page, couponQueryWrapper);
        return new AppPlusPageResult<>(page, coupon -> {
            CouponPageVO couponPageVO = new CouponPageVO();
            BeanUtils.copyProperties(coupon, couponPageVO);
            Query query = new Query();
            // 已领取数量
            Criteria criteria = Criteria.where("activityCode").is(coupon.getActivityCode());
            query.addCriteria(criteria);
            long receiveCouponCount = mongoTemplate.count(query, MemberCoupon.class);
            couponPageVO.setReceiveCouponNumber((int) receiveCouponCount);
            // 已使用数量
            criteria = criteria.and("couponStatus").is(CouponStatus.USED);
            query.addCriteria(criteria);
            long usedCouponCount = mongoTemplate.count(query, MemberCoupon.class);
            couponPageVO.setUsedCouponNumber((int) usedCouponCount);
            // 活动状态
            Date now = new Date();
            if (now.before(coupon.getReceiveStartTime())) {
                couponPageVO.setCouponActivityStatus(CouponActivityStatus.TO_TAKE_EFFECT);
            } else if (now.before(coupon.getReceiveEndTime())) {
                if (DateUtil.between(now, coupon.getReceiveEndTime(), DateUnit.HOUR) < 24) {
                    couponPageVO.setCouponActivityStatus(CouponActivityStatus.DUE_SOON);
                } else {
                    couponPageVO.setCouponActivityStatus(CouponActivityStatus.IN_EFFECT);
                }
            } else {
                couponPageVO.setCouponActivityStatus(CouponActivityStatus.EXPIRED);
            }
            return couponPageVO;
        });
    }

    @Override
    public boolean add(Coupon coupon) {
        // TODO: 2023/7/25 根据后台用户设置渠道id或商家id【默认1】
        coupon.setChannelId(1L);
        coupon.setChannelId(1L);
        String couponFlag = CouponType.MERCHANT.equals(coupon.getCouponType()) ? "MC" : "PC";
        String activityCode = couponFlag + IdUtil.generatorIdStr();
        coupon.setState(false);
        coupon.setInitSuccess(false);
        coupon.setActivityCode(activityCode);
        // TODO: 2023/7/27 保存优惠券关联能使用的商品信息
        return this.save(coupon);
    }

    @Override
    public boolean modify(String activityCode, Coupon coupon) {
        Coupon oldCoupon = getCouponByCode(activityCode);
        coupon.setId(oldCoupon.getId());
        coupon.setChannelId(oldCoupon.getChannelId());
        coupon.setMerchantId(oldCoupon.getMerchantId());
        coupon.setCouponType(oldCoupon.getCouponType());
        coupon.setCouponDiscountType(oldCoupon.getCouponDiscountType());
        coupon.setInitSuccess(oldCoupon.getInitSuccess());
        coupon.setActivityCode(oldCoupon.getActivityCode());
        // TODO: 2023/7/27 修改优惠券关联能使用的商品信息
        return this.updateById(coupon);
    }

    @Override
    @DistributedLock(value = LockConstant.RECEIVE_COUPON_LOCK, operationKey = "#activityCode")
    public boolean receiveCoupon(String activityCode) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        Coupon coupon = getCouponByCode(activityCode);
        // 优惠券校验
        validReceiveCoupon(clientUser, coupon);
        // 构建用户领取优惠券记录
        MemberCoupon memberCoupon = buildMemberCoupon(clientUser, coupon);
        mongoTemplate.save(memberCoupon);
        log.info("用户[{}]领取优惠券[{}]", clientUser.getId(), memberCoupon);
        return true;
    }

    @Override
    public void updateMemberCouponStatus(Long memberId) {
        // 查询用户所有未失效的优惠券
        Query query = new Query();
        query.addCriteria(Criteria.where("memberId").is(memberId)
                        .and("couponStatus").ne(CouponStatus.EXPIRED));
        List<MemberCoupon> memberCouponList = mongoTemplate.find(query, MemberCoupon.class);
        Date now = new Date();
        if (CollectionUtils.isNotEmpty(memberCouponList)) {
            memberCouponList.forEach(memberCoupon -> {
                CouponStatus couponStatus;
                if (now.before(DateUtil.parse(memberCoupon.getUseStartTime(), DatePattern.NORM_DATETIME_PATTERN))) {
                    couponStatus = CouponStatus.TO_TAKE_EFFECT;
                } else if (now.before(DateUtil.parse(memberCoupon.getUseEndTime(), DatePattern.NORM_DATETIME_PATTERN))) {
                    couponStatus = CouponStatus.IN_EFFECT;
                } else {
                    couponStatus = CouponStatus.EXPIRED;
                }
                if (!memberCoupon.getCouponStatus().equals(couponStatus)) {
                    memberCoupon.setCouponStatus(couponStatus);
                    memberCoupon.setUpdateTime(DateUtil.format(now, DatePattern.NORM_DATETIME_PATTERN));
                    mongoTemplate.save(memberCoupon);
                    log.info("用户[{}]优惠券[{}]更新状态为[{}]", memberId, memberCoupon.getCouponTicketCode(), couponStatus);
                }
            });
        }
    }

    private void validReceiveCoupon(ClientUser clientUser, Coupon coupon) {
        Date now = new Date();
        // 活动是否正常
        if (Boolean.FALSE.equals(coupon.getState())) {
            throw new ApiException("优惠券已下架");
        }
        // 校验用户渠道是否一致
        if (!clientUser.getChannelId().equals(coupon.getChannelId())) {
            throw new ApiException("用户所在渠道与优惠券不一致");
        }
        // 是否达到领取时间
        if (now.before(coupon.getReceiveStartTime()) || now.after(coupon.getReceiveEndTime())) {
            throw new ApiException("不在优惠券领取时间范围内，不能领取优惠券");
        }
        if (!CouponDistributeType.RECEIVE.equals(coupon.getCouponDistributeType())) {
            throw new ApiException("当前优惠券未开放领取条件");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("activityCode").is(coupon.getActivityCode()));
        // 库存校验
        long receiveTotalCount = mongoTemplate.count(query, MemberCoupon.class);
        if (receiveTotalCount >= coupon.getInitTotalCouponNumber()) {
            throw new ApiException("当前优惠券已领完");
        }
        // 是否超过领取限制
        // 查询当前用户改优惠券的领取次数
        query = new Query();
        long memberReceiveCount;
        switch (coupon.getCouponLimitReceiveTimeType()) {
            case DAY:
                Date dayBeginDay = DateUtil.beginOfDay(now);
                Date dayEndDay = DateUtil.endOfDay(now);
                query.addCriteria(
                        Criteria.where("activityCode").is(coupon.getActivityCode())
                                .and("memberId").is(clientUser.getId())
                                .and("receiveTime")
                                .gte(DateUtil.format(dayBeginDay, DatePattern.NORM_DATETIME_PATTERN))
                                .lte(DateUtil.format(dayEndDay, DatePattern.NORM_DATETIME_PATTERN))
                );
                memberReceiveCount = mongoTemplate.count(query, MemberCoupon.class);
                break;
            case WEEK:
                Date weekBeginDay = DateUtil.beginOfWeek(now);
                Date weekEndDay = DateUtil.endOfWeek(now);
                query.addCriteria(
                        Criteria.where("activityCode").is(coupon.getActivityCode())
                                .and("memberId").is(clientUser.getId())
                                .and("receiveTime")
                                .gte(DateUtil.format(weekBeginDay, DatePattern.NORM_DATETIME_PATTERN))
                                .lte(DateUtil.format(weekEndDay, DatePattern.NORM_DATETIME_PATTERN))
                );
                memberReceiveCount = mongoTemplate.count(query, MemberCoupon.class);
                break;
            case MONTH:
                Date monthBeginDay = DateUtil.beginOfMonth(now);
                Date monthEndDay = DateUtil.endOfMonth(now);
                query.addCriteria(
                        Criteria.where("activityCode").is(coupon.getActivityCode())
                                .and("memberId").is(clientUser.getId())
                                .and("receiveTime")
                                .gte(DateUtil.format(monthBeginDay, DatePattern.NORM_DATETIME_PATTERN))
                                .lte(DateUtil.format(monthEndDay, DatePattern.NORM_DATETIME_PATTERN))
                );
                memberReceiveCount = mongoTemplate.count(query, MemberCoupon.class);
                break;
            case FOREVER:
                query.addCriteria(
                        Criteria.where("activityCode").is(coupon.getActivityCode())
                                .and("memberId").is(clientUser.getId())
                );
                memberReceiveCount = mongoTemplate.count(query, MemberCoupon.class);
                break;
            default:
                log.error("优惠券领取限制时间类型数据异常");
                throw new ApiException("优惠券异常");
        }
        if (memberReceiveCount >= coupon.getCouponLimitReceiveNumber()) {
            throw new ApiException("超出优惠券领取限制");
        }
    }

    private MemberCoupon buildMemberCoupon(ClientUser clientUser, Coupon coupon) {
        Date now = new Date();
        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.setMemberId(clientUser.getId());
        memberCoupon.setActivityCode(coupon.getActivityCode());
        memberCoupon.setCouponType(coupon.getCouponType());
        memberCoupon.setCouponDiscountType(coupon.getCouponDiscountType());
        memberCoupon.setAchieveAmount(coupon.getAchieveAmount());
        memberCoupon.setDeductionAmount(coupon.getDeductionAmount());
        memberCoupon.setReceiveTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        // 优惠券
        if (Objects.requireNonNull(coupon.getCouponUseTimeType()) == CouponUseTimeType.FIXED) {
            memberCoupon.setUseStartTime(DateUtil.format(coupon.getUseStartTime(), DatePattern.NORM_DATETIME_PATTERN));
            memberCoupon.setUseEndTime(DateUtil.format(coupon.getUseEndTime(), DatePattern.NORM_DATETIME_PATTERN));
        } else if (coupon.getCouponUseTimeType() == CouponUseTimeType.AFTER_RECEIVING) {
            Integer receiveAfterDayEffect = coupon.getReceiveAfterDayEffect();
            Integer receiveAfterEffectDay = coupon.getReceiveAfterEffectDay();
            memberCoupon.setUseStartTime(DateUtil.format(DateUtil.offsetDay(now, receiveAfterDayEffect), DatePattern.NORM_DATETIME_PATTERN));
            memberCoupon.setUseEndTime(DateUtil.format(DateUtil.offsetDay(DateUtil.offsetDay(now, receiveAfterDayEffect), receiveAfterEffectDay), DatePattern.NORM_DATETIME_PATTERN));

        } else {
            throw new ApiException("优惠券异常");
        }
        if (now.before(DateUtil.parse(memberCoupon.getUseStartTime(), DatePattern.NORM_DATETIME_PATTERN))) {
            memberCoupon.setCouponStatus(CouponStatus.TO_TAKE_EFFECT);
        } else if (now.before(DateUtil.parse(memberCoupon.getUseEndTime(), DatePattern.NORM_DATETIME_PATTERN))) {
            memberCoupon.setCouponStatus(CouponStatus.IN_EFFECT);
        } else {
            memberCoupon.setCouponStatus(CouponStatus.EXPIRED);
        }
        memberCoupon.setCouponTicketCode(clientUser.getId() + RandomUtil.randomStringUpper(20));
        return memberCoupon;
    }

    private Coupon getCouponByCode(String activityCode) {
        // 查询优惠券活动
        Coupon coupon = this.getOne(new QueryWrapper<Coupon>()
                .eq("activity_code", activityCode)
        );
        if (coupon == null) {
            throw new ApiException("优惠券不存在");
        }
        return coupon;
    }

}
