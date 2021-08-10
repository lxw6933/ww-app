package com.ww.mall.utils.pdf;

import com.google.zxing.WriterException;
import com.ww.mall.common.utils.image.BarcodeUtils;
import com.ww.mall.common.utils.image.QRCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: pdf测试
 * @author: ww
 * @create: 2021-04-14 19:49
 */
@Slf4j
@RestController
public class PdfController {

    private static final String CONTENT = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "  <meta charset=\"UTF-8\"/>\n" +
            "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"/>\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n" +
            "  <title>Title</title>\n" +
            "  <style>\n" +
            "    @page {\n" +
            "      /* 设置pdf页面类型 */\n" +
            "      size: a4;\n" +
            "    }\n" +
            "    html, body {\n" +
            "      /* 字体必须设置 */\n" +
            "      font-family: \"Microsoft YaHei\";\n" +
            "      padding: 0;\n" +
            "      margin: 0;\n" +
            "      font-size: 14px;\n" +
            "      font-weight: bold;\n" +
            "    }\n" +
            "    #main {\n" +
            "      color: #8b8b8b;\n" +
            "      width: 100%;\n" +
            "      margin: auto;\n" +
            "    }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div id=\"main\">\n" +
            "    <p>总数据：${vo?size}</p>\n" +
            "    ===========================\n" +
            "    二维码：${qrCode}\n" +
            "    条形码：${barCode}\n" +
            "    ===========================\n" +
            "    <table style=\"width: 100%;line-height: 30px;border-collapse: collapse;text-align: center\" border=\"1px 1px solid #999\">\n" +
            "        <tr>\n" +
            "          <th>名字</th>\n" +
            "          <th>日期</th>\n" +
            "          <th>作者</th>\n" +
            "        </tr>\n" +
            "        <#list vo as pdf>\n" +
            "        <tr>\n" +
            "          <td>${pdf.name}</td>\n" +
            "          <td>${pdf.time}</td>\n" +
            "          <td>${pdf.author}</td>\n" +
            "        </tr>\n" +
            "        </#list>\n" +
            "    </table>\n" +
            "\n" +
            "</div>\n" +
            "</body>\n" +
            "</html>\n";

    private static final List<PdfEntity> LIST = new ArrayList<>();

    static{
        int num = 10;
        // 初始化数据
        for (int i = 0; i < num; i++) {
            LIST.add(new PdfEntity("测试"+i,"2022-04-01","ww"));
        }
    }

    @RequestMapping("/pdf/model")
    public ResponseEntity<byte[]> testPdfModel() throws IOException, WriterException {
        Map<String,Object> map = new HashMap<>(256);
        map.put("vo", LIST);
        map.put("test", "WW");
        // =================初始化数据完毕=================
        // 二维码条形码
        String qrCodeBaseImg = Base64.encodeBase64String(QRCodeUtils.create("www.baidu.com").toByteArray());
        String barCodeBaseImg = Base64.encodeBase64String(BarcodeUtils.create("www.baidu.com").toByteArray());
        map.put("qrCode","<img src=\"data:image/png;base64,"+qrCodeBaseImg+"\" alt=\"二维码\"/>");
        map.put("barCode","<img src=\"data:image/png;base64,"+barCodeBaseImg+"\" alt=\"条形码\"/>");
        ByteArrayOutputStream bos = HtmlToPdfUtil.exportPdfByModel(map, "pdf测试模板.html");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + "测试model.pdf")
                .body(bos.toByteArray());
    }

    @RequestMapping("/pdf/str")
    public ResponseEntity<byte[]> testPdfStr() throws IOException, WriterException {
        Map<String,Object> map = new HashMap<>(256);
        map.put("vo", LIST);
        map.put("test", "WW");
        // 二维码条形码
        String qrCodeBaseImg = Base64.encodeBase64String(QRCodeUtils.create("www.baidu.com").toByteArray());
        String barCodeBaseImg = Base64.encodeBase64String(QRCodeUtils.create("www.baidu.com").toByteArray());
        map.put("qrCode","<img src=\"data:image/png;base64,"+qrCodeBaseImg+"\" alt=\"二维码\"/>");
        map.put("barCode","<img src=\"data:image/png;base64,"+barCodeBaseImg+"\" alt=\"条形码\"/>");
        ByteArrayOutputStream bos = HtmlToPdfUtil.exportPdfByString(CONTENT, map);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + "测试str.pdf")
                .body(bos.toByteArray());
    }


}
