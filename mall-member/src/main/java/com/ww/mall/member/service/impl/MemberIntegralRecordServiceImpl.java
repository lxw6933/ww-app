package com.ww.mall.member.service.impl;

import com.ww.mall.common.common.MallClientUser;
import com.ww.mall.member.entity.mongo.MemberIntegralRecord;
import com.ww.mall.member.service.MemberIntegralRecordService;
import com.ww.mall.web.cmmon.MallPage;
import com.ww.mall.web.cmmon.MallPageResult;
import com.ww.mall.web.utils.AuthorizationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

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

    @Override
    public void save(MemberIntegralRecord memberIntegralRecord) {
        mongoTemplate.save(memberIntegralRecord);
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
}
