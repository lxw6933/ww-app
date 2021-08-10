package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.mvc.dao.SysJobGroupDao;
import com.ww.mall.mvc.entity.SysJobGroupEntity;
import com.ww.mall.mvc.service.SysJobGroupService;
import org.springframework.stereotype.Service;

/**
 * 任务组
 *
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Service("sysJobGroupService")
public class SysJobGroupServiceImpl extends ServiceImpl<SysJobGroupDao, SysJobGroupEntity> implements SysJobGroupService {

}
