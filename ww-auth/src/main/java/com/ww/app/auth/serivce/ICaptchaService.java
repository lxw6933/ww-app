package com.ww.app.auth.serivce;

import com.ww.app.auth.view.vo.CaptchaResp;

/**
 * @author NineSu
 */
public interface ICaptchaService {

    /**
     * 生成图形验证码
     *
     * @param width  宽度
     * @param height 高度
     * @return CaptchaResp
     */
    CaptchaResp image(int width, int height);

    /**
     * 校验验证码是否正确
     *
     * @param uuid  uuid
     * @param value captcha value
     * @return true if successful, false if failed
     */
    boolean validate(String uuid, String value);
}
