package com.skada.common.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Snowflake 分布式ID生成器
 * <p>
 * 结构：1位符号位 + 41位时间戳（毫秒） + 10位workerId + 12位序列号
 * 支持最多1024个节点，每个节点每毫秒可生成4096个ID
 * </p>
 */
public class SnowflakeIdGenerator {

    private static final Logger log = LogManager.getLogger(SnowflakeIdGenerator.class);

    /** 起始时间戳：2025-01-01 00:00:00 UTC */
    private static final long EPOCH = 1735689600000L;

    /** workerId 位数 */
    private static final long WORKER_ID_BITS = 10L;

    /** 序列号位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大workerId */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 序列号掩码 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** workerId左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 时间戳左移位数 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "workerId must be between 0 and " + MAX_WORKER_ID + ", got " + workerId);
        }
        this.workerId = workerId;
        log.info("SnowflakeIdGenerator initialized with workerId={}", workerId);
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            log.warn("Clock moved backwards, waiting for lastTimestamp={}", lastTimestamp);
            while (timestamp < lastTimestamp) {
                timestamp = System.currentTimeMillis();
            }
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 当前毫秒序列号用完，等待下一毫秒
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
}
