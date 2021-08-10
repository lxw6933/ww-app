package com.ww.mall.mvc.controller;

import com.alibaba.excel.EasyExcelFactory;
import com.google.zxing.WriterException;
import com.ww.mall.annotation.Limit;
import com.ww.mall.annotation.Secret;
import com.ww.mall.annotation.SysLog;
import com.ww.mall.common.common.R;
import com.ww.mall.common.utils.BeanCopierUtils;
import com.ww.mall.common.utils.image.BarcodeUtils;
import com.ww.mall.config.drools.entity.Calculation;
import com.ww.mall.config.drools.service.RuleManager;
import com.ww.mall.config.excel.ExcelManager;
import com.ww.mall.config.excel.listener.UploadDataModelMqListener;
import com.ww.mall.config.excel.model.UserDataModel;
import com.ww.mall.config.mybatisplus.page.MyPageHelper;
import com.ww.mall.config.mybatisplus.page.MyPageInfo;
import com.ww.mall.config.mybatisplus.page.Pagination;
import com.ww.mall.config.rabbitmq.publisher.UserPublisher;
import com.ww.mall.mvc.controller.admin.AbstractController;
import com.ww.mall.mvc.entity.SysLogEntity;
import com.ww.mall.mvc.entity.User;
import com.ww.mall.mvc.service.UserService;
import com.ww.mall.mvc.view.form.IdForm;
import com.ww.mall.mvc.view.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
import javax.validation.constraints.Email;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @description: demo
 * @author: ww
 * @create: 2021-04-16 09:51
 */
@Slf4j
@RestController
@Validated
@Secret
public class DemoController extends AbstractController {

    @Resource
    private UserService userService;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private ExcelManager excelManager;

    @Resource
    private RuleManager ruleManager;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MessagePostProcessor messagePostProcessor;

    @Resource
    private UserPublisher userPublisher;

    /**
     * 删除
     */
    @PostMapping("/delete")
    public R delete(@RequestBody @Validated IdForm form) {
        userService.removeById(form.getId());
        return R.ok();
    }

    @GetMapping("/async")
    public R async(@RequestParam("email") @Email(message = "邮箱格式错误") String email) {
        log.info(Thread.currentThread().getName()+"=====controller======"+email);
        userService.async();
        return R.ok();
    }

    @PostMapping("/add")
    @SysLog(value = "新增用户记录", type = SysLogEntity.LOG_TYPE_TENANT)
    public boolean add(@RequestBody @Validated User user) {
        boolean save = userService.save(user);
        CompletableFuture.runAsync(() -> {
            try {
                log.info("异步执行任务");
            } catch (Exception e) {
                log.warn("动态移除远程问诊订单自动完成任务异常：", e);
            }
        }, threadPoolExecutor);
        return save;
    }

    @GetMapping("/list")
    public R list(Pagination pagination) {
        MyPageHelper.startPage(pagination, UserVO.class);
        List<User> list = userService.list();
        MyPageInfo<UserVO> pageInfo = new MyPageInfo<>(list).convert(
                res -> {
                    UserVO vo = new UserVO();
                    BeanCopierUtils.copyProperties(res, vo);
                    return vo;
                }
        );
        return R.ok(pageInfo);
    }

    @GetMapping("download")
    public void download(HttpServletResponse response) throws IOException {
        List<UserDataModel> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserDataModel data = new UserDataModel();
            data.setName("测试"+i);
            data.setAge(i);
            list.add(data);
        }
        excelManager.exportExcelOfOneSheet(response, list, UserDataModel.class, "测试","测试sheet");
    }

    @GetMapping("download/many/sheet")
    public void downloadManySheet(HttpServletResponse response) {
        Map<String, List<?>> map = new HashMap<>(256);

        List<UserDataModel> list1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserDataModel data = new UserDataModel();
            data.setName("测试"+i);
            data.setAge(i);
            list1.add(data);
        }
        map.put("userModel", list1);

        List<User> list2 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User data = new User();
            data.setName("学生"+i);
            data.setAge(i);
            data.setCreateTime(new Date());
            list2.add(data);
        }
        map.put("user", list2);

        try {
            excelManager.exportExcelOfManySheet(response, map, "测试");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @PostMapping("upload")
    @Transactional(rollbackFor = Exception.class)
    public String upload(MultipartFile file) throws IOException {
        if (file == null) {
            throw new ValidationException("上传文件不能为空");
        }
//        excelManager.readExcel(file, UserDataModel.class, userService);
        EasyExcelFactory.read(
                file.getInputStream(),
                UserDataModel.class,
                new UploadDataModelMqListener(userService, rabbitTemplate, messagePostProcessor, userPublisher)
        ).sheet().doRead();
        return "success";
    }

    @PostMapping("create/barCode")
    public void barCode() {
        try {
            BarcodeUtils.createWithText("ww123456","D:/");
        } catch (IOException | WriterException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/limit")
    @Limit(key = "test", period = 60, count = 3, name = "测试接口", prefix = "limit")
    public R getData() {
        return R.ok();
    }

    @PostMapping("/drools/calculation")
    public R calculation(@RequestBody Calculation calculation) {
        return R.oks(ruleManager.calculation(calculation));
    }

    private static final String USER_DRL_STRING = "package rules;\n" +
            "\n" +
            "import com.ww.test.mvc.entity.User;\n" +
            "import java.util.List;\n" +
            "global java.util.List listRules;\n" +
            "\n" +
            "rule \"personCheck_10\"\n" +
            "    salience 65535\n" +
            "    agenda-group \"sign\"\n" +
            "    when\n" +
            "        $person : User(sex != 1)\n" +
            "    then\n" +
            "        listRules.add(\"性别不对\");\n" +
            "end\n" +
            "\n" +
            "rule \"personCheck_11\"\n" +
            "    salience 65534\n" +
            "    agenda-group \"sign\"\n" +
            "    when\n" +
            "        $person : User(age < 22 || age > 25)\n" +
            "    then\n" +
            "        listRules.add(\"年龄不合适\");\n" +
            "end\n" +
            "\n" +
            "rule \"personCheck_12\"\n" +
            "    salience 65533\n" +
            "    agenda-group \"sign\"\n" +
            "    when\n" +
            "        $person : User(salary < 10000)\n" +
            "    then\n" +
            "        listRules.add(\"工资太低了\");\n" +
            "end\n";

    @PostMapping("/drools/drl/str/user")
    public R drlStringUser(@RequestBody User user) throws Exception {
        return R.oks(ruleManager.userByDrlString(user, USER_DRL_STRING));
    }

}
