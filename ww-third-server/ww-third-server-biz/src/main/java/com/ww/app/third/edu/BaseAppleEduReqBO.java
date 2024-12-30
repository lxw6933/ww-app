package com.ww.app.third.edu;

import com.alibaba.fastjson.JSON;
import com.google.common.hash.Hashing;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;

/**
 * @author ww
 * @create 2023-03-27- 16:51
 * @description:
 */
@Data
public class BaseAppleEduReqBO<T> {

    @NotNull(message = "appId不能为空")
    private String appId;

    @NotNull(message = "当前时间戳不能为空")
    private String timestamp;

    @NotNull(message = "加密后的字符不能为空")
    private String digest;

    @Valid
    @NotNull(message = "payload不能为空")
    private T payload;

    /**
     * digest ⽣成规则
     *
     * @param security appId和security是成对的，需要接⼝提供⽅提供
     * @return digest
     */
    public String getSha256(String security) {
        String payloadStr = JSON.toJSONString(this.payload);
        CharSequence originalString = this.appId + this.timestamp + payloadStr + security;
        return Hashing.sha256().hashString(originalString, StandardCharsets.UTF_8).toString();
    }

}
