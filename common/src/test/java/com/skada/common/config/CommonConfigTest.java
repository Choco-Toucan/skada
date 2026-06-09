package com.skada.common.config;

import com.google.gson.Gson;
import com.skada.common.util.DistributedLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CommonConfig 单元测试")
class CommonConfigTest {

    static class TestDto {
        String name;
        String empty;
    }

    @Test
    @DisplayName("gson() 返回配置好的 Gson 实例，含 serializeNulls")
    void gsonBean() {
        CommonConfig config = new CommonConfig();
        Gson gson = config.gson();

        assertThat(gson).isNotNull();
        TestDto dto = new TestDto();
        dto.name = "test";
        dto.empty = null;
        String json = gson.toJson(dto);
        assertThat(json).contains("test");
        // serializeNulls 生效：null 字段也会出现在JSON中
        assertThat(json).contains("empty");
    }

    @Test
    @DisplayName("distributedLock() 返回 DistributedLock 实例")
    void distributedLockBean() {
        CommonConfig config = new CommonConfig();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        DistributedLock lock = config.distributedLock(redisTemplate);

        assertThat(lock).isNotNull();
    }
}
