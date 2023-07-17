package com.ww.mall.member.controller;

import com.ww.mall.member.entity.Member;
import com.ww.mall.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;


/**
 * @author ww
 * @create 2023-07-17- 11:09
 * @description:
 */
@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping("/list")
    public List<Member> memberList() {
        return memberService.list();
    }


}

