package com.skada.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skada.common.util.DistributedLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

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

    @Bean
    public DistributedLock distributedLock(StringRedisTemplate redisTemplate) {
        return new DistributedLock(redisTemplate);
    }
}
