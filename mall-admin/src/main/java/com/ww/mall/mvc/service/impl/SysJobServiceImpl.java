package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.mvc.dao.SysJobDao;
import com.ww.mall.mvc.entity.SysJobEntity;
import com.ww.mall.mvc.service.SysJobService;
import org.springframework.stereotype.Service;

/**
 * 定时任务
 *
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Service("sysJobService")
public class SysJobServiceImpl extends ServiceImpl<SysJobDao, SysJobEntity> implements SysJobService {

}
