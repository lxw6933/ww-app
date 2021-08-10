package com.ww.mall.mvc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.mvc.entity.User;
import com.ww.mall.mvc.view.vo.UserVO;

/**
 * @description:
 * @author: ww
 * @create: 2021-04-16 09:44
 */
public interface UserService extends IService<User> {

    void save(UserVO userVO);

    void update(UserVO userVO);

    void async();

}
