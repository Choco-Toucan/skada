package com.skada.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 公共模块自动配置
 */
@Configuration
public class CommonConfig {

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .serializeNulls()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }
}
