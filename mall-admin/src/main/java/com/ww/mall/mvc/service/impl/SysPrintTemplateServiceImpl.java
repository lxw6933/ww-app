package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.config.mybatisplus.page.MyPageHelper;
import com.ww.mall.config.mybatisplus.page.MyPageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.dao.SysPrintTemplateDao;
import com.ww.mall.mvc.entity.SysPrintTemplateEntity;
import com.ww.mall.mvc.service.SysPrintTemplateService;
import com.ww.mall.mvc.view.form.admin.SysPrintTemplateForm;
import com.ww.mall.mvc.view.vo.admin.SysPrintTemplateVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-17 13:30
 */
@Service
public class SysPrintTemplateServiceImpl extends ServiceImpl<SysPrintTemplateDao, SysPrintTemplateEntity> implements SysPrintTemplateService {

    /**
     * 分页
     *
     * @param pagination 分页条件
     * @param query      查询条件
     * @return PageInfo
     */
    @Override
    public PageInfo<SysPrintTemplateVO> page(Pagination pagination, QueryWrapper<SysPrintTemplateEntity> query) {
        MyPageHelper.startPage(pagination, SysPrintTemplateVO.class);
        List<SysPrintTemplateEntity> list = list(query);
        return new MyPageInfo<>(list).convert(entity -> {
            SysPrintTemplateVO vo = new SysPrintTemplateVO();
            BeanCopierUtils.copyProperties(entity, vo);
            return vo;
        });
    }

    /**
     * 详情
     *
     * @param id 主键ID
     * @return SysPrintTemplateVO
     */
    @Override
    public SysPrintTemplateVO info(Long id) {
        SysPrintTemplateEntity entity = super.getById(id);
        SysPrintTemplateVO vo = new SysPrintTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * 新增
     *
     * @param form 表单信息
     * @return SysPrintTemplateVO
     */
    @Override
    public SysPrintTemplateVO save(SysPrintTemplateForm form) {
        SysPrintTemplateEntity entity = new SysPrintTemplateEntity();
        BeanUtils.copyProperties(form, entity);
        super.save(entity);
        return info(entity.getId());
    }

    /**
     * 编辑
     *
     * @param form 表单信息
     */
    @Override
    public SysPrintTemplateVO update(SysPrintTemplateForm form) {
        SysPrintTemplateEntity entity = super.getById(form.getId());
        BeanUtils.copyProperties(form, entity);
        super.updateById(entity);
        return info(entity.getId());
    }

    /**
     * 删除
     *
     * @param id 主键ID
     */
    @Override
    public void deleteById(Long id) {
        SysPrintTemplateEntity entity = super.getById(id);
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
}
