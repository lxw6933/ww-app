package com.ww.app.member.service.address;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.CollectionUtils;
import com.ww.app.member.controller.app.address.req.MemberAddressBO;
import com.ww.app.member.controller.app.address.res.MemberAddressVO;
import com.ww.app.member.convert.address.MemberAddressConvert;
import com.ww.app.member.dao.address.MemberAddressMapper;
import com.ww.app.member.entity.MemberAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ww.app.member.member.enums.ErrorCodeConstants.ADDRESS_NOT_EXISTS;

/**
 * @author ww
 * @create 2025-09-26 14:01
 * @description:
 */
@Slf4j
@Service
public class MemberAddressServiceImpl extends ServiceImpl<MemberAddressMapper, MemberAddress> implements MemberAddressService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(MemberAddressBO memberAddressBO) {
        ClientUser clientUser = AuthorizationContext.getClientUser();

        if (Boolean.TRUE.equals(memberAddressBO.getDefaultStatus())) {
            List<MemberAddress> addresses = this.list(new LambdaQueryWrapper<MemberAddress>()
                    .eq(MemberAddress::getUserId, clientUser.getId())
                    .eq(MemberAddress::getDefaultStatus, true)
            );
            addresses.forEach(address -> {
                address.setDefaultStatus(false);
                this.updateById(address);
            });
        }

        MemberAddress address = MemberAddressConvert.INSTANCE.convert(memberAddressBO);
        address.setUserId(clientUser.getId());
        this.save(address);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(MemberAddressBO memberAddressBO) {
        ClientUser clientUser = AuthorizationContext.getClientUser();

        // 校验存在,校验是否能够操作
        validAddressExists(memberAddressBO.getId());

        // 如果修改的是默认收件地址，则将原默认地址修改为非默认
        if (Boolean.TRUE.equals(memberAddressBO.getDefaultStatus())) {
            List<MemberAddress> addresses = this.list(new LambdaQueryWrapper<MemberAddress>()
                    .eq(MemberAddress::getUserId, clientUser.getId())
                    .eq(MemberAddress::getDefaultStatus, true)
            );
            addresses.stream().filter(address -> !address.getId().equals(memberAddressBO.getId()))
                    .forEach(address -> {
                        address.setDefaultStatus(false);
                        this.updateById(address);
                    });
        }

        MemberAddress updateObj = MemberAddressConvert.INSTANCE.convert(memberAddressBO);
        this.updateById(updateObj);
        return true;
    }

    @Override
    public boolean delete(Long id) {
        validAddressExists(id);
        this.removeById(id);
        return true;
    }

    @Override
    public MemberAddressVO get(Long id) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        MemberAddress memberAddress = this.getOne(new LambdaQueryWrapper<MemberAddress>()
                .eq(MemberAddress::getUserId, clientUser.getId())
                .eq(MemberAddress::getId, id)
        );
        return MemberAddressConvert.INSTANCE.convert(memberAddress);
    }

    @Override
    public List<MemberAddressVO> getMemberAddressList() {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        List<MemberAddress> memberAddressList = this.list(new LambdaQueryWrapper<MemberAddress>().eq(MemberAddress::getUserId, clientUser.getId()));
        return MemberAddressConvert.INSTANCE.convertList(memberAddressList);
    }

    @Override
    public MemberAddressVO getDefaultMemberAddress() {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        List<MemberAddress> addresses = this.list(new LambdaQueryWrapper<MemberAddress>()
                .eq(MemberAddress::getUserId, clientUser.getId())
                .eq(MemberAddress::getDefaultStatus, true)
        );
        MemberAddress memberAddress = CollectionUtils.getFirst(addresses);
        return MemberAddressConvert.INSTANCE.convert(memberAddress);
    }

    private void validAddressExists(Long id) {
        MemberAddressVO memberAddressVO = get(id);
        if (memberAddressVO == null) {
            throw new ApiException(ADDRESS_NOT_EXISTS);
        }
    }

}
