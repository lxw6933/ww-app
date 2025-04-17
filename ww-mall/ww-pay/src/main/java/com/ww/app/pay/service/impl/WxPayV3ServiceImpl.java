package com.ww.app.pay.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ijpay.core.IJPayHttpResponse;
import com.ijpay.core.enums.AuthTypeEnum;
import com.ijpay.core.enums.RequestMethodEnum;
import com.ijpay.core.kit.HttpKit;
import com.ijpay.core.kit.PayKit;
import com.ijpay.core.kit.WxPayKit;
import com.ijpay.core.utils.DateTimeZoneUtil;
import com.ijpay.wxpay.WxPayApi;
import com.ijpay.wxpay.enums.WxDomainEnum;
import com.ijpay.wxpay.enums.v3.BasePayApiEnum;
import com.ijpay.wxpay.model.v3.*;
import com.ww.app.common.exception.ApiException;
import com.ww.app.pay.properties.WxPayV3Properties;
import com.ww.app.pay.service.WxPayV3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ww
 * @create 2024-06-05- 15:31
 * @description:
 */
@Slf4j
@Service
public class WxPayV3ServiceImpl implements WxPayV3Service {

    private final static int OK = 200;

    @Resource
    private WxPayV3Properties wxPayV3Properties;

    @Override
    public String getSerialNumber() {
        // 获取证书序列号
        X509Certificate certificate = PayKit.getCertificate(wxPayV3Properties.getCertPath());
        if (null == certificate) {
            return null;
        }
        String serialNo = certificate.getSerialNumber().toString(16).toUpperCase();
        log.info("serialNo: {}", serialNo);
        // 提前两天检查证书是否有效
        boolean isValid = PayKit.checkCertificateIsValid(certificate, wxPayV3Properties.getMchId(), -2);
        log.info("证书是否可用 {} 证书有效期为 {}", isValid, DateUtil.format(certificate.getNotAfter(), DatePattern.NORM_DATETIME_PATTERN));
//            System.out.println("输出证书信息:\n" + certificate.toString());
//            // 输出关键信息，截取部分并进行标记
//            System.out.println("证书序列号:" + certificate.getSerialNumber().toString(16));
//            System.out.println("版本号:" + certificate.getVersion());
//            System.out.println("签发者：" + certificate.getIssuerDN());
//            System.out.println("有效起始日期：" + certificate.getNotBefore());
//            System.out.println("有效终止日期：" + certificate.getNotAfter());
//            System.out.println("主体名：" + certificate.getSubjectDN());
//            System.out.println("签名算法：" + certificate.getSigAlgName());
//            System.out.println("签名：" + certificate.getSignature().toString());
        return serialNo;
    }

    @Override
    public String getPlatSerialNumber() {
        // 获取平台证书序列号
        X509Certificate certificate = PayKit.getCertificate(FileUtil.getInputStream(wxPayV3Properties.getPlatformCertPath()));
        if (null == certificate) {
            return null;
        }
        String platSerialNo = certificate.getSerialNumber().toString(16).toUpperCase();
        log.info("platSerialNo: {}", platSerialNo);
        return platSerialNo;
    }

    @Override
    public String queryPayResult(String outTradeNo) {
        try {
            Map<String, String> params = new HashMap<>(16);
            params.put("mchid", wxPayV3Properties.getMchId());
            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethodEnum.GET,
                    WxDomainEnum.CHINA.toString(),
                    String.format(BasePayApiEnum.ORDER_QUERY_BY_OUT_TRADE_NO.toString(), outTradeNo),
                    wxPayV3Properties.getMchId(),
                    getSerialNumber(),
                    null,
                    wxPayV3Properties.getKeyPath(),
                    params,
                    AuthTypeEnum.RSA.getCode()
            );
            log.info("商户[{}]外部交易号[{}]支付结果查询响应[{}]", wxPayV3Properties.getMchId(), outTradeNo, response);
            // 根据证书序列号查询对应的证书来验证签名结果
//				boolean verifySignature = WxPayKit.verifySignature(response, wxPayV3Bean.getPlatformCertPath());
            // 微信公钥验证签名
            boolean verifySignature = WxPayKit.verifyPublicKeySignature(response, wxPayV3Properties.getPlatformCertPath());
            log.info("微信支付查询verifySignature: {}", verifySignature);
            if (response.getStatus() == OK && verifySignature) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("外部交易号[{}]支付结果查询异常", outTradeNo, e);
        }
        return null;
    }

    @Override
    public String appPay() {
        try {
            // 构建app支付订单model
            UnifiedOrderModel unifiedOrderModel = buildWxV3PayModel(BasePayApiEnum.APP_PAY, null);
            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethodEnum.POST,
                    WxDomainEnum.CHINA.toString(),
                    BasePayApiEnum.APP_PAY.toString(),
                    wxPayV3Properties.getMchId(),
                    getSerialNumber(),
                    null,
                    wxPayV3Properties.getKeyPath(),
                    JSONUtil.toJsonStr(unifiedOrderModel),
                    AuthTypeEnum.RSA.getCode()
            );
            return wxV3PayResultHandle(BasePayApiEnum.APP_PAY, response);
        } catch (Exception e) {
            log.error("支付异常", e);
            return null;
        }
    }

    @Override
    public String jsApiPay(String openId) {
        try {
            UnifiedOrderModel unifiedOrderModel = buildWxV3PayModel(BasePayApiEnum.JS_API_PAY, openId);
            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethodEnum.POST,
                    WxDomainEnum.CHINA.toString(),
                    BasePayApiEnum.JS_API_PAY.toString(),
                    wxPayV3Properties.getMchId(),
                    getSerialNumber(),
                    null,
                    wxPayV3Properties.getKeyPath(),
                    JSONUtil.toJsonStr(unifiedOrderModel)
            );
            return wxV3PayResultHandle(BasePayApiEnum.JS_API_PAY, response);
        } catch (Exception e) {
            log.error("支付异常", e);
            return null;
        }
    }

    @Override
    public String wapPay() {
        try {
            UnifiedOrderModel unifiedOrderModel = buildWxV3PayModel(BasePayApiEnum.H5_PAY, null);
            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethodEnum.POST,
                    WxDomainEnum.CHINA.toString(),
                    BasePayApiEnum.H5_PAY.toString(),
                    wxPayV3Properties.getMchId(),
                    getSerialNumber(),
                    null,
                    wxPayV3Properties.getKeyPath(),
                    JSONUtil.toJsonStr(unifiedOrderModel)
            );
            return wxV3PayResultHandle(BasePayApiEnum.H5_PAY, response);
        } catch (Exception e) {
            log.error("支付异常", e);
            return null;
        }
    }

    @Override
    public String refund(String transactionId, String outTradeNo) {
        try {
            // 商户退款单号
            String outRefundNo = PayKit.generateStr();
            // 退款金额[分]
            int refundAmount = 1;
            // 交易单号支付金额[分]
            int payAmount = 1;
            // 退款原因
            String reasonMsg = "测试";

            List<RefundGoodsDetail> list = new ArrayList<>();
            RefundGoodsDetail refundGoodsDetail = new RefundGoodsDetail()
                    // 商户商品编码
                    .setMerchant_goods_id("123")
                    // 商品名称
                    .setGoods_name("ww-app 测试")
                    // 商品单价
                    .setUnit_price(1)
                    // 商品退款金额
                    .setRefund_amount(1)
                    // 商品退款数量
                    .setRefund_quantity(1);
            list.add(refundGoodsDetail);

            RefundModel refundModel = new RefundModel()
                    .setOut_refund_no(outRefundNo)
                    .setReason(reasonMsg)
                    .setNotify_url(wxPayV3Properties.getDomain().concat("/v3/refundNotify"))
                    .setAmount(new RefundAmount().setRefund(refundAmount).setTotal(payAmount).setCurrency("CNY"))
                    .setGoods_detail(list);
            // 交易流水
            if (StrUtil.isNotEmpty(transactionId)) {
                refundModel.setTransaction_id(transactionId);
            }
            // 交易单号
            if (StrUtil.isNotEmpty(outTradeNo)) {
                refundModel.setOut_trade_no(outTradeNo);
            }
            log.info("退款参数 {}", JSONUtil.toJsonStr(refundModel));
            IJPayHttpResponse response = WxPayApi.v3(
                    RequestMethodEnum.POST,
                    WxDomainEnum.CHINA.toString(),
                    BasePayApiEnum.REFUND.toString(),
                    wxPayV3Properties.getMchId(),
                    getSerialNumber(),
                    null,
                    wxPayV3Properties.getKeyPath(),
                    JSONUtil.toJsonStr(refundModel)
            );
            return wxV3PayResultHandle(BasePayApiEnum.REFUND, response);
        } catch (Exception e) {
            log.error("退款异常", e);
            return null;
        }
    }

    @Override
    public void payNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            resolveNotifyReq(request, response, 1);
        } catch (Exception e) {
            log.error("系统异常", e);
        }
    }

    @Override
    public void refundNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            resolveNotifyReq(request, response, 2);
        } catch (Exception e) {
            log.error("系统异常", e);
        }
    }

    private UnifiedOrderModel buildWxV3PayModel(BasePayApiEnum payApiEnum, String openId) {
        try {
            // 支付超时时间
            String timeExpire = DateTimeZoneUtil.dateToTimeZone(System.currentTimeMillis() + 1000 * 60 * 3);
            // 系统支付交易单号
            String outTradeNo = PayKit.generateStr();
            // 构建app支付订单model
            UnifiedOrderModel unifiedOrderModel = new UnifiedOrderModel()
                    .setAppid(wxPayV3Properties.getAppId())
                    .setMchid(wxPayV3Properties.getMchId())
                    .setDescription("ww-app pay")
                    .setOut_trade_no(outTradeNo)
                    .setTime_expire(timeExpire)
                    .setAttach("ww-app pay 附加信息")
                    .setNotify_url(wxPayV3Properties.getDomain().concat("/v3/payNotify"))
                    .setAmount(new Amount().setTotal(1));
            switch (payApiEnum) {
                case APP_PAY:
                    break;
                case JS_API_PAY:
                    unifiedOrderModel.setPayer(new Payer().setOpenid(openId));
                    break;
                case H5_PAY:
                    unifiedOrderModel.setScene_info(
                            new SceneInfo().setPayer_client_ip("").setH5_info(new H5Info().setType("Wap"))
                    );
                    break;
                default:
                    break;
            }
            log.info("[{}]支付参数[{}]", payApiEnum.getDesc(), JSONUtil.toJsonStr(unifiedOrderModel));
            return unifiedOrderModel;
        } catch (Exception e) {
            log.error("构建[{}]支付请求Model异常", payApiEnum.getDesc(), e);
            throw new ApiException("支付请求构建异常");
        }
    }

    private String wxV3PayResultHandle(BasePayApiEnum payApiEnum, IJPayHttpResponse response) {
        log.info("[{}]支付响应 {}", payApiEnum, response);
        try {
            // 根据证书序列号查询对应的证书来验证签名结果
//            boolean verifySignature = WxPayKit.verifySignature(response, wxPayV3Properties.getPlatformCertPath());
            // 微信支付公钥验证签名
            boolean verifySignature = WxPayKit.verifyPublicKeySignature(response, wxPayV3Properties.getPlatformCertPath());
            log.info("[{}]verifySignature: {}", payApiEnum, verifySignature);
            if (response.getStatus() == OK && verifySignature) {
                String body = response.getBody();
                JSONObject jsonObject;
                Map<String, String> map;
                switch (payApiEnum) {
                    case APP_PAY:
                        jsonObject = JSONUtil.parseObj(body);
                        map = WxPayKit.appCreateSign(wxPayV3Properties.getAppId(), wxPayV3Properties.getMchId(), jsonObject.getStr("prepay_id"), wxPayV3Properties.getKeyPath());
                        return JSONUtil.toJsonStr(map);
                    case JS_API_PAY:
                        jsonObject = JSONUtil.parseObj(body);
                        map = WxPayKit.jsApiCreateSign(wxPayV3Properties.getAppId(), jsonObject.getStr("prepay_id"), wxPayV3Properties.getKeyPath());
                        return JSONUtil.toJsonStr(map);
                    case H5_PAY:
                        return JSONUtil.toJsonStr(body);
                    case REFUND:
                        return body;
                    default:
                        throw new ApiException("暂不支持" + payApiEnum.getDesc());
                }
            }
            return null;
        } catch (Exception e) {
            log.error("[{}]支付结果处理异常", payApiEnum, e);
            throw new ApiException("支付结果处理异常");
        }
    }

    private void resolveNotifyReq(HttpServletRequest request, HttpServletResponse response, int type) throws Exception {
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String serialNo = request.getHeader("Wechatpay-Serial");
        String signature = request.getHeader("Wechatpay-Signature");

        String typeStr = type == 1 ? "支付" : "退款";
        log.info("[{}]通知 timestamp:{} nonce:{} serialNo:{} signature:{}", typeStr, timestamp, nonce, serialNo, signature);
        String result = HttpKit.readData(request);
        log.info("[{}]通知密文 {}", typeStr, result);

        // 需要通过证书序列号查找对应的证书，verifyNotify 中有验证证书的序列号
        String plainText = WxPayKit.verifyNotify(serialNo, result, signature, nonce, timestamp,
                wxPayV3Properties.getApiKey3(), wxPayV3Properties.getPlatformCertPath());
        // 微信公钥验证签名并解密
//            String plainText = null;
//            if (StringUtils.equals(serialNo, wxPayV3Properties.getPublicKeyId())) {
//                plainText = WxPayKit.verifyPublicKeyNotify(result, signature, nonce, timestamp,
//                        wxPayV3Properties.getApiKey3(), wxPayV3Properties.getPlatformCertPath());
//            }
        log.info("[{}]通知明文 {}", typeStr, plainText);
        Map<String, String> map = new HashMap<>();
        if (StrUtil.isNotEmpty(plainText)) {
            if (type == 1) {
                // TODO 判断支付通知状态进行业务处理

            } else {
                // TODO 判断退款通知状态进行业务处理

            }
            response.setStatus(OK);
            map.put("code", "SUCCESS");
            map.put("message", "SUCCESS");
        } else {
            response.setStatus(500);
            map.put("code", "ERROR");
            map.put("message", "签名错误");
        }
        response.setHeader("Content-type", ContentType.JSON.toString());
        response.getOutputStream().write(JSONUtil.toJsonStr(map).getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }
}
