package com.ww.mall.auth.serivce.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.jwt.JWTUtil;
import com.ww.mall.auth.config.JwtProperties;
import com.ww.mall.auth.serivce.LoginService;
import com.ww.mall.auth.view.vo.LoginVO;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.constant.RedisKeyConstant;
import com.ww.mall.common.enums.CodeEnum;
import com.ww.mall.common.enums.UserType;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.web.feign.AdminFeignService;
import com.ww.mall.web.feign.MemberFeignService;
import com.ww.mall.web.feign.ThirdServerFeignService;
import com.ww.mall.web.view.bo.MemberLoginBO;
import com.ww.mall.web.view.bo.SysUserLoginBO;
import com.ww.mall.web.view.dto.MemberDTO;
import com.ww.mall.web.view.dto.SysUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
public class LoginServiceImpl implements LoginService {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ThirdServerFeignService thirdServerFeignService;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private AdminFeignService adminFeignService;

    @Override
    public LoginVO adminLogin(SysUserLoginBO sysUserLoginBO) {
        // 获取登录用户信息
        Result<SysUserDTO> result = adminFeignService.login(sysUserLoginBO);
        if (Boolean.TRUE.equals(result.isSuccess()) && CodeEnum.SUCCESS.getCode().equals(result.getCode())) {
            SysUserDTO sysUserDTO = result.getValue();
            // 生成jwt token
            Date tokenEffectTime = new Date();
            Date tokenExpTime = DateUtils.addHours(tokenEffectTime, jwtProperties.getExpire());
            Map<String, Object> map = new HashMap<>();
            map.put("memberId", sysUserDTO.getId());
            map.put("mobile", sysUserDTO.getMobile());
            map.put("exp", tokenExpTime.getTime());
            map.put("nbf", tokenEffectTime.getTime());
            map.put("iss", jwtProperties.getIss());
            map.put("userType", UserType.ADMIN);
            String token = JWTUtil.createToken(map, jwtProperties.getSecret().getBytes());
            LoginVO loginVO = new LoginVO();
            loginVO.setToken(token);
            loginVO.setTokenExpTime(tokenExpTime.getTime());
            return loginVO;
        } else {
            log.error("远程调用mall-admin-manage服务失败：{}", result);
            throw new ApiException(CodeEnum.SYSTEM_ERROR.getCode(), CodeEnum.SYSTEM_ERROR.getMessage());
        }
    }

    @Override
    public LoginVO clientMobileLogin(MemberLoginBO memberLoginBO) {
        String mobile = memberLoginBO.getMobile();
        String mobileCode = redisTemplate.opsForValue().get(RedisKeyConstant.SMS_CODE_CACHE_PREFIX + mobile);
        mobileCode = StringUtils.isNotEmpty(mobileCode) ? mobileCode.split("_")[0] : null;
        if (memberLoginBO.getVerifyCode().equals(mobileCode)) {
            // 获取登录用户信息
            Result<MemberDTO> memberResult = memberFeignService.getMemberByMobile(mobile);
            if (Boolean.TRUE.equals(memberResult.isSuccess()) && CodeEnum.SUCCESS.getCode().equals(memberResult.getCode())) {
                MemberDTO member = memberResult.getValue();
                // 生成jwt token
                Date tokenEffectTime = new Date();
                Date tokenExpTime = DateUtils.addHours(tokenEffectTime, jwtProperties.getExpire());
                Map<String, Object> map = new HashMap<>();
                map.put("memberId", member.getId());
                map.put("channelId", member.getChannelId());
                map.put("mobile", member.getMobile());
                map.put("exp", tokenExpTime.getTime());
                map.put("nbf", tokenEffectTime.getTime());
                map.put("iss", jwtProperties.getIss());
                map.put("userType", UserType.CLIENT);
                String token = JWTUtil.createToken(map, jwtProperties.getSecret().getBytes());
                LoginVO loginVO = new LoginVO();
                loginVO.setToken(token);
                loginVO.setTokenExpTime(tokenExpTime.getTime());
                return loginVO;
            } else {
                log.error("远程调用mall-member服务失败：{}", memberResult);
                throw new ApiException(CodeEnum.SYSTEM_ERROR.getCode(), CodeEnum.SYSTEM_ERROR.getMessage());
            }
        } else {
            log.error("验证码错误");
            throw new ApiException(CodeEnum.CODE_ERROR.getCode(), CodeEnum.CODE_ERROR.getMessage());
        }
    }

    @Override
    public void sendCode(String mobile) {
        String mobileCode = redisTemplate.opsForValue().get(RedisKeyConstant.SMS_CODE_CACHE_PREFIX + mobile);
        if (StringUtils.isNotEmpty(mobileCode)) {
            // 判断是否超过验证码过期时间
            long mobileCodeTime = Long.parseLong(mobileCode.split("_")[1]);
            if (System.currentTimeMillis() - mobileCodeTime < 60000) {
                // 验证码一分钟内不能重发
                throw new ApiException(CodeEnum.SMS_CODE_EXCEPTION.getCode(), CodeEnum.SMS_CODE_EXCEPTION.getMessage());
            }
        }
        // 生成新的验证码
        String newCode = RandomUtil.randomNumbers(6);
        // 记录验证码生成的时间
        String newCodeTime =  newCode + "_" + System.currentTimeMillis();
        // 验证码三分钟内有效
        redisTemplate.opsForValue()
                .set(RedisKeyConstant.SMS_CODE_CACHE_PREFIX + mobile, newCodeTime, 3, TimeUnit.MINUTES);
        // 发送验证码短信
        Result<Boolean> sendSmsResult = thirdServerFeignService.sendSms(mobile, newCode);
        if (Boolean.TRUE.equals(sendSmsResult.isSuccess())) {
            if (Boolean.TRUE.equals(sendSmsResult.getValue())) {
                log.info("发送短信验证码成功");
            } else {
                throw new ApiException("发送短信验证码失败");
            }
        } else {
            throw new ApiException("调用发送短信接口失败");
        }
    }
}
