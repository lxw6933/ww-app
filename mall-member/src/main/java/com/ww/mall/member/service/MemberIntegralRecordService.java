package com.ww.mall.member.service;

import com.ww.mall.member.entity.mongo.MemberIntegralRecord;
import com.ww.mall.web.cmmon.MallPage;
import com.ww.mall.web.cmmon.MallPageResult;
import com.ww.mall.web.view.bo.AddMemberIntegralBO;

/**
 * @author ww
 * @create 2023-07-21- 15:54
 * @description:
 */
public interface MemberIntegralRecordService {

    /**
     * 保存积分记录
     * @param memberIntegralRecord record
     * @return boolean
     */
    Boolean save(MemberIntegralRecord memberIntegralRecord);

    /**
     * 获取用户积分记录
     * @param mallPage page
     * @return MallPageResult
     */
    MallPageResult<MemberIntegralRecord> page(MallPage mallPage);

    /**
     * 查看用户积分使用记录
     * @param integralRecordId id
     * @return MemberIntegralRecord
     */
    MemberIntegralRecord memberIntegralRecordDetail(String integralRecordId);

    /**
     * 新用户赠送积分
     *
     * @param addMemberIntegralBO bo
     * @return boolean
     */
    Boolean addNewMemberIntegral(AddMemberIntegralBO addMemberIntegralBO);

}
