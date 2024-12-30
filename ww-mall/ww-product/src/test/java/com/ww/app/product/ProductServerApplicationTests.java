package com.ww.app.product;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ww.app.product.entity.Category;
import com.ww.app.product.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductServerApplicationTests {

    @Autowired
    private CategoryService categoryService;

    @Test
    void contextLoads() {
        UpdateWrapper<Category> wrapper = new UpdateWrapper<>();
        wrapper.eq("cat_id", 2).eq("cat_level", "1")
                .set("name", "大哥大").set("ico", "ico");
        boolean update = categoryService.update(wrapper);
        System.out.println("=======end==========");
    }

}
