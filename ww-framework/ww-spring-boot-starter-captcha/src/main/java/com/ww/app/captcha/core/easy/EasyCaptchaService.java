package com.ww.app.captcha.core.easy;

import com.ww.app.captcha.core.easy.vo.EasyCaptchaVO;

/**
 * @author ww
 * @create 2025-08-24 9:53
 * @description:
 */
public interface EasyCaptchaService {

    /**
     * 生成图形验证码
     *
     * @param width  宽度
     * @param height 高度
     * @return CaptchaResp
     */
    EasyCaptchaVO image(int width, int height);

    /**
     * 校验验证码是否正确
     *
     * @param uuid  uuid
     * @param value captcha value
     * @return true if successful, false if failed
     */
    boolean validate(String uuid, String value);

}
