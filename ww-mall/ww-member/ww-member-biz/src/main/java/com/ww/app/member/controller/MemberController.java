package com.ww.app.member.controller;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.member.entity.Member;
import com.ww.app.member.service.MemberService;
import com.ww.app.member.view.vo.MemberVO;
import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.utils.AuthorizationContext;
import com.ww.app.common.utils.IdUtil;
import com.ww.app.mybatis.common.AppPlusPageResult;
import com.ww.app.member.member.dto.MemberDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
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

    @RequestMapping("/getId")
    public String getId() {
        String id = IdUtil.generatorIdStr();
        System.out.println(id);
        return id;
    }

    @GetMapping("/getMemberByMobile")
    public MemberDTO getMemberByMobile(@RequestParam("mobile") String mobile) {
        return memberService.getMemberByMobile(mobile);
    }

    @GetMapping("/list")
    public AppPageResult<MemberVO> pageList(AppPage appPage) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        if (clientUser == null) {
            throw new ApiException(GlobalResCodeConstants.ILLEGAL_REQUEST);
        }
        IPage<Member> page = new Page<>(appPage.getPageNum(), appPage.getPageSize());
        memberService.page(page);
        return new AppPlusPageResult<>(page, result -> {
            MemberVO memberVO = new MemberVO();
            BeanUtils.copyProperties(result, memberVO);
            return memberVO;
        });
    }

    @GetMapping("/genericMemberRecord")
    public boolean genericMemberRecord() {
        List<Member> memberList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Member member = new Member();
            member.setOpenId(IdUtil.generatorIdStr());
            member.setChannelId(1L);
            member.setPassword(RandomUtil.randomNumbers(6));
            member.setNickName(RandomUtil.randomString(5));
            member.setMobile("19" + RandomUtil.randomNumbers(9));
            member.setBirthday(new Date());
            memberList.add(member);
        }
        memberService.saveBatch(memberList);
        return true;
    }


}

