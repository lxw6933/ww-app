package com.ww.app.im.service;

/**
 * @author ww
 * @create 2024-12-25 11:40
 * @description:
 */
public interface OnLineService {

    /**
     * 用户是否在线
     */
    boolean isOnline(Long userId, Integer appId);

}
