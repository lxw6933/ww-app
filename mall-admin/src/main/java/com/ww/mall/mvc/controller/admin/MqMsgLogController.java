package com.ww.mall.mvc.controller.admin;

import com.github.pagehelper.PageInfo;
import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.valid.group.BatchDeleteGroup;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.service.MqMsgLogService;
import com.ww.mall.mvc.view.form.IdForm;
import com.ww.mall.mvc.view.form.admin.MqMsgLogForm;
import com.ww.mall.mvc.view.query.admin.MqMsgLogQuery;
import com.ww.mall.mvc.view.vo.admin.MqMsgLogVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: MQ消费日志controller
 * @author: ww
 * @create: 2021-06-30 09:35
 */
@RestController
@RequestMapping("/admin/sys/mq/log")
public class MqMsgLogController extends AbstractController {

    @Resource
    private MqMsgLogService mqMsgLogService;

    /**
     * 分页
     */
    @GetMapping("/page")
    public R page(Pagination pagination, MqMsgLogQuery query) {
        PageInfo<MqMsgLogVO> pageInfo = mqMsgLogService.page(pagination, query.getQueryWrapper());
        return R.ok(pageInfo);
    }

    /**
     * 详情
     */
    @GetMapping("/info")
    public R info(@RequestParam Long id) {
        return R.oks(mqMsgLogService.info(id));
    }

    /**
     * 新增
     */
    @PostMapping("/save")
    @SysLog(value = "新增MQ消费日志")
    public R save(@RequestBody @Validated MqMsgLogForm form) {
        return R.oks(mqMsgLogService.save(form));
    }

    /**
     * 编辑
     */
    @PostMapping("/update")
    @SysLog(value = "编辑MQ消费日志")
    public R update(@RequestBody @Validated({UpdateGroup.class}) MqMsgLogForm form) {
        return R.oks(mqMsgLogService.update(form));
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @SysLog(value = "删除MQ消费日志")
    public R delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        mqMsgLogService.deleteById(form.getId());
        return R.ok();
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    @SysLog(value = "批量删除MQ消费日志")
    public R batchDelete(@RequestBody @Validated({BatchDeleteGroup.class}) IdForm form) {
        return R.oks(mqMsgLogService.batchDelete(form.getIds()));
    }

}

