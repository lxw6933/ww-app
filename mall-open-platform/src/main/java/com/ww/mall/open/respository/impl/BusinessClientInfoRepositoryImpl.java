package com.ww.mall.open.respository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.open.entity.BusinessClientInfo;
import com.ww.mall.open.infrastructure.BusinessClientInfoMapper;
import com.ww.mall.open.respository.BusinessClientInfoRepository;
import com.ww.mall.common.common.MallPage;
import com.ww.mall.common.common.MallPageResult;

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
    public MallPageResult<BusinessClientInfo> page(MallPage mallPage) {
        return null;
    }
}
