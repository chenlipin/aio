/*
 * Copyright (C) 1997-2021 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.Util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * <B>Description:线程池配置</B>  <br>
 * <B>Create on:</B> 2021/9/3 9:29 <br>
 *
 * @author dong.wan
 * @version 1.0
 */
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
