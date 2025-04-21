package com.ww.app.pay.service;

import com.ww.app.pay.dto.PaymentCallbackDTO;
import com.ww.app.pay.dto.PaymentQueryDTO;
import com.ww.app.pay.dto.PaymentRequestDTO;
import com.ww.app.pay.dto.RefundCallbackDTO;
import com.ww.app.pay.dto.RefundQueryDTO;
import com.ww.app.pay.dto.RefundRequestDTO;
import com.ww.app.pay.vo.PaymentQueryResponseVO;
import com.ww.app.pay.vo.PaymentResponseVO;
import com.ww.app.pay.vo.RefundQueryResponseVO;
import com.ww.app.pay.vo.RefundResponseVO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 统一支付服务接口
 */
public interface PaymentService {
    
    /**
     * 创建支付订单
     * @param requestDTO 支付请求参数
     * @return 支付响应结果
     */
    PaymentResponseVO pay(PaymentRequestDTO requestDTO);
    
    /**
     * 处理支付页面跳转
     * @param requestDTO 支付请求参数
     * @param response HTTP响应对象
     */
    void payRedirect(PaymentRequestDTO requestDTO, HttpServletResponse response);
    
    /**
     * 处理支付回调
     * @param request HTTP请求对象
     * @return 支付回调处理结果
     */
    String handlePaymentCallback(HttpServletRequest request);
    
    /**
     * 查询支付结果
     * @param queryDTO 支付查询参数
     * @return 支付查询结果
     */
    PaymentQueryResponseVO queryPayment(PaymentQueryDTO queryDTO);
    
    /**
     * 申请退款
     * @param refundRequestDTO 退款请求参数
     * @return 退款响应结果
     */
    RefundResponseVO refund(RefundRequestDTO refundRequestDTO);
    
    /**
     * 处理退款回调
     * @param request HTTP请求对象
     * @return 退款回调处理结果
     */
    String handleRefundCallback(HttpServletRequest request);
    
    /**
     * 查询退款结果
     * @param refundQueryDTO 退款查询参数
     * @return 退款查询结果
     */
    RefundQueryResponseVO queryRefund(RefundQueryDTO refundQueryDTO);
} 