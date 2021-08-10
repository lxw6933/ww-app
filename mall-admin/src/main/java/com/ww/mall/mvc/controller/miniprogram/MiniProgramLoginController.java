package com.ww.mall.mvc.controller.miniprogram;

import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.common.R;
import com.ww.mall.common.constant.MiniProgramConstant;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.common.utils.JsonUtils;
import com.ww.mall.common.utils.mini.MiniAesUtils;
import com.ww.mall.enums.Action;
import com.ww.mall.mvc.view.form.mini.AuthForm;
import com.ww.mall.mvc.view.vo.mini.AuthReturnVO;
import com.ww.mall.mvc.view.vo.mini.WxUserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: 小程序登录授权
 * @author: ww
 * @create: 2021-06-15 10:36
 */
@Slf4j
@RestController
@RequestMapping("/api/mini/")
public class MiniProgramLoginController {

    @Resource
    private RestTemplate restTemplate;


    @PostMapping("auth")
    @SysLog(value = "小程序授权登录", action = Action.LOGIN)
    public R auth(@RequestBody @Validated AuthForm form) {
        Map<String,String> params = new HashMap<>(256);
        params.put("jsCode", form.getCode());
        params.put("appId", MiniProgramConstant.APPID);
        params.put("secret", MiniProgramConstant.SECRET);
        params.put("grantType", MiniProgramConstant.GRANT_TYPE);
        String url = MiniProgramConstant.LOGIN_URL + "?appid={appId}&secret={secret}&js_code={jsCode}&grant_type={grantType}";
        // 请求微信服务端
        ResponseEntity<String> res = restTemplate.getForEntity(url, String.class, params);
        Map<String,Object> parse = JsonUtils.parse(res.getBody(), Map.class);
        int statusCodeValue = res.getStatusCodeValue();
        // 判断是否请求失败
        if (parse.get("errcode") != null) {
            Integer errCode = Integer.valueOf(parse.get("errcode").toString());
            AuthReturnVO obj = new AuthReturnVO();
            obj.setCode(statusCodeValue);
            obj.setErrCode(errCode);
            return R.oks(obj);
        }
        // 获取登录注册用户的基本信息
        String openid = parse.get("openid").toString();
        String sessionKey = parse.get("session_key").toString();
        String unionId = parse.get("unionid").toString();
        // 根据openid查询是否存在老用户
        // TODO: 2021/6/22  根据openid查询是否存在老用户
        Object user = null;
        if (user == null) {
            if (StringUtils.isEmpty(form.getIv()) || StringUtils.isEmpty(form.getEncryptedData()) || StringUtils.isEmpty(form.getUserData())) {
                throw new ValidatorException("缺少新用户授权信息，授权失败");
            }
            try {
                String decrypt = MiniAesUtils.wxDecrypt(form.getEncryptedData(), sessionKey, form.getIv());
                Map<String,Object> map = JsonUtils.parse(decrypt, Map.class);
                String phoneNumber = map.get("purePhoneNumber").toString();
                form.setMobile(phoneNumber);
            } catch (Exception e) {
                log.info("手机号信息不存在");
                e.printStackTrace();
                throw new ValidatorException("获取手机号信息失败");
            }
            // 获取用户基本信息
            WxUserInfoVO userInfo = JsonUtils.parse(form.getUserData(), WxUserInfoVO.class);
            // TODO: 2021/6/22 插入新用户记录
        }else {
            // TODO: 2021/6/21 更新用户最新信息
        }
        // 返回登录态
        String token = "";
        AuthReturnVO obj = new AuthReturnVO();
        obj.setToken(token);
        obj.setCode(statusCodeValue);
        obj.setUser(user);
        return R.oks(obj);
    }

}
