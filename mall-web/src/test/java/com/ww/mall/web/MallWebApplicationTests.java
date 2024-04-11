package com.ww.mall.web;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.net.NetUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import com.alibaba.fastjson.JSON;
import com.ww.mall.common.exception.ApiException;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class MallWebApplicationTests {

    @Test
    void contextLoads() {
    }

    public static void main(String[] args) {
        String ip = "111.206.170.20";
        String a = "111.206.170.0/24";
        System.out.println(NetUtil.isInRange(ip, a));

//        String secret = "CITYEFKK16FU5UYNURQ2BLVRBVVKXPWW";
//        Map<String, Object> reqBO = new HashMap<>(128);
//        reqBO.put("jwOrderCode", "M132456456132");
//        reqBO.put("checkStatus", 1);
//        reqBO.put("checkPrice", "9527.88");
//        reqBO.put("checkDate", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
//        String reqStr = JSON.toJSONString(reqBO) + secret;
//        System.out.println("签名：" + DigestUtils.md5Hex(reqStr));
    }

}
