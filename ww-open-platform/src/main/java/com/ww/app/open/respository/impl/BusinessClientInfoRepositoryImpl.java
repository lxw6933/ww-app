package com.ww.app.open.respository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.utils.RSAUtil;
import com.ww.app.open.entity.BusinessClientInfo;
import com.ww.app.open.infrastructure.BusinessClientInfoMapper;
import com.ww.app.open.respository.BusinessClientInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;

/**
 * 商户信息仓储实现类
 * 
 * @author ww
 * @create 2024-05-25 15:30
 * @description: 负责商户信息的CRUD操作，包括RSA密钥对的自动生成
 */
@Slf4j
@Service
public class BusinessClientInfoRepositoryImpl extends ServiceImpl<BusinessClientInfoMapper, BusinessClientInfo> implements BusinessClientInfoRepository {

    /**
     * 保存商户信息
     * 如果商户未提供RSA密钥对，则自动生成
     * 
     * @param businessClientInfo 商户信息
     * @return 保存结果（1-成功，0-失败）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveBusinessClient(BusinessClientInfo businessClientInfo) {
        // 如果商户未提供密钥对，自动生成RSA密钥对
        if (!StringUtils.hasText(businessClientInfo.getPublicKey()) || !StringUtils.hasText(businessClientInfo.getPrivateKey())) {
            log.info("商户 {} 未提供RSA密钥对，自动生成中...", businessClientInfo.getSysCode());
            HashMap<String, String> keyPair = RSAUtil.generatePublicPrivateKeys();
            businessClientInfo.setPublicKey(keyPair.get(Constant.RSA_PUBLIC_KEY));
            businessClientInfo.setPrivateKey(keyPair.get(Constant.RSA_PRIVATE_KEY));
            log.info("商户 {} RSA密钥对生成完成，公钥长度: {}, 私钥长度: {}", 
                    businessClientInfo.getSysCode(),
                    businessClientInfo.getPublicKey().length(),
                    businessClientInfo.getPrivateKey().length());
        }
        
        // 设置默认状态为启用
        if (businessClientInfo.getStatus() == null) {
            businessClientInfo.setStatus(true);
        }
        
        boolean success = this.save(businessClientInfo);
        if (success) {
            log.info("商户信息保存成功: sysCode={}, businessName={}", 
                    businessClientInfo.getSysCode(), businessClientInfo.getBusinessName());
            return 1;
        } else {
            log.error("商户信息保存失败: sysCode={}", businessClientInfo.getSysCode());
            return 0;
        }
    }

    @Override
    public AppPageResult<BusinessClientInfo> page(AppPage appPage) {
        return null;
    }

    @Override
    public BusinessClientInfo getBySysCode(String sysCode) {
        LambdaQueryWrapper<BusinessClientInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BusinessClientInfo::getSysCode, sysCode);
        return this.getOne(wrapper);
    }
}
