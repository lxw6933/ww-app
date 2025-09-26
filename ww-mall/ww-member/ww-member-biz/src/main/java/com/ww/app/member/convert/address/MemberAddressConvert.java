package com.ww.app.member.convert.address;

import com.ww.app.ip.utils.AreaUtils;
import com.ww.app.member.controller.app.address.req.MemberAddressBO;
import com.ww.app.member.controller.app.address.res.MemberAddressVO;
import com.ww.app.member.entity.MemberAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-26 14:20
 * @description:
 */
@Mapper
public interface MemberAddressConvert {

    MemberAddressConvert INSTANCE = Mappers.getMapper(MemberAddressConvert.class);

    MemberAddress convert(MemberAddressBO memberAddressBO);

    @Mapping(source = "areaId", target = "areaName", qualifiedByName = "convertAreaIdToAreaName")
    MemberAddressVO convert(MemberAddress memberAddress);

    List<MemberAddressVO> convertList(List<MemberAddress> memberAddressList);

    @Named("convertAreaIdToAreaName")
    default String convertAreaIdToAreaName(Integer areaId) {
        return AreaUtils.format(areaId);
    }

}
