package com.ww.app.pay.service.impl;

import com.ww.app.pay.common.PaymentStrategy;
import com.ww.app.pay.common.PaymentStrategyFactory;
import com.ww.app.pay.dto.PaymentCallbackDTO;
import com.ww.app.pay.dto.PaymentQueryDTO;
import com.ww.app.pay.dto.PaymentRequestDTO;
import com.ww.app.pay.dto.RefundCallbackDTO;
import com.ww.app.pay.dto.RefundQueryDTO;
import com.ww.app.pay.dto.RefundRequestDTO;
import com.ww.app.pay.service.PaymentService;
import com.ww.app.pay.vo.PaymentQueryResponseVO;
import com.ww.app.pay.vo.PaymentResponseVO;
import com.ww.app.pay.vo.RefundQueryResponseVO;
import com.ww.app.pay.vo.RefundResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 统一支付服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    
    private final PaymentStrategyFactory strategyFactory;
    
    @Override
    public PaymentResponseVO pay(PaymentRequestDTO requestDTO) {
        PaymentStrategy strategy = strategyFactory.getStrategy(requestDTO.getPayChannel());
        log.info("创建支付订单，商户订单号：{}，支付渠道：{}，支付方式：{}", 
                requestDTO.getOutTradeNo(), requestDTO.getPayChannel(), requestDTO.getPayType());
        return strategy.pay(requestDTO);
    }
    
    @Override
    public void payRedirect(PaymentRequestDTO requestDTO, HttpServletResponse response) {
        PaymentStrategy strategy = strategyFactory.getStrategy(requestDTO.getPayChannel());
        log.info("支付页面跳转，商户订单号：{}，支付渠道：{}，支付方式：{}", 
                requestDTO.getOutTradeNo(), requestDTO.getPayChannel(), requestDTO.getPayType());
        strategy.payRedirect(requestDTO, response);
    }
    
    @Override
    public String handlePaymentCallback(HttpServletRequest request) {
        // 需要从request中提取支付渠道信息，这里假设从请求路径中提取
        String requestURI = request.getRequestURI();
        String[] pathParts = requestURI.split("/");
        String channelCode = pathParts[pathParts.length - 2]; // 假设路径格式为 /pay/callback/{channel}
        
        // 根据渠道获取对应的支付策略
        PaymentStrategy strategy = strategyFactory.getStrategy(com.ww.app.pay.enums.PayChannelEnum.getByCode(channelCode));
        log.info("处理支付回调，支付渠道：{}", channelCode);
        
        // 处理回调
        PaymentCallbackDTO callbackDTO = strategy.handlePaymentCallback(request);
        
        // 处理业务逻辑，例如更新订单状态等
        log.info("支付回调处理完成，商户订单号：{}，支付状态：{}", callbackDTO.getOutTradeNo(), callbackDTO.getPayStatus());
        
        // 返回给支付平台的响应
        return callbackDTO.getResponseData();
    }
    
    @Override
    public PaymentQueryResponseVO queryPayment(PaymentQueryDTO queryDTO) {
        PaymentStrategy strategy = strategyFactory.getStrategy(queryDTO.getPayChannel());
        log.info("查询支付结果，商户订单号：{}，支付渠道：{}", queryDTO.getOutTradeNo(), queryDTO.getPayChannel());
        return strategy.queryPayment(queryDTO);
    }
    
    @Override
    public RefundResponseVO refund(RefundRequestDTO refundRequestDTO) {
        PaymentStrategy strategy = strategyFactory.getStrategy(refundRequestDTO.getPayChannel());
        log.info("申请退款，商户订单号：{}，退款单号：{}，支付渠道：{}", 
                refundRequestDTO.getOutTradeNo(), refundRequestDTO.getOutRefundNo(), refundRequestDTO.getPayChannel());
        return strategy.refund(refundRequestDTO);
    }
    
    @Override
    public String handleRefundCallback(HttpServletRequest request) {
        // 需要从request中提取支付渠道信息，这里假设从请求路径中提取
        String requestURI = request.getRequestURI();
        String[] pathParts = requestURI.split("/");
        String channelCode = pathParts[pathParts.length - 2]; // 假设路径格式为 /refund/callback/{channel}
        
        // 根据渠道获取对应的支付策略
        PaymentStrategy strategy = strategyFactory.getStrategy(com.ww.app.pay.enums.PayChannelEnum.getByCode(channelCode));
        log.info("处理退款回调，支付渠道：{}", channelCode);
        
        // 处理回调
        RefundCallbackDTO callbackDTO = strategy.handleRefundCallback(request);
        
        // 处理业务逻辑，例如更新退款状态等
        log.info("退款回调处理完成，商户订单号：{}，退款单号：{}，退款状态：{}", 
                callbackDTO.getOutTradeNo(), callbackDTO.getOutRefundNo(), callbackDTO.getRefundStatus());
        
        // 返回给支付平台的响应
        return callbackDTO.getResponseData();
    }
    
    @Override
    public RefundQueryResponseVO queryRefund(RefundQueryDTO refundQueryDTO) {
        PaymentStrategy strategy = strategyFactory.getStrategy(refundQueryDTO.getPayChannel());
        log.info("查询退款结果，商户订单号：{}，退款单号：{}，支付渠道：{}", 
                refundQueryDTO.getOutTradeNo(), refundQueryDTO.getOutRefundNo(), refundQueryDTO.getPayChannel());
        return strategy.queryRefund(refundQueryDTO);
    }
} 