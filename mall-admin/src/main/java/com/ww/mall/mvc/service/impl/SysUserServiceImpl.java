package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.common.utils.BeanCopierUtils;
import com.ww.mall.config.mybatisplus.page.MyPageHelper;
import com.ww.mall.config.mybatisplus.page.MyPageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.config.security.entity.MyUserDetails;
import com.ww.mall.mvc.dao.SysUserDao;
import com.ww.mall.mvc.entity.SysUserEntity;
import com.ww.mall.mvc.service.SysUserService;
import com.ww.mall.mvc.view.form.admin.ModifyPasswordForm;
import com.ww.mall.mvc.view.form.admin.SysUserForm;
import com.ww.mall.mvc.view.form.admin.UserAndRoleForm;
import com.ww.mall.mvc.view.vo.admin.SysPermissionVO;
import com.ww.mall.mvc.view.vo.admin.SysRoleVO;
import com.ww.mall.mvc.view.vo.admin.SysUserVO;
import com.ww.mall.utils.LoginUserUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台用户表
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Slf4j
@Service("sysUserService")
public class SysUserServiceImpl extends ServiceImpl<SysUserDao, SysUserEntity> implements SysUserService {

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private SysUserService sysUserService;

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    @Override
    public PageInfo<SysUserVO> page(Pagination pagination, QueryWrapper<SysUserEntity> query) {
        MyPageHelper.startPage(pagination, SysUserVO.class);
        List<SysUserEntity> list = list(query);
        return new MyPageInfo<>(list).convert(entity -> {
            SysUserVO vo = new SysUserVO();
            BeanCopierUtils.copyProperties(entity, vo);
            return vo;
        });
    }

    /**
     * 详情
     *
     * @param id 主键ID
     * @return SysUserVO
     */
    @Override
    public SysUserVO info(Long id) {
        SysUserEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        SysUserVO vo = new SysUserVO();
        BeanCopierUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * 新增
     *
     * @param form 表单信息
     * @return SysUserVO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserVO save(SysUserForm form) {
        // 保证username不重复
        List<SysUserEntity> userList = super.list(new QueryWrapper<SysUserEntity>()
                .eq("username", form.getUsername())
        );
        if (CollectionUtils.isNotEmpty(userList)) {
            throw new ValidatorException("新增用户的账号已存在！不能添加重复账号用户。");
        }
        SysUserEntity entity = new SysUserEntity();
        BeanCopierUtils.copyProperties(form, entity);
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        super.save(entity);
        if (CollectionUtils.isNotEmpty(form.getRoleIds())) {
            // 保存用户角色关联信息
            UserAndRoleForm data = new UserAndRoleForm();
            data.setRoleIds(form.getRoleIds());
            data.setUserId(entity.getId());
            sysUserService.insertUserAndRole(data);
        }
        return info(entity.getId());
    }

    /**
     * 编辑
     *
     * @param form 表单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserVO update(SysUserForm form) {
        SysUserEntity entity = super.getById(form.getId());
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        form.setUsername(entity.getUsername());
        BeanCopierUtils.copyProperties(form, entity);
        super.updateById(entity);
        // 更新用户角色关联信息
        // 判断角色是否变化
        List<Long> sources = getBaseMapper().queryUserOfRole(entity.getUsername())
                            .stream()
                            .map(SysRoleVO::getId).collect(Collectors.toList());
        if (!CollectionUtils.isEqualCollection(form.getRoleIds(), sources)) {
            log.info("角色数据变化，更新关联数据");
            // 删除之前所有的关联信息，新增目前的关联信息
            getBaseMapper().removeUserAndRole(entity.getId());
            if (CollectionUtils.isNotEmpty(form.getRoleIds())) {
                UserAndRoleForm data = new UserAndRoleForm();
                data.setUserId(entity.getId());
                data.setRoleIds(form.getRoleIds());
                getBaseMapper().insertUserAndRole(data);
            }
        }
        return info(entity.getId());
    }

    /**
     * 删除
     *
     * @param id 主键ID
     */
    @Override
    public void deleteById(Long id) {
        SysUserEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("信息不存在");
        }
        super.removeById(id);
    }

    /**
     * 批量删除
     *
     * @param ids 主键ID
     */
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
    public List<SysRoleVO> queryUserOfRole(String username) {
        return getBaseMapper().queryUserOfRole(username);
    }

    @Override
    public List<SysPermissionVO> queryUserOfPermission(String username) {
        return getBaseMapper().queryUserOfPermission(username);
    }

    @Override
    public void modifyPassword(ModifyPasswordForm form) {
        String username = LoginUserUtils.getCurrentUserName();
        SysUserEntity user = super.getOne(new QueryWrapper<SysUserEntity>()
                .eq("username", username)
        );
        // 获取当前登陆用户
        MyUserDetails currentUser = LoginUserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ValidatorException("登陆异常！请重新登陆");
        }
        super.update(new UpdateWrapper<SysUserEntity>()
                .eq("id", form.getId())
                .set("password", passwordEncoder.encode(form.getNewPassword()))
                .set("update_time", new Date())
                .set("update_by", currentUser.getId())
        );
    }

    @Override
    public void insertUserAndRole(UserAndRoleForm form) {
        getBaseMapper().insertUserAndRole(form);
    }

}
