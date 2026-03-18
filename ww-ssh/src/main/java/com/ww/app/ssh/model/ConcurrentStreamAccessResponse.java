package com.ww.app.ssh.model;

import lombok.Data;

/**
 * 并发流查看入口可见性响应。
 * <p>
 * 用于告知前端当前访问方是否具备查看实时流占用概览的权限，
 * 以便决定是否展示右上角的“并发流”按钮。
 * </p>
 */
@Data
public class ConcurrentStreamAccessResponse {

    /**
     * 当前访问方是否允许查看并发流使用概览。
     */
    private boolean enabled;

}
