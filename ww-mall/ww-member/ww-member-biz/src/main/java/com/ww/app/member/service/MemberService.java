package com.ww.app.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.app.member.entity.Member;
import com.ww.app.member.member.dto.MemberDTO;

/**
 * @author ww
 * @create 2023-07-17- 11:09
 * @description:
 */
public interface MemberService extends IService<Member> {
    /**
     * 获取用户
     *
     * @param mobile 手机号
     * @return MemberDTO
     */
    MemberDTO getMemberByMobile(String mobile);
}
