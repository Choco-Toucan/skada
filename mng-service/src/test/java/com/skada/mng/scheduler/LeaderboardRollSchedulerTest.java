package com.skada.mng.scheduler;

import com.skada.common.util.DistributedLock;
import com.skada.mng.mapper.LeaderboardInstanceMapper;
import com.skada.mng.model.Leaderboard;
import com.skada.mng.model.LeaderboardInstance;
import com.skada.mng.service.LeaderboardConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardRollScheduler 单元测试")
class LeaderboardRollSchedulerTest {

    @Mock
    private LeaderboardConfigService leaderboardConfigService;

    @Mock
    private LeaderboardInstanceMapper instanceMapper;

    @Mock
    private DistributedLock distributedLock;

    private LeaderboardRollScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new LeaderboardRollScheduler(leaderboardConfigService, instanceMapper, distributedLock);
    }

    @Nested
    @DisplayName("toMilliseconds")
    class ToMilliseconds {

        @Test
        @DisplayName("minute → 毫秒")
        void minuteToMs() {
            assertThat(scheduler.toMilliseconds(1, "minute")).isEqualTo(60_000L);
            assertThat(scheduler.toMilliseconds(5, "minute")).isEqualTo(300_000L);
        }

        @Test
        @DisplayName("hour → 毫秒")
        void hourToMs() {
            assertThat(scheduler.toMilliseconds(1, "hour")).isEqualTo(3_600_000L);
        }

        @Test
        @DisplayName("day → 毫秒")
        void dayToMs() {
            assertThat(scheduler.toMilliseconds(1, "day")).isEqualTo(86_400_000L);
        }

        @Test
        @DisplayName("null 返回 0")
        void nullReturnsZero() {
            assertThat(scheduler.toMilliseconds(null, "minute")).isEqualTo(0);
            assertThat(scheduler.toMilliseconds(1, null)).isEqualTo(0);
        }

        @Test
        @DisplayName("无效单位返回 0")
        void invalidUnit() {
            assertThat(scheduler.toMilliseconds(1, "second")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("checkPeriodicRoll")
    class CheckPeriodicRoll {

        @Test
        @DisplayName("获取分布式锁失败则跳过执行")
        void lockFailed() {
            when(distributedLock.tryLock(eq("scheduler:periodic-roll"), anyString(), eq(30L), eq(TimeUnit.SECONDS)))
                    .thenReturn(false);

            scheduler.checkPeriodicRoll();

            verify(distributedLock, never()).unlock(anyString(), anyString());
            verify(leaderboardConfigService, never()).findAll();
        }

        @Test
        @DisplayName("无排行榜时正常结束不报错")
        void noLeaderboards() {
            when(distributedLock.tryLock(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);
            when(leaderboardConfigService.findAll()).thenReturn(Collections.emptyList());

            scheduler.checkPeriodicRoll();

            verify(distributedLock).unlock(eq("scheduler:periodic-roll"), anyString());
        }

        @Test
        @DisplayName("跳过非 active 状态的排行榜")
        void skipNonActive() {
            when(distributedLock.tryLock(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("stopped");
            when(leaderboardConfigService.findAll()).thenReturn(List.of(lb));

            scheduler.checkPeriodicRoll();

            verify(instanceMapper, never()).findActiveByLeaderboardId(anyLong());
            verify(leaderboardConfigService, never()).roll(anyLong(), anyString());
            verify(leaderboardConfigService, never()).stop(anyLong(), anyString());
        }

        @Test
        @DisplayName("结束时间已到则自动终止")
        void autoStop() {
            when(distributedLock.tryLock(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            lb.setEndTime(1000L); // 已过期
            lb.setRollStrategy("none");
            when(leaderboardConfigService.findAll()).thenReturn(List.of(lb));

            scheduler.checkPeriodicRoll();

            verify(leaderboardConfigService).stop(1L, "scheduler");
        }

        @Test
        @DisplayName("自动终止失败时仅记录日志不中断循环")
        void autoStopFails() {
            when(distributedLock.tryLock(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            Leaderboard lb1 = new Leaderboard();
            lb1.setId(1L);
            lb1.setStatus("active");
            lb1.setEndTime(1000L);
            lb1.setRollStrategy("none");

            Leaderboard lb2 = new Leaderboard();
            lb2.setId(2L);
            lb2.setStatus("active");
            lb2.setEndTime(1000L);
            lb2.setRollStrategy("none");

            when(leaderboardConfigService.findAll()).thenReturn(List.of(lb1, lb2));
            doThrow(new RuntimeException("lock failed"))
                    .when(leaderboardConfigService).stop(1L, "scheduler");

            scheduler.checkPeriodicRoll();

            verify(leaderboardConfigService).stop(1L, "scheduler");
            verify(leaderboardConfigService).stop(2L, "scheduler");
        }

        @Test
        @DisplayName("非周期性滚动策略跳过")
        void skipNonPeriodicRoll() {
            when(distributedLock.tryLock(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            lb.setEndTime(null);
            lb.setRollStrategy("none");
            when(leaderboardConfigService.findAll()).thenReturn(List.of(lb));

            scheduler.checkPeriodicRoll();

            verify(instanceMapper, never()).findActiveByLeaderboardId(anyLong());
        }

        @Test
        @DisplayName("周期性滚动：无活跃实例则跳过")
        void periodicNoActiveInstance() {
            when(distributedLock.tryLock(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            lb.setEndTime(null);
            lb.setRollStrategy("periodic");
            lb.setRollIntervalValue(1);
            lb.setRollIntervalUnit("hour");
            when(leaderboardConfigService.findAll()).thenReturn(List.of(lb));
            when(instanceMapper.findActiveByLeaderboardId(1L)).thenReturn(null);

            scheduler.checkPeriodicRoll();

            verify(leaderboardConfigService, never()).roll(anyLong(), anyString());
        }

        @Test
        @DisplayName("周期性滚动：实例未到结束时间则跳过")
        void periodicNotYetExpired() {
            when(distributedLock.tryLock(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            lb.setEndTime(null);
            lb.setRollStrategy("periodic");
            lb.setRollIntervalValue(1);
            lb.setRollIntervalUnit("minute");

            LeaderboardInstance instance = new LeaderboardInstance();
            instance.setId(100L);
            instance.setInstanceSeq(1);
            instance.setStartTime(System.currentTimeMillis()); // 刚刚创建

            when(leaderboardConfigService.findAll()).thenReturn(List.of(lb));
            when(instanceMapper.findActiveByLeaderboardId(1L)).thenReturn(instance);

            scheduler.checkPeriodicRoll();

            verify(leaderboardConfigService, never()).roll(anyLong(), anyString());
        }

        @Test
        @DisplayName("周期性滚动：实例已到期则触发滚动")
        void periodicTriggerRoll() {
            when(distributedLock.tryLock(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            lb.setEndTime(null);
            lb.setRollStrategy("periodic");
            lb.setRollIntervalValue(1);
            lb.setRollIntervalUnit("minute");

            LeaderboardInstance instance = new LeaderboardInstance();
            instance.setId(100L);
            instance.setInstanceSeq(1);
            instance.setStartTime(System.currentTimeMillis() - 120_000L); // 2分钟前

            when(leaderboardConfigService.findAll()).thenReturn(List.of(lb));
            when(instanceMapper.findActiveByLeaderboardId(1L)).thenReturn(instance);

            scheduler.checkPeriodicRoll();

            verify(leaderboardConfigService).roll(1L, "scheduler");
        }

        @Test
        @DisplayName("周期性滚动失败时仅记录日志不中断循环")
        void periodicRollFails() {
            when(distributedLock.tryLock(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            Leaderboard lb1 = new Leaderboard();
            lb1.setId(1L);
            lb1.setStatus("active");
            lb1.setEndTime(null);
            lb1.setRollStrategy("periodic");
            lb1.setRollIntervalValue(1);
            lb1.setRollIntervalUnit("minute");

            Leaderboard lb2 = new Leaderboard();
            lb2.setId(2L);
            lb2.setStatus("active");
            lb2.setEndTime(null);
            lb2.setRollStrategy("periodic");
            lb2.setRollIntervalValue(1);
            lb2.setRollIntervalUnit("minute");

            LeaderboardInstance instance = new LeaderboardInstance();
            instance.setId(100L);
            instance.setInstanceSeq(1);
            instance.setStartTime(System.currentTimeMillis() - 120_000L);

            when(leaderboardConfigService.findAll()).thenReturn(List.of(lb1, lb2));
            when(instanceMapper.findActiveByLeaderboardId(1L)).thenReturn(instance);
            when(instanceMapper.findActiveByLeaderboardId(2L)).thenReturn(instance);
            doThrow(new RuntimeException("roll failed"))
                    .when(leaderboardConfigService).roll(1L, "scheduler");

            scheduler.checkPeriodicRoll();

            verify(leaderboardConfigService).roll(1L, "scheduler");
            verify(leaderboardConfigService).roll(2L, "scheduler");
        }

        @Test
        @DisplayName("分布式锁在 finally 中释放")
        void lockReleasedOnException() {
            when(distributedLock.tryLock(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);
            when(leaderboardConfigService.findAll()).thenThrow(new RuntimeException("db error"));

            try {
                scheduler.checkPeriodicRoll();
            } catch (RuntimeException ignored) {
            }

            verify(distributedLock).unlock(eq("scheduler:periodic-roll"), anyString());
        }
    }
}
