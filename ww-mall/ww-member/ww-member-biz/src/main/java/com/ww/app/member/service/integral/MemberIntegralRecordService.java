package com.ww.app.member.service.integral;

import com.ww.app.member.entity.mongo.MemberIntegralRecord;
import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.member.member.bo.AddMemberIntegralBO;

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
     * @param appPage page
     * @return MallPageResult
     */
    AppPageResult<MemberIntegralRecord> page(AppPage appPage);

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
