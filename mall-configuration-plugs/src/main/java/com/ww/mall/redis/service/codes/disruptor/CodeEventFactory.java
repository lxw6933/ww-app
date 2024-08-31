package com.ww.mall.redis.service.codes.disruptor;

import com.lmax.disruptor.EventFactory;
import com.ww.mall.redis.service.codes.IssueCodeRecord;

/**
 * @author ww
 * @create 2024-08-31 9:55
 * @description:
 */
public class CodeEventFactory implements EventFactory<IssueCodeRecord> {
    @Override
    public IssueCodeRecord newInstance() {
        return new IssueCodeRecord();
    }
}
