package com.ww.mall.coupon.excel.listener;

import com.ww.mall.coupon.excel.model.ConvertCodeExcelModel;
import com.ww.mall.web.excel.ExcelImportResultVO;
import com.ww.mall.web.excel.MallAbstractImportListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author ww
 * @create 2024-03-12- 13:39
 * @description:
 */
@Slf4j
public class CouponExcelImportListener extends MallAbstractImportListener<ConvertCodeExcelModel> {

    private final MongoTemplate mongoTemplate;

    public CouponExcelImportListener(ExcelImportResultVO excelImportResultVO, MongoTemplate mongoTemplate) {
        super.excelImportResultVO = excelImportResultVO;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    protected void handleData() {
        mongoTemplate.insertAll(dataList);
    }

}
