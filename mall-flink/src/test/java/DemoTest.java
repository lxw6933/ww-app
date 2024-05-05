import com.ww.mall.minio.MallMinioTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class DemoTest {

    @Resource
    private MallMinioTemplate mallMinioTemplate;

    @Test
    void contextLoads() {
        System.out.println("============================");
//        System.out.println(mallMinioUtil.existBucket("mall-demo"));
        System.out.println("============================");
    }

}
