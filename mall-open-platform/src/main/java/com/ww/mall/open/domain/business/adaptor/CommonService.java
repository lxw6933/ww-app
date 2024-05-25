package com.ww.mall.open.domain.business.adaptor;

import com.alibaba.fastjson.JSONObject;

/**
 * @author ww
 * @create 2024-05-25 13:42
 * @description: 通用业务接口，所有开放平台的业务都实现这个接口
 */
public interface CommonService {

    /**
     * 处理业务接口
     *
     * @param baseOpenRequest 业务请求参数
     * @return JSONObject
     */
    JSONObject doBusiness(BaseOpenRequest baseOpenRequest);

    /**
     * 业务类型code
     */
    String businessCode();

}
