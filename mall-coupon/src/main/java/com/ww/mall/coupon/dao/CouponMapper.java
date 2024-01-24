package com.ww.mall.coupon.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author ww
 * @create 2023-07-25- 10:18
 * @description:
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    int lineLockTest(@Param("activityCode") String activityCode, @Param("buyNumber") Integer buyNumber);

}
