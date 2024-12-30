package com.ww.app.pay.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ijpay.core.enums.SignType;
import com.ijpay.core.enums.TradeType;
import com.ijpay.core.kit.HttpKit;
import com.ijpay.core.kit.QrCodeKit;
import com.ijpay.core.kit.WxPayKit;
import com.ijpay.wxpay.WxPayApi;
import com.ijpay.wxpay.WxPayApiConfig;
import com.ijpay.wxpay.WxPayApiConfigKit;
import com.ijpay.wxpay.model.*;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.IpUtil;
import com.ww.app.pay.properties.WxPayProperties;
import com.ww.app.pay.service.WxPayService;
import com.ww.app.pay.vo.H5SceneInfo;
import com.ww.app.pay.vo.PayResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-06-05- 15:23
 * @description:
 */
@Slf4j
@Service
public class WxPayServiceImpl implements WxPayService {

    @Resource
    private WxPayProperties wxPayProperties;

    private String notifyUrl;
    private String refundNotifyUrl;
    private static final String USER_PAYING = "USERPAYING";

    public WxPayApiConfig getApiConfig() {
        WxPayApiConfig apiConfig;
        try {
            apiConfig = WxPayApiConfigKit.getApiConfig(wxPayProperties.getAppId());
        } catch (Exception e) {
            apiConfig = WxPayApiConfig.builder()
                    .appId(wxPayProperties.getAppId())
                    .mchId(wxPayProperties.getMchId())
                    .partnerKey(wxPayProperties.getPartnerKey())
                    .certPath(wxPayProperties.getCertPath())
                    .domain(wxPayProperties.getDomain())
                    .build();
        }
        notifyUrl = apiConfig.getDomain().concat("/wxPay/payNotify");
        refundNotifyUrl = apiConfig.getDomain().concat("/wxPay/refundNotify");
        return apiConfig;
    }

    @Override
    public PayResult appPay(HttpServletRequest request) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("body", "");
        paramMap.put("attach", "");
        paramMap.put("outTradeNo", "");
        paramMap.put("payAmount", "0.01");
        paramMap.put("payType", TradeType.APP.getTradeType());
//        paramMap.put("openId", "");
//        paramMap.put("sceneInfo", "");

        WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();
        Map<String, String> result = sendWxPayReq(request, wxPayApiConfig, paramMap);

        String prepayId = result.get("prepay_id");

        Map<String, String> packageParams = WxPayKit.appPrepayIdCreateSign(wxPayApiConfig.getAppId(), wxPayApiConfig.getMchId(), prepayId,
                wxPayApiConfig.getPartnerKey(), SignType.HMACSHA256);

        String jsonStr = JSON.toJSONString(packageParams);
        log.info("app支付返回参数:{}", jsonStr);
        return new PayResult().success(jsonStr);
    }

    @Override
    public void wapPay(HttpServletRequest request, HttpServletResponse response) {
        H5SceneInfo sceneInfo = new H5SceneInfo();
        H5SceneInfo.H5 h5_info = new H5SceneInfo.H5();
        h5_info.setType("Wap");
        // 此域名必须在商户平台--"产品中心"--"开发配置"中添加
        h5_info.setWap_url("https://gitee.com/javen205/IJPay");
        h5_info.setWap_name("IJPay VIP 充值");
        sceneInfo.setH5_info(h5_info);

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("body", "");
        paramMap.put("attach", "");
        paramMap.put("outTradeNo", "");
        paramMap.put("payAmount", "0.01");
        paramMap.put("payType", TradeType.MWEB.getTradeType());
//        paramMap.put("openId", "");
        paramMap.put("sceneInfo", JSON.toJSONString(sceneInfo));

        WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

        Map<String, String> result = sendWxPayReq(request, wxPayApiConfig, paramMap);
        String webUrl = result.get("mweb_url");
        log.info("h5微信支付链接: [{}]", webUrl);
        try {
            response.sendRedirect(webUrl);
        } catch (IOException e) {
            log.error("微信H5支付唤起失败", e);
            throw new ApiException("微信支付唤起失败");
        }
    }

    @Override
    public PayResult webPay(HttpServletRequest request, String totalFee) {
        // openId采用网页授权获取 access_token API：SnsAccessTokenApi获取
        String openId = (String) request.getSession().getAttribute("openId");
        if (StringUtils.isEmpty(openId)) {
            throw new ApiException("未获取到openId");
        }
        if (StringUtils.isEmpty(totalFee)) {
            throw new ApiException("请输入数字金额");
        }

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("body", "");
        paramMap.put("attach", "");
        paramMap.put("outTradeNo", "");
        paramMap.put("payAmount", totalFee);
        paramMap.put("payType", TradeType.JSAPI.getTradeType());
        paramMap.put("openId", openId);
//        paramMap.put("sceneInfo", JSON.toJSONString(sceneInfo));

        WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

        Map<String, String> result = sendWxPayReq(request, wxPayApiConfig, paramMap);
        String prepayId = result.get("prepay_id");

        Map<String, String> packageParams = WxPayKit.prepayIdCreateSign(prepayId, wxPayApiConfig.getAppId(),
                wxPayApiConfig.getPartnerKey(), SignType.HMACSHA256);

        String jsonStr = JSON.toJSONString(packageParams);
        log.info("web支付返回参数: {}", jsonStr);
        return new PayResult().success(jsonStr);
    }

    @Override
    public PayResult miniAppPay(HttpServletRequest request) {
        // 需要通过授权来获取openId
        String openId = (String) request.getSession().getAttribute("openId");
        if (StringUtils.isEmpty(openId)) {
            throw new ApiException("未获取到openId");
        }

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("body", "");
        paramMap.put("attach", "");
        paramMap.put("outTradeNo", "");
        paramMap.put("payAmount", "0.01");
        paramMap.put("payType", TradeType.JSAPI.getTradeType());
        paramMap.put("openId", openId);
//        paramMap.put("sceneInfo", JSON.toJSONString(sceneInfo));

        WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

        Map<String, String> result = sendWxPayReq(request, wxPayApiConfig, paramMap);
        String prepayId = result.get("prepay_id");
        Map<String, String> packageParams = WxPayKit.miniAppPrepayIdCreateSign(wxPayApiConfig.getAppId(), prepayId,
                wxPayApiConfig.getPartnerKey(), SignType.HMACSHA256);

        String jsonStr = JSON.toJSONString(packageParams);
        log.info("小程序支付返回参数: {}", jsonStr);
        return new PayResult().success(jsonStr);
    }

    private @NotNull Map<String, String> sendWxPayReq(HttpServletRequest request,
                                                      WxPayApiConfig wxPayApiConfig,
                                                      Map<String, String> paramMap) {
        String body = paramMap.get("body");
        String attach = paramMap.get("attach");
        String outTradeNo = paramMap.get("outTradeNo");
        String payAmount = paramMap.get("payAmount");
        String payType = paramMap.get("payType");
        String openId = paramMap.get("openId");
        String sceneInfo = paramMap.get("sceneInfo");

        String ip = IpUtil.getRealIp(request);
        Map<String, String> params = UnifiedOrderModel
                .builder()
                .appid(wxPayApiConfig.getAppId())
                .mch_id(wxPayApiConfig.getMchId())
                .nonce_str(WxPayKit.generateStr())
                .body(body)
                .attach(attach)
                .out_trade_no(outTradeNo)
                .total_fee(payAmount)
                .spbill_create_ip(ip)
                .notify_url(notifyUrl)
                .trade_type(payType)
                .openid(openId)
                .scene_info(sceneInfo)
                .build()
                .createSign(wxPayApiConfig.getPartnerKey(), SignType.HMACSHA256);
        // 微信统一下单请求
        String xmlResult = WxPayApi.pushOrder(false, params);
        Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
        log.info("微信下单请求结果：{}", result);
        String returnCode = result.get("return_code");
        String returnMsg = result.get("return_msg");
        if (!WxPayKit.codeIsOk(returnCode)) {
            throw new ApiException(returnMsg);
        }
        String resultCode = result.get("result_code");
        if (!WxPayKit.codeIsOk(resultCode)) {
            throw new ApiException(returnMsg);
        }
        return result;
    }

    @Override
    public PayResult microPay(HttpServletRequest request, HttpServletResponse response) {
        String authCode = request.getParameter("auth_code");
        String totalFee = request.getParameter("total_fee");
        if (StringUtils.isBlank(totalFee)) {
            throw new ApiException("支付金额不能为空");
        }
        if (StringUtils.isBlank(authCode)) {
            throw new ApiException("auth_code参数错误");
        }
        String ip = IpUtil.getRealIp(request);
        WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

        Map<String, String> params = MicroPayModel.builder()
                .appid(wxPayApiConfig.getAppId())
                .mch_id(wxPayApiConfig.getMchId())
                .nonce_str(WxPayKit.generateStr())
                .body("让支付触手可及-刷卡支付")
                .attach("ww ww ww")
                .out_trade_no(WxPayKit.generateStr())
                .total_fee(totalFee)
                .spbill_create_ip(ip)
                .auth_code(authCode)
                .build()
                .createSign(wxPayApiConfig.getPartnerKey(), SignType.HMACSHA256);

        String xmlResult = WxPayApi.microPay(false, params);
        Map<String, String> result = WxPayKit.xmlToMap(xmlResult);
        String returnCode = result.get("return_code");
        String returnMsg = result.get("return_msg");
        if (!WxPayKit.codeIsOk(returnCode)) {
            // 通讯失败
            String errCode = result.get("err_code");
            if (StringUtils.isNotBlank(errCode)) {
                // 用户支付中，需要输入密码
                if (USER_PAYING.equals(errCode)) {
                    // 等待5秒后调用【查询订单API】
                }
            }
            log.info("提交刷卡支付失败>>{}", xmlResult);
            return new PayResult().addError(returnMsg);
        }

        String resultCode = result.get("result_code");
        if (!WxPayKit.codeIsOk(resultCode)) {
            log.info("刷卡支付失败 {}", xmlResult);
            String errCodeDes = result.get("err_code_des");
            return new PayResult().addError(errCodeDes);
        }
        // 支付成功
        return new PayResult().success(xmlResult);
    }

    @Override
    public String payNotify(HttpServletRequest request) {
        String payNotifyXmlMsg = HttpKit.readData(request);
        Map<String, String> payNotifyMsg = WxPayKit.xmlToMap(payNotifyXmlMsg);
        log.info("微信支付回调通知：{}", payNotifyMsg);
        String returnCode = payNotifyMsg.get("return_code");
        // 注意重复通知的情况，同一订单号可能收到多次通知，请注意一定先判断订单状态
        // 注意此处签名方式需与统一下单的签名类型一致
        boolean success = WxPayKit.verifyNotify(payNotifyMsg, WxPayApiConfigKit.getWxPayApiConfig().getPartnerKey(), SignType.HMACSHA256);
        if (success) {
            if (WxPayKit.codeIsOk(returnCode)) {
                // TODO 支付回调处理
                // 发送通知等
                Map<String, String> xml = new HashMap<String, String>(2);
                xml.put("return_code", "SUCCESS");
                xml.put("return_msg", "OK");
                return WxPayKit.toXml(xml);
            }
        }
        return null;
    }

    @Override
    public PayResult scanCode1(HttpServletRequest request, HttpServletResponse response, String productId) {
        try {
            if (StringUtils.isBlank(productId)) {
                return new PayResult().addError("productId is null");
            }
            WxPayApiConfig config = WxPayApiConfigKit.getWxPayApiConfig();
            // 获取扫码支付（模式一）url
            String qrCodeUrl = WxPayKit.bizPayUrl(config.getPartnerKey(), config.getAppId(), config.getMchId(), productId);
            log.info(qrCodeUrl);
            // 生成二维码保存的路径
            String name = "payQRCode1.png";
            log.info(ResourceUtils.getURL("classpath:").getPath());
            boolean encode = QrCodeKit.encode(qrCodeUrl, BarcodeFormat.QR_CODE, 3, ErrorCorrectionLevel.H,
                    "png", 200, 200,
                    ResourceUtils.getURL("classpath:").getPath().concat("static").concat(File.separator).concat(name));
            if (encode) {
                // 在页面上显示
                return new PayResult().success(name);
            }
        } catch (Exception e) {
            throw new ApiException("微信扫码异常");
        }
        return null;
    }

    @Override
    public PayResult scanCode2(HttpServletRequest request, HttpServletResponse response, String totalFee) {
        if (StringUtils.isBlank(totalFee)) {
            return new PayResult().addError("支付金额不能为空");
        }

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("body", "");
        paramMap.put("attach", "");
        paramMap.put("outTradeNo", "");
        paramMap.put("payAmount", totalFee);
        paramMap.put("payType", TradeType.NATIVE.getTradeType());
//        paramMap.put("openId", openId);
//        paramMap.put("sceneInfo", JSON.toJSONString(sceneInfo));

        WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

        Map<String, String> result = sendWxPayReq(request, wxPayApiConfig, paramMap);

        String qrCodeUrl = result.get("code_url");
        String name = "payQRCode2.png";

        boolean encode = QrCodeKit.encode(qrCodeUrl, BarcodeFormat.QR_CODE, 3, ErrorCorrectionLevel.H, "png", 200, 200,
                request.getSession().getServletContext().getRealPath("/") + File.separator + name);
        if (encode) {
            // 在页面上显示
            return new PayResult().success(name);
        }
        return null;
    }

    @Override
    public String scanCodeNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            String notifyData = HttpKit.readData(request);
            Map<String, String> notifyMap = WxPayKit.xmlToMap(notifyData);
            log.info("收到微信扫码发起支付：{}", notifyMap);

            String appId = notifyMap.get("appid");
            String openId = notifyMap.get("openid");
            String mchId = notifyMap.get("mch_id");
            String isSubscribe = notifyMap.get("is_subscribe");
            String nonceStr = notifyMap.get("nonce_str");
            String productId = notifyMap.get("product_id");
            String sign = notifyMap.get("sign");

            Map<String, String> packageParams = new HashMap<>(6);
            packageParams.put("appid", appId);
            packageParams.put("openid", openId);
            packageParams.put("mch_id", mchId);
            packageParams.put("is_subscribe", isSubscribe);
            packageParams.put("nonce_str", nonceStr);
            packageParams.put("product_id", productId);

            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("body", "");
            paramMap.put("attach", "");
            paramMap.put("outTradeNo", "");
            paramMap.put("payAmount", "0.01");
            paramMap.put("payType", TradeType.NATIVE.getTradeType());
            paramMap.put("openId", openId);
//        paramMap.put("sceneInfo", JSON.toJSONString(sceneInfo));

            WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

            String packageSign = WxPayKit.createSign(packageParams, wxPayApiConfig.getPartnerKey(), SignType.MD5);

            Map<String, String> result = sendWxPayReq(request, wxPayApiConfig, paramMap);

            String prepayId = result.get("prepay_id");
            Map<String, String> prepayParams = new HashMap<>(10);
            prepayParams.put("return_code", "SUCCESS");
            prepayParams.put("appid", appId);
            prepayParams.put("mch_id", mchId);
            prepayParams.put("nonce_str", System.currentTimeMillis() + "");
            prepayParams.put("prepay_id", prepayId);
            String prepaySign;
            if (sign.equals(packageSign)) {
                prepayParams.put("result_code", "SUCCESS");
            } else {
                prepayParams.put("result_code", "FAIL");
                // result_code为FAIL时，添加该键值对，value值是微信告诉客户的信息
                prepayParams.put("err_code_des", "订单失效");
            }
            prepaySign = WxPayKit.createSign(prepayParams, wxPayApiConfig.getPartnerKey(), SignType.HMACSHA256);
            prepayParams.put("sign", prepaySign);
            String xml = WxPayKit.toXml(prepayParams);
            log.info("微信扫码发起支付结果：{}", xml);
            return xml;
        } catch (Exception e) {
            throw new ApiException("微信扫码支付异常");
        }
    }

    @Override
    public String queryPayResult(String transactionId, String outTradeNo) {
        log.info("transactionId:[{}]outTradeNo:[{}]查询微信支付结果", transactionId, outTradeNo);
        try {
            WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

            Map<String, String> params = OrderQueryModel.builder()
                    .appid(wxPayApiConfig.getAppId())
                    .mch_id(wxPayApiConfig.getMchId())
                    .transaction_id(transactionId)
                    .out_trade_no(outTradeNo)
                    .nonce_str(WxPayKit.generateStr())
                    .build()
                    .createSign(wxPayApiConfig.getPartnerKey(), SignType.MD5);
            String result = WxPayApi.orderQuery(params);
            log.info("查询微信支付结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("查询微信支付结果异常", e);
            throw new ApiException("查询微信支付结果异常");
        }
    }

    @Override
    public String refund(String transactionId, String outTradeNo) {
        log.info("transactionId:[{}]outTradeNo:[{}]发起微信退款", transactionId, outTradeNo);
        if (StringUtils.isBlank(outTradeNo) && StringUtils.isBlank(transactionId)) {
            throw new ApiException("退款单号不能为空");
        }
        try {
            WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

            Map<String, String> params = RefundModel.builder()
                    .appid(wxPayApiConfig.getAppId())
                    .mch_id(wxPayApiConfig.getMchId())
                    .nonce_str(WxPayKit.generateStr())
                    .transaction_id(transactionId)
                    .out_trade_no(outTradeNo)
                    .out_refund_no(WxPayKit.generateStr())
                    .total_fee("1")
                    .refund_fee("1")
                    .notify_url(refundNotifyUrl)
                    .build()
                    .createSign(wxPayApiConfig.getPartnerKey(), SignType.MD5);
            String refundStr = WxPayApi.orderRefund(false, params, wxPayApiConfig.getCertPath(), wxPayApiConfig.getMchId());
            log.info("transactionId:[{}]outTradeNo:[{}]微信发起退款结果：{}", transactionId, outTradeNo, refundStr);
            return refundStr;
        } catch (Exception e) {
            log.error("微信发起退款异常", e);
            throw new ApiException("微信退款异常");
        }
    }

    @Override
    public String refundNotify(HttpServletRequest request) {
        String refundNotifyXmlMsg = HttpKit.readData(request);
        Map<String, String> refundNotifyMsg = WxPayKit.xmlToMap(refundNotifyXmlMsg);
        log.info("微信退款回调通知：{}", refundNotifyMsg);
        String returnCode = refundNotifyMsg.get("return_code");
        // 注意重复通知的情况，同一订单号可能收到多次通知，请注意一定先判断订单状态
        if (WxPayKit.codeIsOk(returnCode)) {
            String reqInfo = refundNotifyMsg.get("req_info");
            String decryptData = WxPayKit.decryptData(reqInfo, WxPayApiConfigKit.getWxPayApiConfig().getPartnerKey());
            log.info("微信退款回调通知解密后结果：{}", decryptData);
            // TODO 退款回调处理
            Map<String, String> xml = new HashMap<>(2);
            xml.put("return_code", "SUCCESS");
            xml.put("return_msg", "OK");
            return WxPayKit.toXml(xml);
        }
        return null;
    }

    @Override
    public String refundQuery(String transactionId, String outTradeNo, String outRefundNo, String refundId) {
        WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

        Map<String, String> params = RefundQueryModel.builder()
                .appid(wxPayApiConfig.getAppId())
                .mch_id(wxPayApiConfig.getMchId())
                .nonce_str(WxPayKit.generateStr())
                .transaction_id(transactionId)
                .out_trade_no(outTradeNo)
                .out_refund_no(outRefundNo)
                .refund_id(refundId)
                .build()
                .createSign(wxPayApiConfig.getPartnerKey(), SignType.MD5);

        return WxPayApi.orderRefundQuery(false, params);
    }

    @Override
    public String sendRedPack(HttpServletRequest request, String openId) {
        try {
            String ip = IpUtil.getRealIp(request);

            WxPayApiConfig wxPayApiConfig = WxPayApiConfigKit.getWxPayApiConfig();

            Map<String, String> params = SendRedPackModel.builder()
                    .nonce_str(WxPayKit.generateStr())
                    .mch_billno(WxPayKit.generateStr())
                    .mch_id(wxPayApiConfig.getMchId())
                    .wxappid(wxPayApiConfig.getAppId())
                    .send_name("红包测试")
                    .re_openid(openId)
                    .total_amount("1000")
                    .total_num("1")
                    .wishing("红包备注")
                    .client_ip(ip)
                    .act_name("感恩回馈活动")
                    .remark("点 start 送红包，快来抢!")
                    .build()
                    .createSign(wxPayApiConfig.getPartnerKey(), SignType.MD5);
            String result = WxPayApi.sendRedPack(params, wxPayApiConfig.getCertPath(), wxPayApiConfig.getMchId());
            Map<String, String> map = WxPayKit.xmlToMap(result);
            log.info("发送红包结果: {}", result);
            return JSON.toJSONString(map);
        } catch (Exception e) {
            log.error("发送红包异常", e);
            throw new ApiException("发送红包异常");
        }
    }
}
