package com.skada.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的分布式锁
 * <p>
 * 使用 SET NX EX 实现原子性加锁，Lua脚本实现安全解锁。
 * 锁的key命名规则：skada:lock:{name}
 * </p>
 */
public class DistributedLock {

    private static final String LOCK_PREFIX = "skada:lock:";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> unlockScript;

    public DistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.unlockScript = new DefaultRedisScript<>();
        this.unlockScript.setScriptText("""
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """);
        this.unlockScript.setResultType(Long.class);
    }

    /**
     * 尝试获取锁
     *
     * @param name     锁名称
     * @param value    持有者标识（用于解锁时校验）
     * @param timeout  锁超时时间
     * @param timeUnit 时间单位
     * @return true 获取成功
     */
    public boolean tryLock(String name, String value, long timeout, TimeUnit timeUnit) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + name, value, timeout, timeUnit);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放锁
     * 使用Lua脚本确保只有持有者才能释放
     */
    public boolean unlock(String name, String value) {
        Long result = redisTemplate.execute(
                unlockScript, List.of(LOCK_PREFIX + name), value);
        return Long.valueOf(1).equals(result);
    }

    /**
     * 生成锁key（方便运维手动操作Redis时识别）
     */
    public static String lockKey(String name) {
        return LOCK_PREFIX + name;
    }
}
