package com.skada.api.config;

import com.google.gson.Gson;
import com.skada.common.interceptor.LogInterceptor;
import com.skada.common.interceptor.ValidationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注册API服务所需的拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Gson gson;

    public WebMvcConfig(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 数据校验拦截器 - 校验请求格式和大小
        registry.addInterceptor(new ValidationInterceptor(gson)).addPathPatterns("/api/**");
        // 日志拦截器 - 记录所有接口调用
        registry.addInterceptor(new LogInterceptor()).addPathPatterns("/**");
    }
}
