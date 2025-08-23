package com.ww.app.auth.view.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author NineSu
 */
@Data
@NoArgsConstructor
@Schema(description = "图形验证码")
public class CaptchaResp implements Serializable {

    private static final long serialVersionUID = -5197079323089523966L;

    @Schema(description = "验证码标识")
    private String uuid;
    @Schema(description = "验证码图片BASE64")
    private String base64;
    @Schema(description = "过期时间")
    private Long expire;

    public CaptchaResp(String uuid, String base64, Long expire) {
        this.uuid = uuid;
        this.base64 = base64;
        this.expire = expire;
    }
}
