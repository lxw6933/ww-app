package com.ww.mall.utils.pdf;

import com.lowagie.text.pdf.BaseFont;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

/**
 * @description: html to pdf utils
 * @author: ww
 * @create: 2021-04-14 19:11
 */
@Slf4j
@Component
public class HtmlToPdfUtil {

    private HtmlToPdfUtil() {}

    /**
     * 通过html模板生成pdf文件
     * @param data 参数
     * @param templateFileName 模板文件名
     */
    public static ByteArrayOutputStream exportPdfByModel(Map<String,Object> data, String templateFileName) {
        // 创建文件输出流存放pdf字节文件流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 创建一个FreeMarker实例, 负责管理FreeMarker模板的Configuration实例
        Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        // 指定FreeMarker模板文件的目录位置
        cfg.setClassForTemplateLoading(HtmlToPdfUtil.class,"/templates");
        // 生成pdf类
        ITextRenderer renderer = new ITextRenderer();
        try {
            // 设置中文字体(模板中必须使用设置的中文字体)
            renderer.getFontResolver().addFont("/templates/font/msyh.ttc", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            // 设置模板的编码格式
            cfg.setEncoding(Locale.CHINA, "UTF-8");
            // 根据上面设置的模板路径和传入的模板名称获取模板
            Template template = cfg.getTemplate(templateFileName, "UTF-8");
            StringWriter writer = new StringWriter();
            // 渲染变量参数
            template.process(data, writer);
            String html = writer.toString();
            // 把html代码传入pdf文档中
            renderer.setDocumentFromString(html);
            // 设置模板中的图片路径 （这里的images在resources目录下） 模板中img标签src路径需要相对路径加图片名 如<img src="images/xh.jpg"/>
//            String url = Objects.requireNonNull(HtmlToPdfUtil.class.getClassLoader().getResource("images")).toURI().toString();
//            renderer.getSharedContext().setBaseURL(url);

            renderer.layout();
            renderer.createPDF(out);
            renderer.finishPDF();
            out.flush();
        }catch (Exception e){
            e.printStackTrace();
            log.error("生成pdf失败");
        }
        return out;
    }

    /**
     * 根据html字符串模板生成PDF文件
     * @param template HTML的模板内容
     * @param params 参数
     * @return ByteArrayOutputStream
     */
    public static ByteArrayOutputStream exportPdfByString(String template, Map<String,Object> params) {
        // 创建文件输出流存放pdf字节文件流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 创建一个FreeMarker实例, 负责管理FreeMarker模板的Configuration实例
        Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        // 构造StringTemplateLoader用来解析字符串模板
        StringTemplateLoader loader = new StringTemplateLoader();
        // 添加String模板
        loader.putTemplate("template", template);
        // 设置模板加载类
        configuration.setTemplateLoader(loader);
        StringWriter writer = new StringWriter();
        // 生成pdf类
        ITextRenderer renderer = new ITextRenderer();
        try {
            // 获取刚才添加的String模板
            Template templates = configuration.getTemplate("template");
            // 渲染变量参数
            templates.process(params, writer);
            String html = writer.toString();
            renderer.setDocumentFromString(html);
            // 解决中文不显示问题
            ITextFontResolver fontResolver = renderer.getFontResolver();
            // 设置中文字体
            ClassPathResource font = new ClassPathResource("/templates/font/msyh.ttc,0");
            fontResolver.addFont(font.getPath(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            renderer.layout();
            renderer.createPDF(out);
            renderer.finishPDF();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("生成pdf失败");
        }
        return out;
    }
}
