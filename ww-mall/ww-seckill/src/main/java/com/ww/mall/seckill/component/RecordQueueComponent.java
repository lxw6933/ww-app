package com.ww.mall.seckill.component;

import com.ww.mall.common.queue.AbstractRecordQueue;
import com.ww.mall.mongodb.handler.MongoBulkDataHandler;
import com.ww.mall.seckill.entity.IssueCodeRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author ww
 * @create 2024-08-31 11:55
 * @description:
 */
@Slf4j
public class RecordQueueComponent extends AbstractRecordQueue<IssueCodeRecord> {

    private final MongoBulkDataHandler<IssueCodeRecord> mongoBulkDataHandler;

    public RecordQueueComponent(MongoBulkDataHandler<IssueCodeRecord> mongoBulkDataHandler) {
        super();
        this.mongoBulkDataHandler = mongoBulkDataHandler;
    }

    @Override
    public int recordDBHandler(List<IssueCodeRecord> batchCodeRecordList) {
        return mongoBulkDataHandler.bulkSave(batchCodeRecordList);
    }

}
