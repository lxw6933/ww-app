package com.ww.app.pay.common.impl;

import com.ijpay.alipay.AliPayApiConfig;
import com.ww.app.pay.common.PaymentStrategy;
import com.ww.app.pay.dto.PaymentCallbackDTO;
import com.ww.app.pay.dto.PaymentQueryDTO;
import com.ww.app.pay.dto.PaymentRequestDTO;
import com.ww.app.pay.dto.RefundCallbackDTO;
import com.ww.app.pay.dto.RefundQueryDTO;
import com.ww.app.pay.dto.RefundRequestDTO;
import com.ww.app.pay.enums.PayChannelEnum;
import com.ww.app.pay.enums.PayStatusEnum;
import com.ww.app.pay.enums.PayTypeEnum;
import com.ww.app.pay.enums.RefundStatusEnum;
import com.ww.app.pay.properties.AliPayProperties;
import com.ww.app.pay.vo.PaymentQueryResponseVO;
import com.ww.app.pay.vo.PaymentResponseVO;
import com.ww.app.pay.vo.RefundQueryResponseVO;
import com.ww.app.pay.vo.RefundResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付策略实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AliPayStrategy implements PaymentStrategy {
    
    private final AliPayProperties aliPayProperties;
    
    @Override
    public PayChannelEnum getPayChannel() {
        return PayChannelEnum.ALIPAY;
    }
    
    @Override
    public PaymentResponseVO pay(PaymentRequestDTO requestDTO) {
        log.info("支付宝支付，商户订单号：{}", requestDTO.getOutTradeNo());
        PaymentResponseVO responseVO = new PaymentResponseVO();
        responseVO.setOutTradeNo(requestDTO.getOutTradeNo())
                .setPayChannel(PayChannelEnum.ALIPAY)
                .setPayType(requestDTO.getPayType())
                .setAmount(requestDTO.getAmount());
        
        PayTypeEnum payType = requestDTO.getPayType();
        // 根据支付方式生成不同的支付参数
        if (payType == PayTypeEnum.APP) {
            // APP支付
            responseVO.setPayParams(generateAppPayParams(requestDTO));
        } else if (payType == PayTypeEnum.WAP) {
            // H5支付
            responseVO.setPayUrl(generateWapPayUrl(requestDTO));
        } else if (payType == PayTypeEnum.PC) {
            // PC支付
            responseVO.setFormHtml(generatePcPayForm(requestDTO));
        } else if (payType == PayTypeEnum.NATIVE) {
            // 扫码支付
            responseVO.setQrCodeUrl(generateQrCodeUrl(requestDTO));
        } else {
            throw new IllegalArgumentException("不支持的支付方式：" + payType);
        }
        
        return responseVO;
    }
    
    @Override
    public void payRedirect(PaymentRequestDTO requestDTO, HttpServletResponse response) {
        try {
            PayTypeEnum payType = requestDTO.getPayType();
            if (payType == PayTypeEnum.WAP) {
                // 重定向到WAP支付页面
                response.sendRedirect(generateWapPayUrl(requestDTO));
            } else if (payType == PayTypeEnum.PC) {
                // 输出PC支付表单
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(generatePcPayForm(requestDTO));
            } else {
                throw new IllegalArgumentException("不支持的支付跳转方式：" + payType);
            }
        } catch (Exception e) {
            log.error("支付宝支付跳转异常", e);
            throw new RuntimeException("支付宝支付跳转异常", e);
        }
    }
    
    @Override
    public PaymentCallbackDTO handlePaymentCallback(HttpServletRequest request) {
        log.info("处理支付宝支付回调");
        // 解析回调参数
        Map<String, String> params = parseRequestParams(request);
        
        // 验证签名
        boolean verifyResult = verifySign(params);
        if (!verifyResult) {
            log.error("支付宝支付回调签名验证失败");
            throw new RuntimeException("支付宝支付回调签名验证失败");
        }
        
        // 解析回调结果
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        
        // 根据交易状态判断支付结果
        PayStatusEnum payStatus;
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            payStatus = PayStatusEnum.PAY_SUCCESS;
        } else if ("TRADE_CLOSED".equals(tradeStatus)) {
            payStatus = PayStatusEnum.CLOSED;
        } else {
            payStatus = PayStatusEnum.PAY_FAILED;
        }
        
        // 构建回调结果
        PaymentCallbackDTO callbackDTO = new PaymentCallbackDTO()
                .setPayChannel(PayChannelEnum.ALIPAY)
                .setOutTradeNo(outTradeNo)
                .setTradeNo(tradeNo)
                .setPayStatus(payStatus)
                .setAmount(new BigDecimal(params.get("total_amount")))
                .setPayTime(LocalDateTime.now()) // 实际应从回调参数中解析时间
                .setBuyerId(params.get("buyer_id"))
                .setRawData(params)
                .setResponseData("success"); // 支付宝规定接收到回调需返回 success 字符串
        
        return callbackDTO;
    }
    
    @Override
    public PaymentQueryResponseVO queryPayment(PaymentQueryDTO queryDTO) {
        log.info("查询支付宝支付结果，商户订单号：{}", queryDTO.getOutTradeNo());
        
        // 调用支付宝查询接口
        // 这里模拟查询结果
        PaymentQueryResponseVO responseVO = new PaymentQueryResponseVO()
                .setOutTradeNo(queryDTO.getOutTradeNo())
                .setTradeNo("2023061022001473890123456789") // 模拟支付宝交易号
                .setPayChannel(PayChannelEnum.ALIPAY)
                .setPayStatus(PayStatusEnum.PAY_SUCCESS) // 模拟支付成功
                .setAmount(new BigDecimal("100")) // 模拟支付金额
                .setPayTime(LocalDateTime.now()) // 模拟支付时间
                .setBuyerId("2088102122542435") // 模拟买家ID
                .setRawData("{\"code\":\"10000\",\"msg\":\"Success\",\"trade_no\":\"2023061022001473890123456789\",\"out_trade_no\":\"" + queryDTO.getOutTradeNo() + "\"}"); // 模拟原始响应
        
        return responseVO;
    }
    
    @Override
    public RefundResponseVO refund(RefundRequestDTO refundRequestDTO) {
        log.info("支付宝退款，商户订单号：{}，退款单号：{}", refundRequestDTO.getOutTradeNo(), refundRequestDTO.getOutRefundNo());
        
        // 调用支付宝退款接口
        // 这里模拟退款结果
        RefundResponseVO responseVO = new RefundResponseVO()
                .setOutTradeNo(refundRequestDTO.getOutTradeNo())
                .setTradeNo(refundRequestDTO.getTradeNo())
                .setOutRefundNo(refundRequestDTO.getOutRefundNo())
                .setRefundId("2023061022001473890123456789R") // 模拟支付宝退款单号
                .setPayChannel(PayChannelEnum.ALIPAY)
                .setRefundStatus(RefundStatusEnum.REFUND_SUCCESS) // 模拟退款成功
                .setRefundAmount(refundRequestDTO.getRefundAmount())
                .setRawData("{\"code\":\"10000\",\"msg\":\"Success\",\"trade_no\":\"" + refundRequestDTO.getTradeNo() + "\",\"out_trade_no\":\"" + refundRequestDTO.getOutTradeNo() + "\"}"); // 模拟原始响应
        
        return responseVO;
    }
    
    @Override
    public RefundCallbackDTO handleRefundCallback(HttpServletRequest request) {
        log.info("处理支付宝退款回调");
        // 解析回调参数
        Map<String, String> params = parseRequestParams(request);
        
        // 验证签名
        boolean verifyResult = verifySign(params);
        if (!verifyResult) {
            log.error("支付宝退款回调签名验证失败");
            throw new RuntimeException("支付宝退款回调签名验证失败");
        }
        
        // 解析回调结果
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String outRefundNo = params.get("out_request_no");
        
        // 构建回调结果
        RefundCallbackDTO callbackDTO = new RefundCallbackDTO()
                .setPayChannel(PayChannelEnum.ALIPAY)
                .setOutTradeNo(outTradeNo)
                .setTradeNo(tradeNo)
                .setOutRefundNo(outRefundNo)
                .setRefundId(params.get("refund_id"))
                .setRefundStatus(RefundStatusEnum.REFUND_SUCCESS) // 假设退款成功
                .setRefundAmount(new BigDecimal(params.get("refund_amount")))
                .setRefundTime(LocalDateTime.now()) // 实际应从回调参数中解析时间
                .setRawData(params)
                .setResponseData("success"); // 支付宝规定接收到回调需返回 success 字符串
        
        return callbackDTO;
    }
    
    @Override
    public RefundQueryResponseVO queryRefund(RefundQueryDTO refundQueryDTO) {
        log.info("查询支付宝退款结果，商户订单号：{}，退款单号：{}", refundQueryDTO.getOutTradeNo(), refundQueryDTO.getOutRefundNo());
        
        // 调用支付宝退款查询接口
        // 这里模拟查询结果
        RefundQueryResponseVO responseVO = new RefundQueryResponseVO()
                .setOutTradeNo(refundQueryDTO.getOutTradeNo())
                .setTradeNo(refundQueryDTO.getTradeNo())
                .setOutRefundNo(refundQueryDTO.getOutRefundNo())
                .setRefundId("2023061022001473890123456789R") // 模拟支付宝退款单号
                .setPayChannel(PayChannelEnum.ALIPAY)
                .setRefundStatus(RefundStatusEnum.REFUND_SUCCESS) // 模拟退款成功
                .setRefundAmount(new BigDecimal("100")) // 模拟退款金额
                .setRefundTime(LocalDateTime.now()) // 模拟退款时间
                .setRawData("{\"code\":\"10000\",\"msg\":\"Success\",\"trade_no\":\"" + refundQueryDTO.getTradeNo() + "\",\"out_trade_no\":\"" + refundQueryDTO.getOutTradeNo() + "\"}"); // 模拟原始响应
        
        return responseVO;
    }
    
    /**
     * 获取支付宝API配置
     */
    private AliPayApiConfig getApiConfig() {
        // 实际实现中需要从配置中读取
        return AliPayApiConfig.builder()
                .setAppId(aliPayProperties.getAppId())
                .setPrivateKey(aliPayProperties.getPrivateKey())
                .setAliPayPublicKey(aliPayProperties.getPublicKey())
                .setCharset("UTF-8")
                .setSignType("RSA2")
                .setServiceUrl(aliPayProperties.getServerUrl())
                .build();
    }
    
    /**
     * 生成APP支付参数
     */
    private Map<String, String> generateAppPayParams(PaymentRequestDTO requestDTO) {
        // 实际实现中调用支付宝SDK生成APP支付参数
        Map<String, String> params = new HashMap<>();
        params.put("app_id", aliPayProperties.getAppId());
        params.put("method", "alipay.trade.app.pay");
        params.put("charset", "UTF-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", "2023-06-10 12:00:00");
        params.put("version", "1.0");
        params.put("notify_url", requestDTO.getNotifyUrl());
        params.put("biz_content", "{\"out_trade_no\":\"" + requestDTO.getOutTradeNo() + "\",\"total_amount\":\"" + requestDTO.getAmount() + "\",\"subject\":\"" + requestDTO.getSubject() + "\",\"product_code\":\"QUICK_MSECURITY_PAY\"}");
        return params;
    }
    
    /**
     * 生成WAP支付URL
     */
    private String generateWapPayUrl(PaymentRequestDTO requestDTO) {
        // 实际实现中调用支付宝SDK生成WAP支付URL
        return "https://openapi.alipay.com/gateway.do?app_id=" + aliPayProperties.getAppId() + "&method=alipay.trade.wap.pay&charset=UTF-8&sign_type=RSA2&timestamp=2023-06-10%2012:00:00&version=1.0&notify_url=" + requestDTO.getNotifyUrl() + "&return_url=" + requestDTO.getReturnUrl() + "&biz_content=%7B%22out_trade_no%22%3A%22" + requestDTO.getOutTradeNo() + "%22%2C%22total_amount%22%3A%22" + requestDTO.getAmount() + "%22%2C%22subject%22%3A%22" + requestDTO.getSubject() + "%22%2C%22product_code%22%3A%22QUICK_WAP_WAY%22%7D";
    }
    
    /**
     * 生成PC支付表单
     */
    private String generatePcPayForm(PaymentRequestDTO requestDTO) {
        // 实际实现中调用支付宝SDK生成PC支付表单
        return "<form name=\"punchout_form\" method=\"post\" action=\"https://openapi.alipay.com/gateway.do?charset=UTF-8\">\n" +
                "<input type=\"hidden\" name=\"app_id\" value=\"" + aliPayProperties.getAppId() + "\">\n" +
                "<input type=\"hidden\" name=\"method\" value=\"alipay.trade.page.pay\">\n" +
                "<input type=\"hidden\" name=\"charset\" value=\"UTF-8\">\n" +
                "<input type=\"hidden\" name=\"sign_type\" value=\"RSA2\">\n" +
                "<input type=\"hidden\" name=\"timestamp\" value=\"2023-06-10 12:00:00\">\n" +
                "<input type=\"hidden\" name=\"version\" value=\"1.0\">\n" +
                "<input type=\"hidden\" name=\"notify_url\" value=\"" + requestDTO.getNotifyUrl() + "\">\n" +
                "<input type=\"hidden\" name=\"return_url\" value=\"" + requestDTO.getReturnUrl() + "\">\n" +
                "<input type=\"hidden\" name=\"biz_content\" value=\"{&quot;out_trade_no&quot;:&quot;" + requestDTO.getOutTradeNo() + "&quot;,&quot;total_amount&quot;:&quot;" + requestDTO.getAmount() + "&quot;,&quot;subject&quot;:&quot;" + requestDTO.getSubject() + "&quot;,&quot;product_code&quot;:&quot;FAST_INSTANT_TRADE_PAY&quot;}\">\n" +
                "<input type=\"submit\" value=\"立即支付\" style=\"display:none\" >\n" +
                "</form>\n" +
                "<script>document.forms[0].submit();</script>";
    }
    
    /**
     * 生成二维码支付URL
     */
    private String generateQrCodeUrl(PaymentRequestDTO requestDTO) {
        // 实际实现中调用支付宝SDK生成二维码支付URL
        return "https://qr.alipay.com/bax12345678901234";
    }
    
    /**
     * 解析请求参数
     */
    private Map<String, String> parseRequestParams(HttpServletRequest request) {
        // 实际实现中解析request中的参数
        Map<String, String> params = new HashMap<>();
        // ... 解析逻辑
        return params;
    }
    
    /**
     * 验证签名
     */
    private boolean verifySign(Map<String, String> params) {
        // 实际实现中调用支付宝SDK验证签名
        return true;
    }
} 