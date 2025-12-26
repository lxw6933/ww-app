package com.ww.app.open.service;

import com.ww.app.open.entity.BusinessClientInfo;
import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;

/**
 * @author ww
 * @create 2024-05-25 15:27
 * @description:
 */
public interface BusinessClientInfoService {

    /**
     * 开发平台商户入驻
     *
     * @param businessClientInfo 商户信息
     * @return int
     */
    int saveBusinessClient(BusinessClientInfo businessClientInfo);

    /**
     * 开放平台商户列表
     *
     * @param appPage pageQuery
     * @return MallPageResult
     */
    AppPageResult<BusinessClientInfo> page(AppPage appPage);

    /**
     * 根据商户编码查询商户信息
     *
     * @param sysCode 商户编码
     * @return 商户信息
     */
    BusinessClientInfo getBySysCode(String sysCode);

}
