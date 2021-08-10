package com.ww.mall.mvc.view.vo.mini;

import lombok.Data;

/**
 * @description:
 * "{
 *      "nickName":"WW",
 *      "gender":1,
 *      "language":"zh_CN",
 *      "city":"Shenzhen",
 *      "province":"Guangdong",
 *      "country":"China",
 *      "avatarUrl":"https://thirdwx.qlogo.cn/mmopen/vi_32/SIefXicfnFHfGVRicekLs22kmOW0u3D9schdQX9Fy30H7G64L0LKmAxPqeyDqQiaicMW8qljVzkIwEqgFcflydFAhQ/132"
 * }"
 * @author: ww
 * @create: 2021-06-15 17:19
 */
@Data
public class WxUserInfoVO {

    /**
     * 微信名
     */
    private String nickName;

    /**
     * 性别 0：女、1：男
     */
    private String gender;

    /**
     * 语言
     */
    private String language;

    /**
     * 城市
     */
    private String city;

    /**
     * 省份
     */
    private String province;

    /**
     * 国家
     */
    private String country;

    /**
     * 头像url
     */
    private String avatarUrl;

}
