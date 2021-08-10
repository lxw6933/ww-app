package com.ww.mall.mvc.controller.admin;

import com.github.pagehelper.PageInfo;
import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.common.R;
import com.ww.mall.common.valid.group.BatchDeleteGroup;
import com.ww.mall.common.valid.group.DeleteGroup;
import com.ww.mall.common.valid.group.UpdateGroup;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.service.SysUserService;
import com.ww.mall.mvc.view.form.IdForm;
import com.ww.mall.mvc.view.form.admin.ModifyPasswordForm;
import com.ww.mall.mvc.view.form.admin.SysUserForm;
import com.ww.mall.mvc.view.query.admin.SysUserQuery;
import com.ww.mall.mvc.view.vo.admin.SysUserVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


/**
 * 后台用户表 - controller
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@RestController
@RequestMapping("admin/sys/user")
public class SysUserController extends AbstractController {

    @Resource
    private SysUserService sysUserService;

    /**
     * 分页
     */
    @GetMapping("/page")
    public R page(Pagination pagination, SysUserQuery query) {
        PageInfo<SysUserVO> pageInfo = sysUserService.page(pagination, query.getQueryWrapper());
        return R.ok(pageInfo);
    }

    /**
     * 详情
     */
    @GetMapping("/info")
    public R info(@RequestParam Long id) {
        return R.oks(sysUserService.info(id));
    }

    /**
     * 新增
     */
    @PostMapping("/save")
    @SysLog(value = "新增用户")
    public R save(@RequestBody @Validated SysUserForm form) {
        return R.oks(sysUserService.save(form));
    }

    /**
     * 编辑
     */
    @PostMapping("/update")
    @SysLog(value = "编辑用户")
    public R update(@RequestBody @Validated({UpdateGroup.class}) SysUserForm form) {
        return R.oks(sysUserService.update(form));
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @SysLog(value = "删除用户")
    public R delete(@RequestBody @Validated({DeleteGroup.class}) IdForm form) {
        sysUserService.deleteById(form.getId());
        return R.ok();
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    @SysLog(value = "批量删除用户")
    public R batchDelete(@RequestBody @Validated({BatchDeleteGroup.class}) IdForm form) {
        return R.oks(sysUserService.batchDelete(form.getIds()));
    }

    @PostMapping("/modifyPassword")
    @SysLog(value = "修改用户密码")
    public R modifyPassword(@RequestBody @Validated ModifyPasswordForm form) {
        sysUserService.modifyPassword(form);
        return R.ok();
    }

}
