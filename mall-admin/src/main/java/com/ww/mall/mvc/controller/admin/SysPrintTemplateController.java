package com.ww.mall.mvc.controller.admin;

import com.github.pagehelper.PageInfo;
import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.valid.group.BatchDeleteGroup;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.service.SysPrintTemplateService;
import com.ww.mall.mvc.view.form.IdForm;
import com.ww.mall.mvc.view.form.admin.SysPrintTemplateForm;
import com.ww.mall.mvc.view.query.admin.SysPrintTemplateQuery;
import com.ww.mall.mvc.view.vo.admin.SysPrintTemplateVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 打印模板controller
 * @author: ww
 * @create: 2021-05-17 13:35
 */
@RestController
@RequestMapping("/admin/sys/print/template")
public class SysPrintTemplateController extends AbstractController {

    @Resource
    private SysPrintTemplateService sysPrintTemplateService;

    /**
     * 分页
     */
    @GetMapping("/page")
    public R page(Pagination pagination, SysPrintTemplateQuery query) {
        PageInfo<SysPrintTemplateVO> pageInfo = sysPrintTemplateService.page(pagination, query.getQueryWrapper());
        return R.ok(pageInfo);
    }

    /**
     * 详情
     */
    @GetMapping("/info")
    public R info(@RequestParam Long id) {
        return R.oks(sysPrintTemplateService.info(id));
    }

    /**
     * 新增
     */
    @PostMapping("/save")
    @SysLog(value = "新增PDF打印模板")
    public R save(@RequestBody @Validated SysPrintTemplateForm form) {
        return R.oks(sysPrintTemplateService.save(form));
    }

    /**
     * 编辑
     */
    @PostMapping("/update")
    @SysLog(value = "编辑PDF打印模板")
    public R update(@RequestBody @Validated({UpdateGroup.class}) SysPrintTemplateForm form) {
        return R.oks(sysPrintTemplateService.update(form));
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @SysLog(value = "删除PDF打印模板")
    public R delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        sysPrintTemplateService.deleteById(form.getId());
        return R.ok();
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    @SysLog(value = "批量删除PDF打印模板")
    public R batchDelete(@RequestBody @Validated({BatchDeleteGroup.class}) IdForm form) {
        return R.oks(sysPrintTemplateService.batchDelete(form.getIds()));
    }

}

