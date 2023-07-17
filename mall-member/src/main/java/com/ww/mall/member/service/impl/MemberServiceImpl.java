package com.ww.mall.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.member.dao.MemberMapper;
import com.ww.mall.member.entity.Member;
import com.ww.mall.member.service.MemberService;
import org.springframework.stereotype.Service;

/**
 * @author ww
 * @create 2023-07-17- 11:11
 * @description:
 */
@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

}
