
package top.suilian.aio.Util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;


@Configuration
public class ThreadPoolExcutorsConfig {
    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor poolTaskExecutor = new ThreadPoolTaskExecutor();

        // 核心线程数
        poolTaskExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors());

        //最大线程数
        poolTaskExecutor.setMaxPoolSize(Runtime.getRuntime().availableProcessors()+1);

        //空闲存活时间
        poolTaskExecutor.setKeepAliveSeconds(60);

        //队列数量
        poolTaskExecutor.setQueueCapacity(60);

        //线程名字前缀
        poolTaskExecutor.setThreadNamePrefix("DingMsg-Thread-");

        // 拒绝策略：丢弃队列里最近的一个任务，并执行当前任务
        poolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        poolTaskExecutor.initialize();

        return poolTaskExecutor;
    }

}
