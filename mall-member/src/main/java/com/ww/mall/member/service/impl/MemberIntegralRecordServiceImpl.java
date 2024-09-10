package com.ww.mall.member.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ww.mall.common.common.MallClientUser;
import com.ww.mall.member.entity.Member;
import com.ww.mall.member.entity.mongo.MemberIntegralRecord;
import com.ww.mall.member.enums.IntegralSource;
import com.ww.mall.member.enums.IntegralType;
import com.ww.mall.member.service.MemberIntegralRecordService;
import com.ww.mall.member.service.MemberService;
import com.ww.mall.web.cmmon.MallPage;
import com.ww.mall.web.cmmon.MallPageResult;
import com.ww.mall.web.utils.AuthorizationContext;
import com.ww.mall.web.view.bo.AddMemberIntegralBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-21- 15:54
 * @description:
 */
@Slf4j
@Service
public class MemberIntegralRecordServiceImpl implements MemberIntegralRecordService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MemberService memberService;

    @Override
    public Boolean save(MemberIntegralRecord memberIntegralRecord) {
        mongoTemplate.save(memberIntegralRecord);
        return true;
    }

    @Override
    public MallPageResult<MemberIntegralRecord> page(MallPage mallPage) {
        MallClientUser clientUser = AuthorizationContext.getClientUser();
        // 创建分页信息对象
        Pageable pageable = PageRequest.of(mallPage.getPageNum() - 1, mallPage.getPageSize());
        // 创建排序条件对象，根据字段进行倒序排序
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        // 创建查询条件
        Query query = new Query();
        query.addCriteria(Criteria.where("memberId").is(clientUser.getMemberId()));
        // 获取总记录数
        long totalCount = mongoTemplate.count(query, MemberIntegralRecord.class);
        // 在查询条件中添加分页信息，并执行查询
        query.with(pageable).with(sort);
        List<MemberIntegralRecord> memberIntegralRecordList = mongoTemplate.find(query, MemberIntegralRecord.class);
        return new MallPageResult<>(mallPage, memberIntegralRecordList, (int) totalCount);
    }

    @Override
    public MemberIntegralRecord memberIntegralRecordDetail(String integralRecordId) {
        MallClientUser clientUser = AuthorizationContext.getClientUser();
        Query query = new Query();
        Criteria criteria = Criteria.where("id").is(integralRecordId)
                .and("memberId").is(clientUser.getMemberId());
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, MemberIntegralRecord.class);
    }

    @Override
//    @Transactional(rollbackFor = Exception.class)
    public Boolean addNewMemberIntegral(AddMemberIntegralBO addMemberIntegralBO) {
        // 先查询新用户是否已经新增过积分
        Query query = new Query();
        Criteria criteria = Criteria.where("memberId").is(addMemberIntegralBO.getMemberId())
                .and("integralSource").is(IntegralSource.REGISTER);
        query.addCriteria(criteria);
        MemberIntegralRecord newMemberIntegralRecord = mongoTemplate.findOne(query, MemberIntegralRecord.class);
        if (newMemberIntegralRecord != null) {
            return true;
        }
        // 更新用户积分数量
        memberService.update(new UpdateWrapper<Member>()
                .eq("id", addMemberIntegralBO.getMemberId())
                .set("available_integral", addMemberIntegralBO.getIntegralNum())
        );
        // 添加积分记录
        MemberIntegralRecord memberIntegralRecord = new MemberIntegralRecord();
        memberIntegralRecord.setMemberId(addMemberIntegralBO.getMemberId());
        memberIntegralRecord.setIntegralNum(addMemberIntegralBO.getIntegralNum());
        memberIntegralRecord.setIntegralSource(IntegralSource.REGISTER);
        memberIntegralRecord.setIntegralType(Boolean.TRUE.equals(addMemberIntegralBO.getIntegralType()) ?
                IntegralType.INCREASE : IntegralType.DECREASE);
        memberIntegralRecord.setRead(false);
        memberIntegralRecord.setCreateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        mongoTemplate.save(memberIntegralRecord);
        return true;
    }
}
