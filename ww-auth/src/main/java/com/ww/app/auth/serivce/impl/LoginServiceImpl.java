package com.ww.app.auth.serivce.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.jwt.JWTUtil;
import com.ww.app.admin.user.bo.SysUserLoginBO;
import com.ww.app.admin.user.dto.SysUserDTO;
import com.ww.app.auth.component.SmsCodeRedisComponent;
import com.ww.app.auth.entity.LoginLog;
import com.ww.app.auth.serivce.BaseService;
import com.ww.app.auth.serivce.LoginService;
import com.ww.app.auth.view.vo.LoginResultVO;
import com.ww.app.common.common.Result;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.enums.LoginType;
import com.ww.app.common.enums.UserType;
import com.ww.app.common.exception.ApiException;
import com.ww.app.member.member.bo.MemberLoginBO;
import com.ww.app.member.member.dto.MemberDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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

    /**
     * 验证码最短重发间隔：1 分钟。
     */
    private static final long SMS_CODE_RESEND_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

    /**
     * 验证码有效期：3 分钟。
     */
    private static final long SMS_CODE_TTL_SECONDS = TimeUnit.MINUTES.toSeconds(3);

    @Resource
    private SmsCodeRedisComponent smsCodeRedisComponent;

    @Override
    public LoginResultVO adminLogin(SysUserLoginBO sysUserLoginBO) {
        // 获取登录用户信息
        Result<SysUserDTO> result = adminUserApi.adminLogin(sysUserLoginBO);
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
        boolean verifySuccess = smsCodeRedisComponent.validateAndConsumeCode(mobile, memberLoginBO.getVerifyCode());
        if (!verifySuccess) {
            LoginLog loginLog = LoginLog.build(memberLoginBO.getMobile(), UserType.CLIENT, LoginType.CODE, GlobalResCodeConstants.CODE_ERROR.getMsg());
            mongoTemplate.save(loginLog);
            throw new ApiException(GlobalResCodeConstants.CODE_ERROR);
        }
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
    }

    @Override
    public void sendCode(String mobile) {
        // 生成新的验证码
        String newCode = RandomUtil.randomNumbers(6);
        long currentTimeMillis = System.currentTimeMillis();
        boolean setSuccess = smsCodeRedisComponent.trySetCode(
                mobile,
                newCode,
                currentTimeMillis,
                SMS_CODE_RESEND_INTERVAL_MILLIS,
                SMS_CODE_TTL_SECONDS
        );
        if (!setSuccess) {
            // 验证码一分钟内不能重发
            throw new ApiException(GlobalResCodeConstants.TOO_MANY_REQUESTS);
        }

        String codeValue = smsCodeRedisComponent.buildCodeValue(newCode, currentTimeMillis);
        try {
            // 发送验证码短信
            Result<Boolean> sendSmsResult = smsApi.sendSms(mobile, newCode);
            sendSmsResult.checkError();
            if (Boolean.TRUE.equals(sendSmsResult.getData())) {
                log.info("发送短信验证码成功");
                return;
            }
            throw new ApiException("发送短信验证码失败");
        } catch (Exception e) {
            // 短信发送失败时执行补偿：仅删除本次写入的验证码，避免误删后续请求写入的新值
            boolean rollbackResult = false;
            try {
                rollbackResult = smsCodeRedisComponent.deleteCodeIfMatch(mobile, codeValue);
            } catch (Exception rollbackException) {
                log.error("短信发送失败后回滚验证码异常。mobile={}", mobile, rollbackException);
            }
            log.warn("短信发送失败，已执行验证码回滚。mobile={}, rollbackResult={}", mobile, rollbackResult, e);
            if (e instanceof ApiException) {
                throw (ApiException) e;
            }
            throw new ApiException("发送短信验证码失败");
        }
    }

    private LoginResultVO buildLoginResult(Long userId, UserType userType, LoginType loginType, Map<String, Object> extraMap) {
        Date now = new Date();
        Date tokenExpTime = DateUtils.addHours(now, jwtProperties.getExpire());
        Map<String, Object> map = new HashMap<>();
        map.put(Constant.USER_ID, userId);
        map.put(Constant.USER_TYPE, userType);
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
