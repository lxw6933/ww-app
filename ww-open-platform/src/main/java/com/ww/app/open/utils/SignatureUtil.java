package com.ww.app.open.utils;

import com.alibaba.fastjson.JSON;
import com.ww.app.open.common.BaseOpenRequest;

/**
 * 签名工具类
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 统一管理签名相关的工具方法，避免重复代码
 */
public class SignatureUtil {

    private SignatureUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 构建签名数据
     * 签名规则：sysCode + appCode + methodCode + transId + data的JSON字符串
     * 
     * @param request 请求对象
     * @return 签名数据字符串
     */
    public static String buildSignData(BaseOpenRequest<?> request) {
        return request.getSysCode() + request.getAppCode() + 
               request.getMethodCode() + request.getTransId() + 
               JSON.toJSONString(request.getData());
    }

    /**
     * 构建签名数据（使用指定参数）
     * 
     * @param sysCode 商户编码
     * @param appCode 应用编码
     * @param methodCode 方法编码
     * @param transId 流水号
     * @param data 业务数据
     * @return 签名数据字符串
     */
    public static String buildSignData(String sysCode, String appCode, 
                                      String methodCode, String transId, Object data) {
        return sysCode + appCode + methodCode + transId + JSON.toJSONString(data);
    }
}

