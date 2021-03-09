package com.ww.mall.generator.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.po.TableFill;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Author:         ww
 * Datetime:       2021\3\9 0009
 * Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@PropertySource("classpath:generate.properties")
public class GeneratorConfig {

    @Value("${generator.url}")
    private String url;

    @Value("${generator.username}")
    private String username;

    @Value("${generator.password}")
    private String password;

    @Value("${generator.driver-class-name}")
    private String driverName;

    @Value("${generator.table.name}")
    private String tableNames;

    @Value("${generator.package.parent}")
    private String packageParent;

    @Value("${generator.prefix}")
    private String tablePrefix;

    public void generate(){
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
        dsc.setUrl(url);
        dsc.setDriverName(driverName);
        dsc.setUsername(username);
        dsc.setPassword(password);
        dsc.setDbType(DbType.MYSQL);
        ag.setDataSource(dsc);

        // 包配置
        PackageConfig pc = new PackageConfig();
        pc.setParent(packageParent);
        pc.setController("controller");
        pc.setEntity("entity");
        pc.setMapper("dao");
        pc.setXml("xml");
        pc.setService("service");
        pc.setServiceImpl("service.impl");
        ag.setPackageInfo(pc);

        // 策略配置
        StrategyConfig sc = new StrategyConfig();
        sc.setInclude(tableNames.split(","));         // 设置要映射的表名
        sc.setNaming(NamingStrategy.underline_to_camel);    // 设置驼峰下划线命名映射
        sc.setColumnNaming(NamingStrategy.underline_to_camel);
        sc.setEntityLombokModel(true);           // 使用Lombok注解
        if(StrUtil.isNotEmpty(tablePrefix)){
            sc.setTablePrefix(tablePrefix);        // 设置表名前缀
        }
//        sc.setLogicDeleteFieldName("deleted");   // 设置逻辑删除字段

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
        tc.setController("/templates/controller.java");
        tc.setService("/templates/service.java");
        tc.setServiceImpl("/templates/serviceImpl.java");
        tc.setEntity("/templates/entity.java");
        tc.setMapper("/templates/mapper.java");
        tc.setXml("/templates/mapper.xml");
        ag.setTemplate(tc);

        ag.execute();          // 执行
    }


}
