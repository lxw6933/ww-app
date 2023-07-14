package com.ww.mall.mvc.controller.admin;

import com.github.pagehelper.PageInfo;
import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.valid.group.AddGroup;
import com.ww.mall.common.valid.group.BatchDeleteGroup;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.service.SysConfigService;
import com.ww.mall.mvc.view.form.IdForm;
import com.ww.mall.mvc.view.form.admin.SysConfigForm;
import com.ww.mall.mvc.view.query.admin.SysConfigQuery;
import com.ww.mall.mvc.view.vo.admin.SysConfigVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 系统配置 controller
 * @author: ww
 * @create: 2021-05-19 10:36
 */
@RestController
@RequestMapping("/admin/sys/config")
public class SysConfigController extends AbstractController {

    @Resource
    private SysConfigService sysConfigService;

    /**
     * 分页
     */
    @GetMapping("/page")
    public R page(Pagination pagination, SysConfigQuery query) {
        PageInfo<SysConfigVO> pageInfo = sysConfigService.page(pagination, query.getQueryWrapper());
        return R.ok(pageInfo);
    }

    /**
     * 根据配置key获取配置value
     */
    @GetMapping("/getValueByKey")
    public R getValueByKey(@RequestParam String configKey){
        String value = sysConfigService.getConfigValue(configKey);
        return R.oks(value);
    }

    /**
     * 新增
     */
    @PostMapping("/save")
    @SysLog(value = "新增系统配置信息")
    public R save(@RequestBody @Validated({AddGroup.class}) SysConfigForm form) {
        sysConfigService.save(form);
        return R.ok();
    }

    /**
     * 编辑
     */
    @PostMapping("/update")
    @SysLog(value = "修改系统配置信息")
    public R update(@RequestBody @Validated({UpdateGroup.class}) SysConfigForm form) {
        sysConfigService.update(form);
        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @SysLog(value = "删除系统配置信息")
    public R delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        sysConfigService.removeById(form.getId());
        return R.ok();
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    @SysLog(value = "批量删除系统配置信息")
    public R batchDelete(@RequestBody @Validated({BatchDeleteGroup.class}) IdForm form) {
        return R.oks(sysConfigService.batchDelete(form.getIds()));
    }


}
