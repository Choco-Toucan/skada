package com.skada.mng.config;

import com.google.gson.Gson;
import com.skada.common.interceptor.LogInterceptor;
import com.skada.common.interceptor.LoginInterceptor;
import com.skada.common.interceptor.PermissionInterceptor;
import com.skada.common.interceptor.ValidationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注册拦截器和Bean
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final StringRedisTemplate redisTemplate;
    private final Gson gson;

    public WebMvcConfig(StringRedisTemplate redisTemplate, Gson gson) {
        this.redisTemplate = redisTemplate;
        this.gson = gson;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 数据校验拦截器 - 校验请求格式和大小
        registry.addInterceptor(new ValidationInterceptor(gson)).addPathPatterns("/api/**");

        // 日志拦截器 - 记录所有接口调用
        registry.addInterceptor(new LogInterceptor()).addPathPatterns("/**");

        // 登录拦截器 - 校验登录态
        registry.addInterceptor(new LoginInterceptor(redisTemplate, gson))
                .addPathPatterns("/api/**");

        // 权限拦截器 - 校验接口权限
        registry.addInterceptor(new PermissionInterceptor(gson))
                .addPathPatterns("/api/**");
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
