package com.ww.app.member.convert.member;

import com.ww.app.member.entity.Member;
import com.ww.app.member.view.vo.MemberVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author ww
 * @create 2025-09-26 14:20
 * @description:
 */
@Mapper
public interface MemberConvert {

    MemberConvert INSTANCE = Mappers.getMapper(MemberConvert.class);

    MemberVO convert(Member member);

}
