package com.ww.mall.common.interfaces;

import java.util.List;

/**
 * @author ww
 * @create 2024-10-14- 16:33
 * @description: 批量处理数据
 */
public interface BulkDataHandler<T> {

    /**
     * 批量保存数据
     *
     * @param dataList 数据集合
     * @return 成功保存数量
     */
    int bulkSave(List<T> dataList);

}
