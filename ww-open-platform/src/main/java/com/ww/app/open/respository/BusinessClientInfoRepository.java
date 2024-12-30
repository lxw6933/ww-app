package com.ww.app.open.respository;

import com.ww.app.open.entity.BusinessClientInfo;
import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;

/**
 * @author ww
 * @create 2024-05-25 15:27
 * @description:
 */
public interface BusinessClientInfoRepository {

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

}
