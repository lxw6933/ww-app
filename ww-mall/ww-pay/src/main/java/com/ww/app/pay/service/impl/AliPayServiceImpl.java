package com.ww.app.pay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayObject;
import com.alipay.api.domain.*;
import com.alipay.api.internal.util.AlipaySignature;
import com.ijpay.alipay.AliPayApi;
import com.ijpay.alipay.AliPayApiConfig;
import com.ijpay.alipay.AliPayApiConfigKit;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.exception.ApiException;
import com.ww.app.pay.properties.AliPayProperties;
import com.ww.app.pay.service.AliPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author ww
 * @create 2024-06-04- 19:23
 * @description:
 */
@Slf4j
@Service
public class AliPayServiceImpl implements AliPayService {

    @Resource
    private AliPayProperties aliPayProperties;

    // 支付结果回调地址
    private final static String NOTIFY_URL = "/aliPay/notify_url";
    // 支付后返回地址
    private final static String RETURN_URL = "/aliPay/return_url";

    private void setPayInfo(AlipayObject alipayObject,
                                      String tradeDesc,
                                      String tradeTitle,
                                      String outTradeNo,
                                      String tradeExpireTime,
                                      String tradeAmount,
                                      String callbackParam) {
        if (alipayObject instanceof AlipayTradeAppPayModel) {
            // APP支付
            AlipayTradeAppPayModel model = (AlipayTradeAppPayModel) alipayObject;
            model.setBody(tradeDesc);
            model.setSubject(tradeTitle);
            model.setOutTradeNo(outTradeNo);
            model.setTimeExpire(tradeExpireTime);
            model.setTotalAmount(tradeAmount);
            model.setPassbackParams(callbackParam);
            //  商家和支付宝签约的产品码。 枚举值（点击查看签约情况）：
            //  QUICK_MSECURITY_PAY：无线快捷支付产品；
            //  CYCLE_PAY_AUTH：周期扣款产品。
            //  默认值为QUICK_MSECURITY_PAY。
//            model.setProductCode("QUICK_MSECURITY_PAY");
        } else if (alipayObject instanceof AlipayTradeWapPayModel) {
            // WAP 支付
            AlipayTradeWapPayModel model = (AlipayTradeWapPayModel) alipayObject;
            model.setBody(tradeDesc);
            model.setSubject(tradeTitle);
            model.setOutTradeNo(outTradeNo);
            model.setTimeExpire(tradeExpireTime);
            model.setTotalAmount(tradeAmount);
            model.setPassbackParams(callbackParam);
            // 商家和支付宝签约的产品码。 枚举值（点击查看签约情况）：
            // QUICK_WAP_WAY：手机网站支付产品。
            // 默认值为 QUICK_WAP_WAY。
//            model.setProductCode("QUICK_WAP_PAY");
        } else if (alipayObject instanceof AlipayTradePagePayModel) {
            // PC 支付
            AlipayTradePagePayModel model = (AlipayTradePagePayModel) alipayObject;
            model.setBody(tradeDesc);
            model.setSubject(tradeTitle);
            model.setOutTradeNo(outTradeNo);
            model.setTimeExpire(tradeExpireTime);
            model.setTotalAmount(tradeAmount);
            model.setPassbackParams(callbackParam);
            // 商家和支付宝签约的产品码。 枚举值（点击查看签约情况）：
            // FAST_INSTANT_TRADE_PAY：新快捷即时到账产品。
            // 注：目前仅支持FAST_INSTANT_TRADE_PAY
//            model.setProductCode("FAST_INSTANT_TRADE_PAY");
        } else if (alipayObject instanceof AlipayTradePrecreateModel) {
            // 生成支付二维码
            AlipayTradePrecreateModel model = (AlipayTradePrecreateModel) alipayObject;
            model.setBody(tradeDesc);
            model.setSubject(tradeTitle);
            model.setOutTradeNo(outTradeNo);
            model.setTimeExpire(tradeExpireTime);
            model.setTotalAmount(tradeAmount);
        }
    }

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
                        .setCharset(StandardCharsets.UTF_8.name())
                        .setPrivateKey(aliPayProperties.getPrivateKey())
                        .setServiceUrl(aliPayProperties.getServerUrl())
                        .setSignType(aliPayProperties.getSignType())
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
    public String appPay() {
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        setPayInfo(model, "", "", "", "", "", "");
        try {
            return AliPayApi.appPayToResponse(model, aliPayProperties.getDomain() + NOTIFY_URL).getBody();
        } catch (AlipayApiException e) {
            log.error("app pay exception", e);
            throw new ApiException("支付异常");
        }
    }

    @Override
    public void wapPay(HttpServletResponse response) {
        String returnUrl = aliPayProperties.getDomain() + RETURN_URL;
        String notifyUrl = aliPayProperties.getDomain() + NOTIFY_URL;
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        setPayInfo(model, "", "", "", "", "", "");
        try {
            AliPayApi.wapPay(response, model, returnUrl, notifyUrl);
        } catch (Exception e) {
            log.error("wap pay exception", e);
            throw new ApiException("支付异常");
        }
    }

    @Override
    public void pcPay(HttpServletResponse response) {
        String returnUrl = aliPayProperties.getDomain() + RETURN_URL;
        String notifyUrl = aliPayProperties.getDomain() + NOTIFY_URL;
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        setPayInfo(model, "", "", "", "", "", "");
        /**
         * 花呗分期相关的设置,测试环境不支持花呗分期的测试
         * hb_fq_num代表花呗分期数，仅支持传入3、6、12，其他期数暂不支持，传入会报错；
         * hb_fq_seller_percent代表卖家承担收费比例，商家承担手续费传入100，用户承担手续费传入0，仅支持传入100、0两种，其他比例暂不支持，传入会报错。
         */
//        ExtendParams extendParams = new ExtendParams();
//        extendParams.setHbFqNum("3");
//        extendParams.setHbFqSellerPercent("0");
//        model.setExtendParams(extendParams);
        try {
            AliPayApi.tradePage(response, model, notifyUrl, returnUrl);
            // https://opensupport.alipay.com/support/helpcenter/192/201602488772?ant_source=antsupport
            // Alipay Easy SDK（新版）目前只支持输出form表单，不支持打印出url链接。
            // AliPayApi.tradePage(response, "GET", model, notifyUrl, returnUrl);
        } catch (Exception e) {
            log.error("pc pay exception", e);
            throw new ApiException("支付异常");
        }
    }

    @Override
    public String tradePreCreatePay() {
        String notifyUrl = aliPayProperties.getDomain() + NOTIFY_URL;
        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        setPayInfo(model, "", "", "", "", "", null);
        // 店铺
//        model.setStoreId("");
        try {
            String resultStr = AliPayApi.tradePrecreatePayToResponse(model, notifyUrl).getBody();
            JSONObject jsonObject = JSONObject.parseObject(resultStr);
            return jsonObject.getJSONObject("alipay_trade_precreate_response").getString("qr_code");
        } catch (Exception e) {
            log.error("qr code create exception", e);
            throw new ApiException("支付二维码生成异常");
        }
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
            log.error("pay result query exception", e);
            throw new ApiException("支付查询异常");
        }
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
            log.error("refund exception", e);
            throw new ApiException("退款异常");
        }
    }

    @Override
    public void toAuth(HttpServletResponse response) {
        try {
            // 支付宝授权完携带authCode回调系统接口获取authToken
            String redirectUri = aliPayProperties.getDomain() + "/aliPay/getAuthToken";
            String oauth2Url = AliPayApi.getOauth2Url(aliPayProperties.getAppId(), redirectUri);
            response.sendRedirect(oauth2Url);
        } catch (Exception e) {
            log.error("唤起支付宝授权异常", e);
            throw new ApiException("唤起支付宝授权异常");
        }
    }

    @Override
    public String getAuthToken(String appId, String appAuthCode) {
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
    public String getAuthTokenUserInfo(String appAuthToken) {
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
            boolean verifyResult = aliPayProperties.getUseCer() ?
                    AlipaySignature.rsaCertCheckV1(map, aliPayProperties.getAliPayCertPath(), Constant.UTF_8, aliPayProperties.getSignType()) :
                    AlipaySignature.rsaCheckV1(map, aliPayProperties.getPublicKey(), Constant.UTF_8, aliPayProperties.getSignType());
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
            log.error("returnUrl exception", e);
            return "failure";
        }
    }

    @Override
    public String notifyUrl(HttpServletRequest request) {
        try {
            // 获取支付宝回调数据
            Map<String, String> params = AliPayApi.toMap(request);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }
            boolean verifyResult = aliPayProperties.getUseCer() ?
                    AlipaySignature.rsaCertCheckV1(params, aliPayProperties.getAliPayCertPath(), Constant.UTF_8, aliPayProperties.getSignType()) :
                    AlipaySignature.rsaCheckV1(params, aliPayProperties.getPublicKey(), Constant.UTF_8, aliPayProperties.getSignType());
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
            log.error("支付宝回调异常：", e);
            return "failure";
        }
    }
}
