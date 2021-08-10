package com.ww.mall.enums;

/**
 * @description: 快递公司code
 * @author: ww
 * @create: 2021-05-25 16:09
 */
public enum ExpressCodeEnum {

    /**
     * 快递code
     */
    SF("顺丰速运", "SF"),
    HTKY("百世快递", "HTKY"),
    ZTO("中通快递", "ZTO"),
    STO("申通快递", "STO"),
    YTO("圆通速递", "YTO"),
    YD("韵达速递", "YD"),
    YZPY("邮政快递包裹", "YZPY"),
    EMS("EMS", "EMS"),
    HHTT("天天快递", "HHTT"),
    JD("京东快递", "JD"),
    UC("优速快递", "UC"),
    DBL("德邦快递", "DBL"),
    ZJS("宅急送", "ZJS");

    /**
     * 快递公司名称
     */
    private String name;

    /**
     * 快递公司编码
     */
    private String code;

    ExpressCodeEnum(String name, String code){
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public static String getCode(String name) {
        for (ExpressCodeEnum value : ExpressCodeEnum.values()) {
            if (value.name.equals(name)) {
                return value.code;
            }
        }
        return "";
    }
}
