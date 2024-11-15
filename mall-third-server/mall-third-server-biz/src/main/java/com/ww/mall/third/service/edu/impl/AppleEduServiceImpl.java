package com.ww.mall.third.service.edu.impl;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson.JSON;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.third.constant.AppleEduProperties;
import com.ww.mall.third.edu.BaseAppleEduReqBO;
import com.ww.mall.third.edu.BaseAppleEduResultVO;
import com.ww.mall.third.edu.req.*;
import com.ww.mall.third.edu.res.*;
import com.ww.mall.third.service.edu.AppleEduService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

/**
 * @author ww
 * @create 2023-03-28- 09:05
 * @description:
 */
@Slf4j
@Service
public class AppleEduServiceImpl implements AppleEduService {

    @Autowired
    private AppleEduProperties appleEduProperties;

    @Autowired
    private RestTemplate okRestTemplate;

    private static final String EDU_SUCCESS_CODE = "0";

    private static final List<String> SYSTEM_ERROR_CODES = Arrays.asList("1000", "1103", "1102");

    private static final String XXW_VALID_CMD = "IdentityCheck";

    private static final String QUOTA_CHECK_CMD = "QuotaCheck";

    private static final String SUBMIT_ORDER_CMD = "PlaceOrder";

    private static final String CANCEL_ORDER_CMD = "CancelOrder";

    private static final String CONFIRM_ORDER_CMD = "ConfirmOrder";

    private static final String UPLOAD_INVOICE_CMD = "UploadInvoice";

    private static final String MPN_LIST_CMD = "ListMpn";

    // 教育接口url
    private String getAppleEduReqUrl() {
        return appleEduProperties.getEduDomain() + appleEduProperties.getReqUri();
    }

    // 上传图片url
    private String getAppleEduUploadReqUrl() {
        return appleEduProperties.getEduDomain() + appleEduProperties.getUploadImgUri();
    }

    // 教育接口请求头
    private HttpHeaders getHttpHeaders() {
        // 设置请求头信息
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }

    // 教育上传图片接口请求头
    private HttpHeaders getUploadFileHttpHeads() {
        // 设置请求头信息
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        return httpHeaders;
    }

    // 封装请求参数
    private <T> BaseAppleEduReqBO<T> getBaseReqBO(T reqBO) {
        BaseAppleEduReqBO<T> baseReqBO = new BaseAppleEduReqBO<>();
        baseReqBO.setAppId(appleEduProperties.getAppId());
        baseReqBO.setTimestamp(String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"))));
        if (reqBO instanceof XxwValidReqBO) {
            ((XxwValidReqBO) reqBO).setCmd(XXW_VALID_CMD);
        } else if (reqBO instanceof QuotaCheckReqBO) {
            ((QuotaCheckReqBO) reqBO).setCmd(QUOTA_CHECK_CMD);
        } else if (reqBO instanceof SubmitOrderReqBO) {
            ((SubmitOrderReqBO) reqBO).setCmd(SUBMIT_ORDER_CMD);
        } else if (reqBO instanceof CancelOrderReqBO) {
            ((CancelOrderReqBO) reqBO).setCmd(CANCEL_ORDER_CMD);
        } else if (reqBO instanceof MpnListReqBO) {
            ((MpnListReqBO) reqBO).setCmd(MPN_LIST_CMD);
        } else if (reqBO instanceof ConfirmOrderReqBO) {
            ((ConfirmOrderReqBO) reqBO).setCmd(CONFIRM_ORDER_CMD);
        } else if (reqBO instanceof UploadInvoiceReqBO) {
            ((UploadInvoiceReqBO) reqBO).setCmd(UPLOAD_INVOICE_CMD);
        }
        baseReqBO.setPayload(reqBO);
        baseReqBO.setDigest(baseReqBO.getSha256(appleEduProperties.getSecurity()));
        log.info("edu请求参数：{}", JSON.toJSONString(baseReqBO));
        return baseReqBO;
    }

    @Override
    public boolean xxwValid(XxwValidReqBO reqBO) {
        // 请求接口
        String appleEduReqUrl = getAppleEduReqUrl();
        // 封装请求参数数据
        BaseAppleEduReqBO<XxwValidReqBO> baseReqBO = getBaseReqBO(reqBO);
        // 远程调用
        HttpEntity<BaseAppleEduReqBO<XxwValidReqBO>> httpEntity = new HttpEntity<>(baseReqBO, getHttpHeaders());
        ParameterizedTypeReference<BaseAppleEduResultVO<XxwValidResultVO>> typeRef = new ParameterizedTypeReference<BaseAppleEduResultVO<XxwValidResultVO>>() {};
        ResponseEntity<BaseAppleEduResultVO<XxwValidResultVO>> responseEntity = okRestTemplate.exchange(appleEduReqUrl, HttpMethod.POST, httpEntity, typeRef);
        // 远程调用结果处理
        resultHandler(baseReqBO, responseEntity, reqBO.getCmd());
        return "Valid".equals(responseEntity.getBody().getPayload().getStatus());
    }

    @Override
    public int quotaCheck(QuotaCheckReqBO reqBO) {
        // 请求接口
        String appleEduReqUrl = getAppleEduReqUrl();
        // 封装请求参数数据
        BaseAppleEduReqBO<QuotaCheckReqBO> baseReqBO = getBaseReqBO(reqBO);
        // 远程调用
        HttpEntity<BaseAppleEduReqBO<QuotaCheckReqBO>> httpEntity = new HttpEntity<>(baseReqBO, getHttpHeaders());
        ParameterizedTypeReference<BaseAppleEduResultVO<QuotaCheckResultVO>> typeRef = new ParameterizedTypeReference<BaseAppleEduResultVO<QuotaCheckResultVO>>() {};
        ResponseEntity<BaseAppleEduResultVO<QuotaCheckResultVO>> responseEntity = okRestTemplate.exchange(appleEduReqUrl, HttpMethod.POST, httpEntity, typeRef);
        // 远程调用结果处理
        resultHandler(baseReqBO, responseEntity, reqBO.getCmd());
        return responseEntity.getBody().getPayload().getQuota();
    }

    @Override
    public String submitOrderToEdu(SubmitOrderReqBO reqBO) {
        // 请求接口
        String appleEduReqUrl = getAppleEduReqUrl();
        // 封装请求参数数据
        BaseAppleEduReqBO<SubmitOrderReqBO> baseReqBO = getBaseReqBO(reqBO);
        // 远程调用
        HttpEntity<BaseAppleEduReqBO<SubmitOrderReqBO>> httpEntity = new HttpEntity<>(baseReqBO, getHttpHeaders());
        ParameterizedTypeReference<BaseAppleEduResultVO<SubmitOrderResultVO>> typeRef = new ParameterizedTypeReference<BaseAppleEduResultVO<SubmitOrderResultVO>>() {};
        ResponseEntity<BaseAppleEduResultVO<SubmitOrderResultVO>> responseEntity = okRestTemplate.exchange(appleEduReqUrl, HttpMethod.POST, httpEntity, typeRef);
        // 远程调用结果处理
        resultHandler(baseReqBO, responseEntity, reqBO.getCmd());
        return responseEntity.getBody().getPayload().getProductOrderNo();
    }

    @Override
    public boolean cancelOrderToEdu(CancelOrderReqBO reqBO) {
        // 请求接口
        String appleEduReqUrl = getAppleEduReqUrl();
        // 封装请求参数数据
        BaseAppleEduReqBO<CancelOrderReqBO> baseReqBO = getBaseReqBO(reqBO);
        // 远程调用
        HttpEntity<BaseAppleEduReqBO<CancelOrderReqBO>> httpEntity = new HttpEntity<>(baseReqBO, getHttpHeaders());
        ParameterizedTypeReference<BaseAppleEduResultVO<CancelOrderResultVO>> typeRef = new ParameterizedTypeReference<BaseAppleEduResultVO<CancelOrderResultVO>>() {};
        ResponseEntity<BaseAppleEduResultVO<CancelOrderResultVO>> responseEntity = okRestTemplate.exchange(appleEduReqUrl, HttpMethod.POST, httpEntity, typeRef);
        // 远程调用结果处理
        resultHandler(baseReqBO, responseEntity, reqBO.getCmd());
        return "success".equals(responseEntity.getBody().getPayload().getStatus());
    }

    @Override
    public MpnListResultVO mpnList(MpnListReqBO reqBO) {
        // 请求接口
        String appleEduReqUrl = getAppleEduReqUrl();
        // 封装请求参数数据
        BaseAppleEduReqBO<MpnListReqBO> baseReqBO = getBaseReqBO(reqBO);
        // 远程调用
        HttpEntity<BaseAppleEduReqBO<MpnListReqBO>> httpEntity = new HttpEntity<>(baseReqBO, getHttpHeaders());
        ParameterizedTypeReference<BaseAppleEduResultVO<MpnListResultVO>> typeRef = new ParameterizedTypeReference<BaseAppleEduResultVO<MpnListResultVO>>() {};
        ResponseEntity<BaseAppleEduResultVO<MpnListResultVO>> responseEntity = okRestTemplate.exchange(appleEduReqUrl, HttpMethod.POST, httpEntity, typeRef);
        // 远程调用结果处理
        resultHandler(baseReqBO, responseEntity, reqBO.getCmd());
        return responseEntity.getBody().getPayload();
    }

    @Override
    public boolean confirmOrderToEdu(ConfirmOrderReqBO reqBO) {
        // 请求接口
        String appleEduReqUrl = getAppleEduReqUrl();
        // 封装请求参数数据
        BaseAppleEduReqBO<ConfirmOrderReqBO> baseReqBO = getBaseReqBO(reqBO);
        // 远程调用
        HttpEntity<BaseAppleEduReqBO<ConfirmOrderReqBO>> httpEntity = new HttpEntity<>(baseReqBO, getHttpHeaders());
        ParameterizedTypeReference<BaseAppleEduResultVO<ConfirmOrderResultVO>> typeRef = new ParameterizedTypeReference<BaseAppleEduResultVO<ConfirmOrderResultVO>>() {};
        ResponseEntity<BaseAppleEduResultVO<ConfirmOrderResultVO>> responseEntity = okRestTemplate.exchange(appleEduReqUrl, HttpMethod.POST, httpEntity, typeRef);
        // 远程调用结果处理
        resultHandler(baseReqBO, responseEntity, reqBO.getCmd());
        return "success".equals(responseEntity.getBody().getPayload().getStatus());
    }

    @Override
        public UploadInvoiceResultVO uploadInvoiceToEdu(UploadInvoiceReqBO reqBO) {
        // 请求接口
        String appleEduReqUrl = getAppleEduReqUrl();
        // 封装请求参数数据
        BaseAppleEduReqBO<UploadInvoiceReqBO> baseReqBO = getBaseReqBO(reqBO);
        // 远程调用
        HttpEntity<BaseAppleEduReqBO<UploadInvoiceReqBO>> httpEntity = new HttpEntity<>(baseReqBO, getHttpHeaders());
        ParameterizedTypeReference<BaseAppleEduResultVO<UploadInvoiceResultVO>> typeRef = new ParameterizedTypeReference<BaseAppleEduResultVO<UploadInvoiceResultVO>>() {
        };
        ResponseEntity<BaseAppleEduResultVO<UploadInvoiceResultVO>> responseEntity = okRestTemplate.exchange(appleEduReqUrl, HttpMethod.POST, httpEntity, typeRef);
        // 远程调用结果处理
        resultHandler(baseReqBO, responseEntity,reqBO.getCmd());
        return responseEntity.getBody().getPayload();
    }

    @Override
    public UploadPicturesResultVO uploadPicturesToEdu(String url) {
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        UrlResource resource;
        try {
            resource = new UrlResource(url) {
                @Override
                public String getFilename() {
                    return UUID.randomUUID() + ".pdf";
                }
            };
        } catch (MalformedURLException e) {
            throw new ApiException("图片url异常");
        }
        params.add("file", resource);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(params, getUploadFileHttpHeads());
        ParameterizedTypeReference<BaseAppleEduResultVO<UploadPicturesResultVO>> typeRef = new ParameterizedTypeReference<BaseAppleEduResultVO<UploadPicturesResultVO>>() {};
        ResponseEntity<BaseAppleEduResultVO<UploadPicturesResultVO>> responseEntity = okRestTemplate.exchange(getAppleEduUploadReqUrl(), HttpMethod.POST, httpEntity, typeRef);
        // 远程调用结果处理
        BaseAppleEduReqBO<String> baseReqBO = new BaseAppleEduReqBO<>();
        baseReqBO.setPayload(url);
        resultHandler(baseReqBO, responseEntity, "上传图片");
        return responseEntity.getBody().getPayload();
    }



    private <T, V> void resultHandler(BaseAppleEduReqBO<T> baseReqBO, ResponseEntity<BaseAppleEduResultVO<V>> responseEntity, String cmd) {
        BaseAppleEduResultVO<V> body = responseEntity.getBody();
        if (body == null) {
            log.error("【apple_edu】cmd接口:[{}]调用结果===》payload请求参数:[{}]调用失败: {}", cmd, baseReqBO.getPayload(), responseEntity);
            throw new ApiException("edu接口远程调用失败");
        }
        if (!EDU_SUCCESS_CODE.equals(body.getCode())) {
            log.error("【apple_edu】cmd接口:[{}]调用结果===》payload请求参数:[{}]调用失败：{}", cmd, baseReqBO.getPayload(), body);
            if (SYSTEM_ERROR_CODES.contains(body.getCode())) {
                throw new ApiException("活动无效");
            }
            throw new ApiException(body.getError());
        }
        if (body.getPayload() == null) {
            log.error("【apple_edu】cmd接口:[{}]调用结果===》payload请求参数:[{}]返回异常", cmd, baseReqBO.getPayload());
            throw new ApiException(body.getError());
        }
        log.info("【apple_edu】cmd接口:[{}]调用结果===》payload请求参数:[{}]调用成功：{}", cmd, baseReqBO.getPayload(), body);
    }

}
