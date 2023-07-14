package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.config.mybatisplus.page.MyPageHelper;
import com.ww.mall.config.mybatisplus.page.MyPageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.dao.SysRoleDao;
import com.ww.mall.mvc.entity.SysRoleEntity;
import com.ww.mall.mvc.service.SysRoleService;
import com.ww.mall.mvc.view.form.admin.RoleAndPermissionForm;
import com.ww.mall.mvc.view.form.admin.SysRoleForm;
import com.ww.mall.mvc.view.vo.admin.SysPermissionVO;
import com.ww.mall.mvc.view.vo.admin.SysRoleVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台角色表
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Slf4j
@Service("sysRoleService")
public class SysRoleServiceImpl extends ServiceImpl<SysRoleDao, SysRoleEntity> implements SysRoleService {

    @Resource
    private SysRoleService sysRoleService;

    @Override
    public PageInfo<SysRoleVO> page(Pagination pagination, QueryWrapper<SysRoleEntity> query) {
        MyPageHelper.startPage(pagination, SysRoleVO.class);
        List<SysRoleEntity> list = list(query);
        return new MyPageInfo<>(list).convert(entity -> {
            SysRoleVO vo = new SysRoleVO();
            BeanCopierUtils.copyProperties(entity, vo);
            return vo;
        });
    }

    @Override
    public SysRoleVO info(Long id) {
        SysRoleEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        SysRoleVO vo = new SysRoleVO();
        BeanCopierUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleVO save(SysRoleForm form) {
        SysRoleEntity entity = new SysRoleEntity();
        BeanCopierUtils.copyProperties(form, entity);
        super.save(entity);
        if (CollectionUtils.isNotEmpty(form.getPermissionIds())) {
            // 保存角色权限关联信息
            RoleAndPermissionForm data = new RoleAndPermissionForm();
            data.setRoleId(form.getId());
            data.setPermissionIds(form.getPermissionIds());
            sysRoleService.insertRoleAndPermission(data);
        }
        return info(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleVO update(SysRoleForm form) {
        SysRoleEntity entity = super.getById(form.getId());
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        BeanCopierUtils.copyProperties(form, entity);
        super.updateById(entity);
        // 更新角色权限关联信息
        // 判断权限是否变化
        List<Long> sources = sysRoleService.queryRoleOfPermission(entity.getId())
                .stream()
                .map(SysPermissionVO::getId).collect(Collectors.toList());
        if (!CollectionUtils.isEqualCollection(form.getPermissionIds(), sources)) {
            log.info("权限数据变化，更新关联数据");
            // 删除之前所有的关联信息，新增目前的关联信息
            sysRoleService.removeRoleAndPermission(entity.getId());
            if (CollectionUtils.isNotEmpty(form.getPermissionIds())) {
                RoleAndPermissionForm data = new RoleAndPermissionForm();
                data.setRoleId(entity.getId());
                data.setPermissionIds(form.getPermissionIds());
                sysRoleService.insertRoleAndPermission(data);
            }
        }
        return info(entity.getId());
    }

    @Override
    public void deleteById(Long id) {
        SysRoleEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        super.removeById(id);
    }

    @Override
    public BatchProcessingResult batchDelete(List<Long> ids) {
        BatchProcessingResult result = new BatchProcessingResult();
        for (Long id : ids) {
            try {
                deleteById(id);
                result.addSuccess(1);
            } catch (Exception ex) {
                result.addError(1).addErrorMsg(ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public void insertRoleAndPermission(RoleAndPermissionForm form) {
        getBaseMapper().insertRoleAndPermission(form);
    }

    @Override
    public void removeRoleAndPermission(Long roleId) {
        getBaseMapper().removeRoleAndPermission(roleId);
    }

    @Override
    public List<SysPermissionVO> queryRoleOfPermission(Long roleId) {
        return getBaseMapper().queryRoleOfPermission(roleId);
    }
}
