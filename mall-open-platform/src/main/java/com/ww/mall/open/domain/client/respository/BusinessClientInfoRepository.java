package com.ww.mall.open.domain.client.respository;

import com.ww.mall.open.domain.client.entity.BusinessClientInfo;
import com.ww.mall.web.cmmon.MallPage;
import com.ww.mall.web.cmmon.MallPageResult;

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
     * @param mallPage
     * @return MallPageResult
     */
    MallPageResult<BusinessClientInfo> page(MallPage mallPage);

}
