package com.ww.mall.admin.controller;

import com.ww.mall.admin.view.query.SysOperateLogPageQuery;
import com.ww.mall.admin.view.vo.OperateLogVO;
import com.ww.mall.common.common.MallPageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ww
 * @create 2024-09-19- 11:46
 * @description:
 */
@RestController
@RequestMapping("/operateLog")
public class OperateLogController extends MallAbstractController {

    @GetMapping("/logs/page")
    public MallPageResult<OperateLogVO> page(SysOperateLogPageQuery query) {
        return sf.getOperateLogService().page(query);
    }

}
