package com.ww.mall.promotion.component;

import com.ww.app.common.queue.AbstractRecordQueue;
import com.ww.app.mongodb.handler.MongoBulkDataHandler;
import com.ww.mall.promotion.entity.group.GroupFlowLog;

import java.util.List;

/**
 * 拼团链路日志批量落库队列。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 通过批量插入降低高并发拼团轨迹写入的 MongoDB 压力
 */
public class GroupFlowLogQueueComponent extends AbstractRecordQueue<GroupFlowLog> {

    private final MongoBulkDataHandler<GroupFlowLog> mongoBulkDataHandler;

    public GroupFlowLogQueueComponent(MongoBulkDataHandler<GroupFlowLog> mongoBulkDataHandler) {
        this.mongoBulkDataHandler = mongoBulkDataHandler;
    }

    @Override
    public int recordDBHandler(List<GroupFlowLog> batchCodeRecordList) {
        return mongoBulkDataHandler.bulkSave(batchCodeRecordList);
    }
}
