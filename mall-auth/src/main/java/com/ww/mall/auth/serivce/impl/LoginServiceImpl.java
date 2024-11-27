package com.ww.mall.auth.serivce.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.jwt.JWTUtil;
import com.ww.mall.admin.user.bo.SysUserLoginBO;
import com.ww.mall.admin.user.dto.SysUserDTO;
import com.ww.mall.auth.entity.LoginLog;
import com.ww.mall.auth.serivce.BaseService;
import com.ww.mall.auth.serivce.LoginService;
import com.ww.mall.auth.view.vo.LoginResultVO;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.constant.RedisKeyConstant;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.common.enums.LoginType;
import com.ww.mall.common.enums.UserType;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.member.member.bo.MemberLoginBO;
import com.ww.mall.member.member.dto.MemberDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2023-07-18- 10:37
 * @description:
 */
@Slf4j
@Service
public class LoginServiceImpl extends BaseService implements LoginService {

    @Override
    public LoginResultVO adminLogin(SysUserLoginBO sysUserLoginBO) {
        // 获取登录用户信息
        Result<SysUserDTO> result = adminUserApi.getAdminLoginUserInfo(sysUserLoginBO);
        result.checkError(() -> {
            LoginLog loginLog = LoginLog.build(sysUserLoginBO.getUsername(), UserType.ADMIN, LoginType.USERNAME, result.getMsg());
            mongoTemplate.save(loginLog);
            return null;
        });
        SysUserDTO sysUserDTO = result.getData();
        // 生成jwt token
        return buildLoginResult(sysUserDTO.getId(), UserType.ADMIN, LoginType.USERNAME, null);
    }

    @Override
    public LoginResultVO clientMobileLogin(MemberLoginBO memberLoginBO) {
        String mobile = memberLoginBO.getMobile();
        String mobileCode = redisTemplate.opsForValue().get(RedisKeyConstant.SMS_CODE_CACHE_PREFIX + mobile);
        mobileCode = StringUtils.isNotEmpty(mobileCode) ? mobileCode.split(Constant.UNDER_LINE_SPLIT)[0] : null;
        if (memberLoginBO.getVerifyCode().equals(mobileCode)) {
            // 获取登录用户信息
            Result<MemberDTO> memberResult = memberApi.getMemberByMobile(mobile);
            memberResult.checkError(() -> {
                LoginLog loginLog = LoginLog.build(memberLoginBO.getMobile(), UserType.CLIENT, LoginType.CODE, memberResult.getMsg());
                mongoTemplate.save(loginLog);
                return null;
            });
            MemberDTO member = memberResult.getData();
            // 处理登录结果
            Map<String, Object> map = new HashMap<>();
            map.put("channelId", member.getChannelId());
            map.put("mobile", member.getMobile());
            return buildLoginResult(member.getId(), UserType.CLIENT, LoginType.CODE, map);
        } else {
            LoginLog loginLog = LoginLog.build(memberLoginBO.getMobile(), UserType.CLIENT, LoginType.CODE, GlobalResCodeConstants.CODE_ERROR.getMsg());
            mongoTemplate.save(loginLog);
            throw new ApiException(GlobalResCodeConstants.CODE_ERROR);
        }
    }

    @Override
    public void sendCode(String mobile) {
        String mobileCode = redisTemplate.opsForValue().get(RedisKeyConstant.SMS_CODE_CACHE_PREFIX + mobile);
        if (StringUtils.isNotEmpty(mobileCode)) {
            // 判断是否超过验证码过期时间
            long mobileCodeTime = Long.parseLong(mobileCode.split(Constant.UNDER_LINE_SPLIT)[1]);
            if (System.currentTimeMillis() - mobileCodeTime < 60000) {
                // 验证码一分钟内不能重发
                throw new ApiException(GlobalResCodeConstants.TOO_MANY_REQUESTS);
            }
        }
        // 生成新的验证码
        String newCode = RandomUtil.randomNumbers(6);
        // 记录验证码生成的时间
        String newCodeTime =  newCode + Constant.UNDER_LINE_SPLIT + System.currentTimeMillis();
        // 验证码三分钟内有效
        redisTemplate.opsForValue()
                .set(RedisKeyConstant.SMS_CODE_CACHE_PREFIX + mobile, newCodeTime, 3, TimeUnit.MINUTES);
        // 发送验证码短信
        Result<Boolean> sendSmsResult = smsApi.sendSms(mobile, newCode);
        sendSmsResult.checkError();
        if (Boolean.TRUE.equals(sendSmsResult.getData())) {
            log.info("发送短信验证码成功");
        } else {
            throw new ApiException("发送短信验证码失败");
        }
    }

    private LoginResultVO buildLoginResult(Long userId, UserType userType, LoginType loginType, Map<String, Object> extraMap) {
        Date now = new Date();
        Date tokenExpTime = DateUtils.addHours(now, jwtProperties.getExpire());
        Map<String, Object> map = new HashMap<>();
        map.put("id", userId);
        map.put("userType", userType);
        map.put("exp", tokenExpTime.getTime());
        map.put("nbf", now);
        map.put("iss", jwtProperties.getIss());
        if (extraMap != null) {
            map.putAll(extraMap);
        }
        String token = JWTUtil.createToken(map, jwtProperties.getSecret().getBytes());
        LoginResultVO loginResultVO = new LoginResultVO();
        loginResultVO.setAccessToken(token);
        loginResultVO.setAccessTokenExpTime(tokenExpTime.getTime());
        // 登录日志
        LoginLog loginLog = LoginLog.build(userId, userType, loginType);
        mongoTemplate.save(loginLog);
        return loginResultVO;
    }
}
