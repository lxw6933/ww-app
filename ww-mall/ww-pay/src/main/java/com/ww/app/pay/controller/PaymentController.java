package com.ww.app.pay.controller;

import com.ww.app.pay.dto.PaymentQueryDTO;
import com.ww.app.pay.dto.PaymentRequestDTO;
import com.ww.app.pay.dto.RefundQueryDTO;
import com.ww.app.pay.dto.RefundRequestDTO;
import com.ww.app.pay.service.PaymentService;
import com.ww.app.pay.vo.PaymentQueryResponseVO;
import com.ww.app.pay.vo.PaymentResponseVO;
import com.ww.app.pay.vo.RefundQueryResponseVO;
import com.ww.app.pay.vo.RefundResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 统一支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * 创建支付订单
     */
    @PostMapping("/pay")
    public PaymentResponseVO pay(@RequestBody PaymentRequestDTO requestDTO) {
        return paymentService.pay(requestDTO);
    }
    
    /**
     * 支付页面跳转
     */
    @GetMapping("/pay/redirect")
    public void payRedirect(PaymentRequestDTO requestDTO, HttpServletResponse response) {
        paymentService.payRedirect(requestDTO, response);
    }
    
    /**
     * 支付回调
     */
    @PostMapping("/callback/{channel}")
    public String payCallback(@PathVariable("channel") String channel, HttpServletRequest request) {
        return paymentService.handlePaymentCallback(request);
    }
    
    /**
     * 查询支付结果
     */
    @PostMapping("/query")
    public PaymentQueryResponseVO queryPayment(@RequestBody PaymentQueryDTO queryDTO) {
        return paymentService.queryPayment(queryDTO);
    }
    
    /**
     * 申请退款
     */
    @PostMapping("/refund")
    public RefundResponseVO refund(@RequestBody RefundRequestDTO refundRequestDTO) {
        return paymentService.refund(refundRequestDTO);
    }
    
    /**
     * 退款回调
     */
    @PostMapping("/refund/callback/{channel}")
    public String refundCallback(@PathVariable("channel") String channel, HttpServletRequest request) {
        return paymentService.handleRefundCallback(request);
    }
    
    /**
     * 查询退款结果
     */
    @PostMapping("/refund/query")
    public RefundQueryResponseVO queryRefund(@RequestBody RefundQueryDTO refundQueryDTO) {
        return paymentService.queryRefund(refundQueryDTO);
    }
} 