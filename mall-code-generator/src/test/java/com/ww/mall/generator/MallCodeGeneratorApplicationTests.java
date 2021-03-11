package com.ww.mall.generator;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.po.TableFill;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.ww.mall.generator.config.GeneratorConfig;
import com.ww.mall.generator.dao.TableMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
class MallCodeGeneratorApplicationTests {

    @Autowired
    GeneratorConfig config;
    @Autowired
    private TableMapper tableMapper;

    @Test
    public void getAllTables(){
        List<Map> maps = tableMapper.listTable();
        StringBuilder tableNames = new StringBuilder(0);
        maps.forEach(res->{
            System.out.println();
            tableNames.append(res.get("TABLE_NAME")).append(",");
        });
        System.out.println(tableNames.deleteCharAt(tableNames.length()-1).toString());
        String url = "jdbc:mysql://localhost:3306/ww_mall_pms?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8";
        String driveName = "com.mysql.cj.jdbc.Driver";
        String username = "root";
        String password = "admin";
        String tablePrefix = "pms_";
        config.generate(url,
                driveName,
                username,
                password,
                tablePrefix,
                "com.ww.mall.product",
                tableNames.toString());

    }

    @Test
    public void generator(){
        config.generate();
    }


    @Test
    void contextLoads() {
        // 代码生成器
        AutoGenerator ag = new AutoGenerator();
        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        String projectPath = System.getProperty("user.dir");
        gc.setOutputDir(projectPath+"/src/main/java");
        gc.setAuthor("ww");
        gc.setOpen(false);
        gc.setBaseResultMap(true);      // 设置resultMap
        gc.setFileOverride(false);      // 设置文件是否覆盖
        gc.setServiceName("%sService"); // 去除Service前面的I
        gc.setIdType(IdType.AUTO);      // 设置ID类型
        gc.setDateType(DateType.ONLY_DATE);
        gc.setSwagger2(false);
        ag.setGlobalConfig(gc);

        // 设置数据源
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl("jdbc:p6spy:mysql://localhost:3306/cloud?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8");
        dsc.setDriverName("com.mysql.cj.jdbc.Driver");
        dsc.setUsername("root");
        dsc.setPassword("admin");
        dsc.setDbType(DbType.MYSQL);
        ag.setDataSource(dsc);

        // 包配置
        PackageConfig pc = new PackageConfig();
        pc.setModuleName("generator");
        pc.setParent("com.ww.mall");
        pc.setController("controller");
        pc.setEntity("entity");
        pc.setMapper("dao");
        pc.setService("service");
        pc.setServiceImpl("service.impl");
        ag.setPackageInfo(pc);

        // 策略配置
        StrategyConfig sc = new StrategyConfig();
        sc.setInclude("user");      // 设置要映射的表名
        sc.setNaming(NamingStrategy.underline_to_camel);    // 设置驼峰下划线命名映射
        sc.setColumnNaming(NamingStrategy.underline_to_camel);
        sc.setEntityLombokModel(true);           // 使用Lombok注解
//        sc.setLogicDeleteFieldName("deleted"); // 设置逻辑删除字段

        TableFill insert = new TableFill("gmt_create", FieldFill.INSERT);
        TableFill update = new TableFill("gmt_modified", FieldFill.INSERT_UPDATE);
        List<TableFill> tableFillList = new ArrayList<>();
        tableFillList.add(insert);
        tableFillList.add(update);
        sc.setTableFillList(tableFillList); // 自动填充配置
//        sc.setVersionFieldName("version");  // 设置乐观锁
        sc.setRestControllerStyle(true);    // 设置RestController风格
        sc.setControllerMappingHyphenStyle(true);
        ag.setStrategy(sc);    // 设置策略配置

        // 设置自定义模板
        ag.setTemplateEngine(new FreemarkerTemplateEngine());
        TemplateConfig tc = new TemplateConfig();
        tc.setController("/controller.java");
        tc.setService("/service.java");
        tc.setServiceImpl("/serviceImpl.java");
        tc.setEntity("/entity.java");
        tc.setMapper("/mapper.java");
        tc.setXml("/mapper.xml");
        ag.setTemplate(tc);

        ag.execute();          // 执行

    }

}
