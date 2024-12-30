package com.ww.app.admin.controller;

import com.ww.app.admin.service.ServiceFactory;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-21- 16:16
 * @description:
 */
public abstract class AbstractController {

    @Resource
    protected ServiceFactory sf;

}
