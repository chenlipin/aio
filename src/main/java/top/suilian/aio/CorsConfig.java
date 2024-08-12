/*
 * Copyright (C) 1997-2022 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <B>Description:</B> <br>
 * <B>Create on:</B> 2024/8/12 11:16 <br>
 *
 * @author dong.wan
 * @version 1.0
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**") // 允许跨域的路径
                    .allowedOrigins("*") // 允许跨域请求的域名
                    .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许的请求方法
                    .allowedHeaders("*") // 允许的请求头
                    .allowCredentials(true); // 是否允许证书（cookies）
        }
}
