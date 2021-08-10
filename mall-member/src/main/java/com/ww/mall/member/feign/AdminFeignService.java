package com.ww.mall.member.feign;

import com.ww.mall.common.common.R;
import com.ww.mall.member.config.mybatisplus.page.Pagination;
import com.ww.mall.member.to.AdminUserTo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @description:
 * @author: ww
 * @create: 2021/7/7 下午7:09
 **/
@FeignClient("mall-admin")
public interface AdminFeignService {

    /**
     * 分页
     */
    @GetMapping("/admin/sys/user/page")
    R page(@RequestParam("pagination") Pagination pagination, @RequestParam("query") AdminUserTo query);

}
