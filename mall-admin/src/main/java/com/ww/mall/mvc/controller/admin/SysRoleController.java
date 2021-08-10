package com.ww.mall.mvc.controller.admin;

import com.github.pagehelper.PageInfo;
import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.common.R;
import com.ww.mall.common.valid.group.BatchDeleteGroup;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.service.SysRoleService;
import com.ww.mall.mvc.view.form.IdForm;
import com.ww.mall.mvc.view.form.admin.SysRoleForm;
import com.ww.mall.mvc.view.query.admin.SysRoleQuery;
import com.ww.mall.mvc.view.vo.admin.SysRoleVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


/**
 * 后台角色表 - controller
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@RestController
@RequestMapping("admin/sys/role")
public class SysRoleController extends AbstractController {

    @Resource
    private SysRoleService sysRoleService;

    /**
     * 分页
     */
    @GetMapping("/page")
    public R page(Pagination pagination, SysRoleQuery query) {
        PageInfo<SysRoleVO> pageInfo = sysRoleService.page(pagination, query.getQueryWrapper());
        return R.ok(pageInfo);
    }

    /**
     * 详情
     */
    @GetMapping("/info")
    public R info(@RequestParam Long id) {
        return R.oks(sysRoleService.info(id));
    }

    /**
     * 新增
     */
    @PostMapping("/save")
    @SysLog(value = "新增角色")
    public R save(@RequestBody @Validated SysRoleForm form) {
        return R.oks(sysRoleService.save(form));
    }

    /**
     * 编辑
     */
    @PostMapping("/update")
    @SysLog(value = "编辑角色")
    public R update(@RequestBody @Validated({UpdateGroup.class}) SysRoleForm form) {
        return R.oks(sysRoleService.update(form));
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @SysLog(value = "删除角色")
    public R delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        sysRoleService.deleteById(form.getId());
        return R.ok();
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    @SysLog(value = "批量删除角色")
    public R batchDelete(@RequestBody @Validated({BatchDeleteGroup.class}) IdForm form) {
        return R.oks(sysRoleService.batchDelete(form.getIds()));
    }

}
