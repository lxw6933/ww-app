package com.ww.mall.config.drools;

import org.drools.decisiontable.InputType;
import org.drools.decisiontable.SpreadsheetCompiler;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021/5/23 下午4:14
 **/
public class DroolsRuleUtils {

    private DroolsRuleUtils() {
    }

    /**
     * 把xls文件解析为String
     *
     * @param realPath 文件真实路径 // 例如：C:\\abc.xls
     * @return drl文件内容字符串
     * @throws FileNotFoundException 文件路径异常
     */
    public static String getDrlStringByExcelPath(String realPath) throws FileNotFoundException {
        File file = new File(realPath);
        InputStream is = new FileInputStream(file);
        SpreadsheetCompiler compiler = new SpreadsheetCompiler();
        String drl = compiler.compile(is, InputType.XLS);
        System.out.println(drl);
        return drl;
    }

    /**
     * drl字符串获取KieSession
     *
     * @param drl drl文件内容字符串
     * @return KieSession
     * @throws Exception ex
     */
    public static KieSession getKieSessionByDrlString(String drl) throws Exception {
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        Results results = kieHelper.verify();
        if (results.hasMessages(Message.Level.WARNING, Message.Level.ERROR)) {
            List<Message> messages = results.getMessages(Message.Level.WARNING, Message.Level.ERROR);
            for (Message message : messages) {
                System.out.println("Error: " + message.getText());
            }
            // throw new IllegalStateException("Compilation errors were found. Check the logs.");
        }
        return kieHelper.build().newKieSession();
    }

    /**
     * 通过excel文件获取KieSession
     *
     * @param realPath Excel文件绝对路径
     * @return KieSession
     * @throws Exception ex
     */
    public static KieSession getKieSessionByExcelPath(String realPath) throws Exception {
        return getKieSessionByDrlString(getDrlStringByExcelPath(realPath));
    }

}
