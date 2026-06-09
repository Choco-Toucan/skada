package com.skada.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedLock 单元测试")
class DistributedLockTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private DistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        distributedLock = new DistributedLock(redisTemplate);
    }

    @Nested
    @DisplayName("tryLock")
    class TryLock {

        @Test
        @DisplayName("获取锁成功返回 true")
        void lockSuccess() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(eq("skada:lock:test"), eq("holder1"), eq(10L), eq(TimeUnit.SECONDS)))
                    .thenReturn(true);

            boolean result = distributedLock.tryLock("test", "holder1", 10, TimeUnit.SECONDS);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("获取锁失败返回 false")
        void lockFailed() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(false);

            boolean result = distributedLock.tryLock("test", "holder1", 10, TimeUnit.SECONDS);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Redis 返回 null 按 false 处理")
        void lockReturnsNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(null);

            boolean result = distributedLock.tryLock("test", "holder1", 10, TimeUnit.SECONDS);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("unlock")
    class Unlock {

        @Test
        @DisplayName("释放锁成功（Lua脚本返回1）")
        void unlockSuccess() {
            when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("skada:lock:test")), eq("holder1")))
                    .thenReturn(1L);

            boolean result = distributedLock.unlock("test", "holder1");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("释放锁失败（不是持有者，Lua脚本返回0）")
        void unlockFailed() {
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                    .thenReturn(0L);

            boolean result = distributedLock.unlock("test", "other");

            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("lockKey 返回 skada:lock:{name} 格式")
    void lockKeyFormat() {
        assertThat(DistributedLock.lockKey("my-resource")).isEqualTo("skada:lock:my-resource");
    }
}
