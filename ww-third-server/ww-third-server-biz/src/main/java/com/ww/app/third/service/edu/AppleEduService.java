package com.ww.app.third.service.edu;

import com.ww.app.third.edu.req.*;
import com.ww.app.third.edu.res.MpnListResultVO;
import com.ww.app.third.edu.res.UploadInvoiceResultVO;
import com.ww.app.third.edu.res.UploadPicturesResultVO;

/**
 * @author ww
 * @create 2023-03-27- 17:23
 * @description:
 */
public interface AppleEduService {

    /**
     * 学信⽹验证
     *
     * @param reqBO 请求参数
     * @return 是否通过
     */
    boolean xxwValid(XxwValidReqBO reqBO);

    /**
     * 额度检查
     *
     * @param reqBO 请求参数
     * @return 剩余额度
     */
    int quotaCheck(QuotaCheckReqBO reqBO);

    /**
     * 提交订单至edu系统
     *
     * @param reqBO 请求参数
     * @return edu订单编号
     */
    String submitOrderToEdu(SubmitOrderReqBO reqBO);

    /**
     * 取消订单至edu系统
     *
     * @param reqBO 请求参数
     * @return 是否成功
     */
    boolean cancelOrderToEdu(CancelOrderReqBO reqBO);

    /**
     * mpn列表
     *
     * @param reqBO 请求参数
     * @return MpnListResultVO
     */
    MpnListResultVO mpnList(MpnListReqBO reqBO);

    /**
     * 确认订单
     *
     * @param reqBO 请求参数
     * @return 是否确认成功
     */
    boolean confirmOrderToEdu(ConfirmOrderReqBO reqBO);

    /**
     * 上传发票
     *
     * @param reqBO 请求参数
     * @return UploadInvoiceResultVO
     */
    UploadInvoiceResultVO uploadInvoiceToEdu(UploadInvoiceReqBO reqBO);

    /**
     * 上传图片
     *
     * @param url 请求参数
     * @return UploadInvoiceResultVO
     */
    UploadPicturesResultVO uploadPicturesToEdu(String url);

}
