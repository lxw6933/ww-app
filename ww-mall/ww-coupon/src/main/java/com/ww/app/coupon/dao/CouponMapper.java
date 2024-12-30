package com.ww.app.coupon.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.app.coupon.entity.Coupon;
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
