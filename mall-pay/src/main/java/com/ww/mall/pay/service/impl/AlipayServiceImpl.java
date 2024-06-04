package com.ww.mall.pay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.domain.*;
import com.alipay.api.internal.util.AlipaySignature;
import com.ijpay.alipay.AliPayApi;
import com.ijpay.alipay.AliPayApiConfig;
import com.ijpay.alipay.AliPayApiConfigKit;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.common.utils.IdUtil;
import com.ww.mall.pay.properties.AliPayProperties;
import com.ww.mall.pay.service.AlipayService;
import com.ww.mall.pay.vo.PayResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author ww
 * @create 2024-06-04- 19:23
 * @description:
 */
@Slf4j
@Service
public class AlipayServiceImpl implements AlipayService {

    @Resource
    private AliPayProperties aliPayProperties;

    private final PayResult result = new PayResult();

    // 普通公钥模式
//     private final static String NOTIFY_URL = "/aliPay/notify_url";
//     private final static String RETURN_URL = "/aliPay/return_url";
    /**
     * 证书模式
     */
    private final static String NOTIFY_URL = "/aliPay/cert_notify_url";
    /**
     * 证书模式
     */
    private final static String RETURN_URL = "/aliPay/cert_return_url";

    @Override
    public AliPayApiConfig getApiConfig() {
        AliPayApiConfig aliPayApiConfig;
        try {
            aliPayApiConfig = AliPayApiConfigKit.getApiConfig(aliPayProperties.getAppId());
        } catch (Exception e) {
            try {
                aliPayApiConfig = AliPayApiConfig.builder()
                        .setAppId(aliPayProperties.getAppId())
                        .setAliPayPublicKey(aliPayProperties.getPublicKey())
                        .setAppCertPath(aliPayProperties.getAppCertPath())
                        .setAliPayCertPath(aliPayProperties.getAliPayCertPath())
                        .setAliPayRootCertPath(aliPayProperties.getAliPayRootCertPath())
                        .setCharset("UTF-8")
                        .setPrivateKey(aliPayProperties.getPrivateKey())
                        .setServiceUrl(aliPayProperties.getServerUrl())
                        .setSignType("RSA2")
                        // 普通公钥方式
                        //.build();
                        // 证书模式
                        .buildByCert();
            } catch (Exception ex) {
                throw new ApiException("支付宝配置初始化异常");
            }
        }
        return aliPayApiConfig;
    }

    @Override
    public PayResult appPay() {
        try {
            AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
            model.setBody("我是测试数据-By Javen");
            model.setSubject("App支付测试-By Javen");
            model.setOutTradeNo(IdUtil.generatorIdStr());
            model.setTimeoutExpress("30m");
            model.setTotalAmount("0.01");
            model.setPassbackParams("callback params");
            model.setProductCode("QUICK_MSECURITY_PAY");
            String appPayInfo = AliPayApi.appPayToResponse(model, aliPayProperties.getDomain() + NOTIFY_URL).getBody();
            result.success(appPayInfo);
        } catch (AlipayApiException e) {
            log.error("app pay exception", e);
            result.addError("system error:" + e.getMessage());
        }
        return result;
    }

    @Override
    public void wapPay(HttpServletResponse response) {
        String body = "我是测试数据-By Javen";
        String subject = "Javen Wap支付测试";
        String totalAmount = "1";
        String passBackParams = "1";
        String returnUrl = aliPayProperties.getDomain() + RETURN_URL;
        String notifyUrl = aliPayProperties.getDomain() + NOTIFY_URL;

        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setBody(body);
        model.setSubject(subject);
        model.setTotalAmount(totalAmount);
        model.setPassbackParams(passBackParams);
        String outTradeNo = IdUtil.generatorIdStr();
        System.out.println("wap outTradeNo>" + outTradeNo);
        model.setOutTradeNo(outTradeNo);
        model.setProductCode("QUICK_WAP_PAY");

        try {
            AliPayApi.wapPay(response, model, returnUrl, notifyUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pcPay(HttpServletResponse response) {
        try {
            String totalAmount = "88.88";
            String outTradeNo = IdUtil.generatorIdStr();
            log.info("pc outTradeNo>" + outTradeNo);

            String returnUrl = aliPayProperties.getDomain() + RETURN_URL;
            String notifyUrl = aliPayProperties.getDomain() + NOTIFY_URL;
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();

            model.setOutTradeNo(outTradeNo);
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            model.setTotalAmount(totalAmount);
            model.setSubject("Javen PC支付测试");
            model.setBody("Javen IJPay PC支付测试");
            model.setPassbackParams("passback_params");
            /**
             * 花呗分期相关的设置,测试环境不支持花呗分期的测试
             * hb_fq_num代表花呗分期数，仅支持传入3、6、12，其他期数暂不支持，传入会报错；
             * hb_fq_seller_percent代表卖家承担收费比例，商家承担手续费传入100，用户承担手续费传入0，仅支持传入100、0两种，其他比例暂不支持，传入会报错。
             */
//            ExtendParams extendParams = new ExtendParams();
//            extendParams.setHbFqNum("3");
//            extendParams.setHbFqSellerPercent("0");
//            model.setExtendParams(extendParams);

            AliPayApi.tradePage(response, model, notifyUrl, returnUrl);
            // https://opensupport.alipay.com/support/helpcenter/192/201602488772?ant_source=antsupport
            // Alipay Easy SDK（新版）目前只支持输出form表单，不支持打印出url链接。
            // AliPayApi.tradePage(response, "GET", model, notifyUrl, returnUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String tradePreCreatePay() {
        String subject = "Javen 支付宝扫码支付测试";
        String totalAmount = "86";
        String storeId = "123";
//        String notifyUrl = aliPayBean.getDomain() + NOTIFY_URL;
        String notifyUrl = aliPayProperties.getDomain() + "/aliPay/cert_notify_url";

        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setSubject(subject);
        model.setTotalAmount(totalAmount);
        model.setStoreId(storeId);
        model.setTimeoutExpress("5m");
        model.setOutTradeNo(IdUtil.generatorIdStr());
        try {
            String resultStr = AliPayApi.tradePrecreatePayToResponse(model, notifyUrl).getBody();
            JSONObject jsonObject = JSONObject.parseObject(resultStr);
            return jsonObject.getJSONObject("alipay_trade_precreate_response").getString("qr_code");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String tradeQuery(String outTradeNo, String tradeNo) {
        try {
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            if (StringUtils.isNotEmpty(outTradeNo)) {
                model.setOutTradeNo(outTradeNo);
            }
            if (StringUtils.isNotEmpty(tradeNo)) {
                model.setTradeNo(tradeNo);
            }
            return AliPayApi.tradeQueryToResponse(model).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String refund(String outTradeNo, String tradeNo) {
        try {
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            if (StringUtils.isNotEmpty(outTradeNo)) {
                model.setOutTradeNo(outTradeNo);
            }
            if (StringUtils.isNotEmpty(tradeNo)) {
                model.setTradeNo(tradeNo);
            }
            model.setRefundAmount("86.00");
            model.setRefundReason("正常退款");
            return AliPayApi.tradeRefundToResponse(model).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void toAuth(HttpServletResponse response) {
        try {
            String redirectUri = aliPayProperties.getDomain() + "/aliPay/redirect_uri";
            String oauth2Url = AliPayApi.getOauth2Url(aliPayProperties.getAppId(), redirectUri);
            response.sendRedirect(oauth2Url);
        } catch (Exception e) {
            log.error("唤起支付宝授权异常", e);
            throw new ApiException("唤起支付宝授权异常");
        }
    }

    @Override
    public String redirectUri(String appId, String appAuthCode) {
        try {
            // 使用app_auth_code换取app_auth_token
            AlipayOpenAuthTokenAppModel model = new AlipayOpenAuthTokenAppModel();
            model.setGrantType("authorization_code");
            model.setCode(appAuthCode);
            return AliPayApi.openAuthTokenAppToResponse(model).getBody();
        } catch (Exception e) {
            log.error("支付宝授权code获取授权token异常", e);
            throw new ApiException("支付宝授权异常");
        }
    }

    @Override
    public String openAuthTokenAppQuery(String appAuthToken) {
        try {
            AlipayOpenAuthTokenAppQueryModel model = new AlipayOpenAuthTokenAppQueryModel();
            model.setAppAuthToken(appAuthToken);
            return AliPayApi.openAuthTokenAppQueryToResponse(model).getBody();
        } catch (AlipayApiException e) {
            log.error("支付宝授权token获取用户信息异常", e);
            throw new ApiException("支付宝授权异常");
        }
    }

    @Override
    public String returnUrl(HttpServletRequest request) {
        try {
            // 获取支付宝GET过来反馈信息
            Map<String, String> map = AliPayApi.toMap(request);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }

            boolean verifyResult = AlipaySignature.rsaCheckV1(map, aliPayProperties.getPublicKey(), "UTF-8",
                    "RSA2");

            if (verifyResult) {
                // TODO 请在这里加上商户的业务逻辑程序代码
                System.out.println("return_url 验证成功");

                return "success";
            } else {
                System.out.println("return_url 验证失败");
                // TODO
                return "failure";
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return "failure";
        }
    }

    @Override
    public String certReturnUrl(HttpServletRequest request) {
        try {
            // 获取支付宝GET过来反馈信息
            Map<String, String> map = AliPayApi.toMap(request);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }

            boolean verifyResult = AlipaySignature.rsaCertCheckV1(map, aliPayProperties.getAliPayCertPath(), "UTF-8",
                    "RSA2");

            if (verifyResult) {
                // TODO 请在这里加上商户的业务逻辑程序代码
                System.out.println("certReturnUrl 验证成功");

                return "success";
            } else {
                System.out.println("certReturnUrl 验证失败");
                // TODO
                return "failure";
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return "failure";
        }
    }

    @Override
    public String notifyUrl(HttpServletRequest request) {
        try {
            // 获取支付宝POST过来反馈信息
            Map<String, String> params = AliPayApi.toMap(request);

            for (Map.Entry<String, String> entry : params.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }

            boolean verifyResult = AlipaySignature.rsaCheckV1(params, aliPayProperties.getPublicKey(), "UTF-8", "RSA2");

            if (verifyResult) {
                // TODO 请在这里加上商户的业务逻辑程序代码 异步通知可能出现订单重复通知 需要做去重处理
                System.out.println("notify_url 验证成功succcess");
                return "success";
            } else {
                System.out.println("notify_url 验证失败");
                // TODO
                return "failure";
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return "failure";
        }
    }

    @Override
    public String certNotifyUrl(HttpServletRequest request) {
        try {
            // 获取支付宝POST过来反馈信息
            Map<String, String> params = AliPayApi.toMap(request);

            for (Map.Entry<String, String> entry : params.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }

            boolean verifyResult = AlipaySignature.rsaCertCheckV1(params, aliPayProperties.getAliPayCertPath(), "UTF-8", "RSA2");

            if (verifyResult) {
                // TODO 请在这里加上商户的业务逻辑程序代码 异步通知可能出现订单重复通知 需要做去重处理
                System.out.println("certNotifyUrl 验证成功succcess");
                return "success";
            } else {
                System.out.println("certNotifyUrl 验证失败");
                // TODO
                return "failure";
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return "failure";
        }
    }
}
