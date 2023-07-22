package com.ww.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.member.config.MemberPublisher;
import com.ww.mall.member.dao.MemberMapper;
import com.ww.mall.member.entity.Member;
import com.ww.mall.member.service.MemberService;
import com.ww.mall.web.view.dto.MemberDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author ww
 * @create 2023-07-17- 11:11
 * @description:
 */
@Slf4j
@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    @Autowired
    private MemberPublisher memberPublisher;

    @Override
    public MemberDTO getMemberByMobile(String mobile) {
        // 查询当前手机号用户
        Member member = this.getOne(new QueryWrapper<Member>().eq("mobile", mobile));
        if (member == null) {
            // 自动注册用户
            member = new Member();
            member.setMobile(mobile);
            member.setNickName(UUID.randomUUID().toString());
            this.save(member);
            // 发送用户注册消息
            memberPublisher.publishMemberRegisterMsg(member.getId());
        }
        MemberDTO memberDTO = new MemberDTO();
        BeanUtils.copyProperties(member, memberDTO);
        return memberDTO;
    }
}
