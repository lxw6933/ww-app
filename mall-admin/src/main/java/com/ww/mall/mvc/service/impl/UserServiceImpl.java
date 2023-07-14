package com.ww.mall.mvc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.mvc.dao.UserDao;
import com.ww.mall.mvc.entity.User;
import com.ww.mall.mvc.manager.SpringContextManager;
import com.ww.mall.mvc.service.UserService;
import com.ww.mall.mvc.view.vo.UserVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2021-04-16 09:45
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {

    @Resource
    private SpringContextManager springContextManager;

    @Override
    public void save(UserVO userVO) {
        UserService bean = springContextManager.getBean(UserService.class);

        bean.update(userVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserVO userVO) {
        User user = new User();
        BeanCopierUtils.copyProperties(userVO, user);
        super.save(user);
    }

    @Override
    @Async
    public void async() {
        UserService bean = springContextManager.getBean(UserService.class);
        System.out.println(Thread.currentThread().getName()+"=========async========="+bean.toString());
    }
}
