package com.ww.mall.member.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author ww
 * @create 2023-07-17- 11:06
 * @description:
 */
@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
