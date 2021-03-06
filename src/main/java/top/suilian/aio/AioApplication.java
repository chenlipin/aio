package top.suilian.aio;

import org.apache.log4j.Logger;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan
@SpringBootApplication
@MapperScan("top.suilian.aio.dao")
public class    AioApplication {
    private static org.apache.log4j.Logger logger = Logger.getLogger(AioApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AioApplication.class, args);
        logger.info("Start Success");
    }

}
