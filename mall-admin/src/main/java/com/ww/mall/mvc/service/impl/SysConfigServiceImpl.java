package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import com.ww.mall.annotation.Cache;
import com.ww.mall.common.common.BatchProcessingResult;
import com.ww.mall.common.constant.RedisKeyConstant;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.config.mybatisplus.page.MyPageHelper;
import com.ww.mall.config.mybatisplus.page.MyPageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.mvc.dao.SysConfigDao;
import com.ww.mall.mvc.entity.SysConfigEntity;
import com.ww.mall.mvc.service.SysConfigService;
import com.ww.mall.mvc.view.form.admin.SysConfigForm;
import com.ww.mall.mvc.view.vo.admin.SysConfigVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-19 09:12
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigDao, SysConfigEntity> implements SysConfigService {

    @Override
    public PageInfo<SysConfigVO> page(Pagination pagination, QueryWrapper<SysConfigEntity> query) {
        MyPageHelper.startPage(pagination, SysConfigVO.class);
        List<SysConfigEntity> list = list(query);
        return new MyPageInfo<>(list).convert(entity -> {
            SysConfigVO vo = new SysConfigVO();
            BeanCopierUtils.copyProperties(entity, vo);
            return vo;
        });
    }

    @Override
    @Cache(key = RedisKeyConstant.CONFIG_KEY, timeout = 30, unit = TimeUnit.MINUTES)
    public String getConfigValue(String key) {
        return baseMapper.getConfigValue(key);
    }

    @Override
    public void save(SysConfigForm form) {
        SysConfigEntity entity = new SysConfigEntity();
        BeanUtils.copyProperties(form, entity);
        super.save(entity);
    }

    @Override
    public void update(SysConfigForm form) {
        SysConfigEntity entity = new SysConfigEntity();
        BeanUtils.copyProperties(form, entity);
        super.updateById(entity);
    }

    @Override
    public void updateStatus(Long id) {
        //根据主键获取记录判断记录是否存在
        SysConfigEntity entity = super.getById(id);
        if (entity == null) {
            throw new ValidatorException("id：【" + id + "】记录不存在");
        }
        //根据原纪录状态修改当前记录状态
        boolean status = true;
        if (Boolean.TRUE.equals(entity.getStatus())) {
            status = false;
        }
        super.update(new UpdateWrapper<SysConfigEntity>()
                .eq("id", id)
                .set("status", status)
        );
    }

    @Override
    public BatchProcessingResult batchDelete(List<Long> ids) {
        BatchProcessingResult result = new BatchProcessingResult();
        for (Long id : ids) {
            try {
                super.removeById(id);
                result.addSuccess(1);
            } catch (Exception ex) {
                result.addError(1).addErrorMsg(ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public <T> T getConfigObject(String key, Class<T> clazz) {
        String configValue = getConfigValue(key);
        if (StringUtils.isNotEmpty(configValue)) {
            return new Gson().fromJson(configValue, clazz);
        }
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new ValidatorException("转换configValue成【"+clazz.getName()+"】对象失败");
        }
    }

    public static void main(String[] args) {
        String name = Date.class.getSimpleName();
        System.out.println(name);
    }

}
