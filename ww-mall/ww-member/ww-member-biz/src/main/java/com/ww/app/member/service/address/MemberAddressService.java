package com.ww.app.member.service.address;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.app.member.controller.app.address.req.MemberAddressBO;
import com.ww.app.member.controller.app.address.res.MemberAddressVO;
import com.ww.app.member.entity.MemberAddress;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-26 14:00
 * @description:
 */
public interface MemberAddressService extends IService<MemberAddress> {

    /**
     * 创建用户收件地址
     *
     * @param memberAddressBO 创建信息
     * @return 编号
     */
    boolean add(MemberAddressBO memberAddressBO);

    /**
     * 更新用户收件地址
     *
     * @param memberAddressBO 更新信息
     */
    boolean update(MemberAddressBO memberAddressBO);

    /**
     * 删除用户收件地址
     *
     * @param id     编号
     */
    boolean delete(Long id);

    /**
     * 获得用户收件地址
     *
     * @param id 编号
     * @return 用户收件地址
     */
    MemberAddressVO get(Long id);

    /**
     * 获得用户收件地址列表
     *
     * @return 用户收件地址列表
     */
    List<MemberAddressVO> getMemberAddressList();

    /**
     * 获得用户默认的收件地址
     *
     * @return 用户收件地址
     */
    MemberAddressVO getDefaultMemberAddress();

}
