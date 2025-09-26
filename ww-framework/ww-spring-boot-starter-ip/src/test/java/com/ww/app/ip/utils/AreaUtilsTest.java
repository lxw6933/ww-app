package com.ww.app.ip.utils;

import com.ww.app.ip.common.Area;
import org.junit.jupiter.api.Test;

/**
 * @author ww
 * @create 2025-09-25 16:59
 * @description:
 */
public class AreaUtilsTest {

    @Test
    public void testGetArea() {
        // 调用：北京
        Area area = AreaUtils.getArea(110100);
        printArea(area);
    }

    private static void printArea(Area area) {
        System.out.println("===============区域父类子集合===============");
        area.getParent().getChildren().forEach(System.out::println);
        System.out.println("===============区域子集合===============");
        area.getChildren().forEach(System.out::println);
    }

    @Test
    public void testParseArea() {
        printArea(AreaUtils.parseArea("河南省/石家庄市/新华区"));
    }

    @Test
    public void testFormat() {
        System.out.println(AreaUtils.format(110105));
        System.out.println(AreaUtils.format(1));
        System.out.println(AreaUtils.format(2));
    }

    @Test
    public void testTree() {
        Area area = AreaUtils.getArea(Area.ID_CHINA);
        area.getChildren().forEach(e -> {
            System.out.println("id：" + e.getId() + "  name：" + e.getName());
            e.getChildren().forEach(child -> {
                System.out.println("--id：" + child.getId() + "  name：" + child.getName());
                child.getChildren().forEach(child2 -> System.out.println("----id：" + child2.getId() + "  name：" + child2.getName()));
            });
        });
    }

}
