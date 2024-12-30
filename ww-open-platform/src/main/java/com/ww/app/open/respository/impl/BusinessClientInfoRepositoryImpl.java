package com.ww.app.open.respository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.open.entity.BusinessClientInfo;
import com.ww.app.open.infrastructure.BusinessClientInfoMapper;
import com.ww.app.open.respository.BusinessClientInfoRepository;
import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;

/**
 * @author ww
 * @create 2024-05-25 15:30
 * @description:
 */
public class BusinessClientInfoRepositoryImpl extends ServiceImpl<BusinessClientInfoMapper, BusinessClientInfo> implements BusinessClientInfoRepository {

    @Override
    public int saveBusinessClient(BusinessClientInfo businessClientInfo) {
        return 0;
    }

    @Override
    public AppPageResult<BusinessClientInfo> page(AppPage appPage) {
        return null;
    }
}
