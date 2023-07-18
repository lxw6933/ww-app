package com.ww.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.member.entity.Member;
import com.ww.mall.web.view.dto.MemberDTO;

/**
 * @author ww
 * @create 2023-07-17- 11:09
 * @description:
 */
public interface MemberService extends IService<Member> {
    MemberDTO getMemberByMobile(String mobile);
}
