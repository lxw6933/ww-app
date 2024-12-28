import com.ww.mall.minio.MinioTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class DemoTest {

    @Resource
    private MinioTemplate minioTemplate;

    @Test
    void contextLoads() {
        System.out.println("============================");
//        System.out.println(mallMinioUtil.existBucket("ww-demo"));
        System.out.println("============================");
    }

}
