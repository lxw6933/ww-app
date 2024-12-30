package com.ww.app.common.enums;

import cn.hutool.core.util.DesensitizedUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

/**
 * @author ww
 * @create 2024-04-26- 10:55
 * @description:
 */
@Getter
@AllArgsConstructor
public enum SensitiveDataType {

    /**
     * 姓名，第2位星号替换
     */
    USERNAME(DesensitizedUtil::chineseName),

    /**
     * 密码，全部字符都用*代替
     */
    PASSWORD(DesensitizedUtil::password),

    /**
     * 身份证，中间10位星号替换
     */
    ID_CARD(s -> DesensitizedUtil.idCardNum(s, 1, 10)),

    /**
     * 手机号，中间4位星号替换
     */
    PHONE(DesensitizedUtil::mobilePhone),

    /**
     * 电子邮箱，仅显示第一个字母和@后面的地址显示，其他星号替换
     */
    EMAIL(DesensitizedUtil::email),

    /**
     * 银行卡号，保留最后4位，其他星号替换
     */
    BANK_CARD(DesensitizedUtil::bankCard),

    /**
     * 车牌号码
     */
    CAR_LICENSE(DesensitizedUtil::carLicense);

    public final Function<String, String> desensitizer;

}
