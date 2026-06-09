package com.skada.mng.service;

import com.skada.common.exception.BusinessException;
import com.skada.common.model.PageResult;
import com.skada.common.util.DistributedLock;
import com.skada.mng.mapper.LeaderboardInstanceMapper;
import com.skada.mng.mapper.LeaderboardMapper;
import com.skada.mng.mapper.LeaderboardMetricMapper;
import com.skada.mng.mapper.ScoreRecordMapper;
import com.skada.mng.model.Leaderboard;
import com.skada.mng.model.LeaderboardInstance;
import com.skada.mng.model.LeaderboardMetric;
import com.skada.mng.model.Tenant;
import com.skada.mng.model.request.LeaderboardCreateRequest;
import com.skada.mng.model.request.LeaderboardUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardConfigService 单元测试")
class LeaderboardConfigServiceTest {

    @Mock private LeaderboardMapper leaderboardMapper;
    @Mock private LeaderboardInstanceMapper instanceMapper;
    @Mock private LeaderboardMetricMapper leaderboardMetricMapper;
    @Mock private ScoreRecordMapper scoreRecordMapper;
    @Mock private TenantService tenantService;
    @Mock private DistributedLock distributedLock;

    private LeaderboardConfigService configService;

    private static final String ADMIN_ID = "ad_00000001";
    private static final String TENANT_ID = "tn_abc12345";

    @BeforeEach
    void setUp() {
        configService = new LeaderboardConfigService(leaderboardMapper, instanceMapper,
                leaderboardMetricMapper, scoreRecordMapper, tenantService, distributedLock);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("成功创建排行榜及首个实例")
        void create_success() {
            when(tenantService.findByTenantId(TENANT_ID)).thenReturn(new Tenant());

            LeaderboardCreateRequest req = validRequest();
            configService.create(req, ADMIN_ID);

            // 验证Leaderboard插入
            ArgumentCaptor<Leaderboard> lbCaptor = ArgumentCaptor.forClass(Leaderboard.class);
            verify(leaderboardMapper).insert(lbCaptor.capture());
            Leaderboard lb = lbCaptor.getValue();
            assertThat(lb.getPlanId()).startsWith("lb_");
            assertThat(lb.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(lb.getStatus()).isEqualTo("active");

            // 验证实例插入
            ArgumentCaptor<LeaderboardInstance> instCaptor = ArgumentCaptor.forClass(LeaderboardInstance.class);
            verify(instanceMapper).insert(instCaptor.capture());
            assertThat(instCaptor.getValue().getInstanceId()).startsWith("li_");

            // 验证指标关联
            verify(leaderboardMetricMapper).insertBatch(anyList());
        }

        @Test
        @DisplayName("租户不存在时抛出异常")
        void create_tenantNotFound_throws() {
            when(tenantService.findByTenantId(TENANT_ID)).thenReturn(null);

            LeaderboardCreateRequest req = validRequest();
            assertThatThrownBy(() -> configService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("租户不存在");
        }

        @Test
        @DisplayName("未关联指标时抛出异常")
        void create_noMetrics_throws() {
            when(tenantService.findByTenantId(TENANT_ID)).thenReturn(new Tenant());

            LeaderboardCreateRequest req = validRequest();
            req.setMetrics(null);

            assertThatThrownBy(() -> configService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("至少需要关联一个指标");
        }

        @Test
        @DisplayName("周期性滚动未指定间隔值时抛出异常")
        void create_periodicMissingInterval_throws() {
            when(tenantService.findByTenantId(TENANT_ID)).thenReturn(new Tenant());

            LeaderboardCreateRequest req = validRequest();
            req.setRollStrategy("periodic");
            req.setRollIntervalValue(null);

            assertThatThrownBy(() -> configService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("间隔值");
        }

        @Test
        @DisplayName("按用户数滚动未指定阈值时抛出异常")
        void create_userCountMissingThreshold_throws() {
            when(tenantService.findByTenantId(TENANT_ID)).thenReturn(new Tenant());

            LeaderboardCreateRequest req = validRequest();
            req.setRollStrategy("user_count");
            req.setRollUserCount(null);

            assertThatThrownBy(() -> configService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户数阈值");
        }

        @Test
        @DisplayName("结束时间早于开始时间时抛出异常")
        void create_endBeforeStart_throws() {
            when(tenantService.findByTenantId(TENANT_ID)).thenReturn(new Tenant());

            LeaderboardCreateRequest req = validRequest();
            req.setEndTime(1000L);
            req.setStartTime(2000L);

            assertThatThrownBy(() -> configService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("结束时间必须晚于开始时间");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("成功更新排行榜配置")
        void update_success() {
            Leaderboard existing = new Leaderboard();
            existing.setId(1L);
            existing.setName("旧名称");
            when(leaderboardMapper.findById(1L)).thenReturn(existing);

            LeaderboardUpdateRequest req = new LeaderboardUpdateRequest();
            req.setId(1L);
            req.setName("新名称");
            req.setMaxQueryUsers(500);

            configService.update(req, ADMIN_ID);

            verify(leaderboardMapper).update(existing);
            assertThat(existing.getName()).isEqualTo("新名称");
            assertThat(existing.getMaxQueryUsers()).isEqualTo(500);
        }

        @Test
        @DisplayName("排行榜不存在时抛出异常")
        void update_notFound_throws() {
            when(leaderboardMapper.findById(99L)).thenReturn(null);

            LeaderboardUpdateRequest req = new LeaderboardUpdateRequest();
            req.setId(99L);

            assertThatThrownBy(() -> configService.update(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("排行榜不存在");
        }

        @Test
        @DisplayName("更新指标关联时先删后插")
        void update_metricsDeleteAndInsert() {
            Leaderboard existing = new Leaderboard();
            existing.setId(1L);
            when(leaderboardMapper.findById(1L)).thenReturn(existing);

            LeaderboardUpdateRequest req = new LeaderboardUpdateRequest();
            req.setId(1L);
            LeaderboardUpdateRequest.MetricAssociation ma = new LeaderboardUpdateRequest.MetricAssociation();
            ma.setMetricId(1L);
            ma.setPriority(1);
            ma.setSortOrder("desc");
            req.setMetrics(List.of(ma));

            configService.update(req, ADMIN_ID);

            verify(leaderboardMetricMapper).deleteByLeaderboardId(1L);
            verify(leaderboardMetricMapper).insertBatch(anyList());
        }
    }

    @Nested
    @DisplayName("roll")
    class Roll {

        @Test
        @DisplayName("成功手动滚动")
        void roll_success() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            when(leaderboardMapper.findById(1L)).thenReturn(lb);
            when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(true);
            LeaderboardInstance active = new LeaderboardInstance();
            active.setId(100L);
            when(instanceMapper.findActiveByLeaderboardId(1L)).thenReturn(active);
            when(instanceMapper.getMaxInstanceSeq(1L)).thenReturn(1);

            configService.roll(1L, ADMIN_ID);

            verify(instanceMapper).closeInstance(eq(100L), anyLong());
            verify(instanceMapper).insert(any());
            verify(distributedLock).unlock(anyString(), anyString());
        }

        @Test
        @DisplayName("排行榜不存在时抛出异常")
        void roll_notFound_throws() {
            when(leaderboardMapper.findById(99L)).thenReturn(null);

            assertThatThrownBy(() -> configService.roll(99L, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("排行榜不存在");
        }

        @Test
        @DisplayName("排行榜已终止时抛出异常")
        void roll_alreadyStopped_throws() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("stopped");
            when(leaderboardMapper.findById(1L)).thenReturn(lb);

            assertThatThrownBy(() -> configService.roll(1L, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已终止");
        }

        @Test
        @DisplayName("获取分布式锁失败时抛出异常")
        void roll_lockFailed_throws() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            when(leaderboardMapper.findById(1L)).thenReturn(lb);
            when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(false);

            assertThatThrownBy(() -> configService.roll(1L, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("滚动中");
        }
    }

    @Nested
    @DisplayName("stop")
    class Stop {

        @Test
        @DisplayName("成功终止排行榜")
        void stop_success() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            when(leaderboardMapper.findById(1L)).thenReturn(lb);
            when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(true);

            configService.stop(1L, ADMIN_ID);

            assertThat(lb.getStatus()).isEqualTo("stopped");
            verify(leaderboardMapper).update(lb);
            verify(distributedLock).unlock(anyString(), anyString());
        }

        @Test
        @DisplayName("排行榜不存在时抛出异常")
        void stop_notFound_throws() {
            when(leaderboardMapper.findById(99L)).thenReturn(null);

            assertThatThrownBy(() -> configService.stop(99L, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("排行榜不存在");
        }

        @Test
        @DisplayName("排行榜已终止时抛出异常")
        void stop_alreadyStopped_throws() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("stopped");
            when(leaderboardMapper.findById(1L)).thenReturn(lb);

            assertThatThrownBy(() -> configService.stop(1L, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已经处于终止状态");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("排行榜存在时返回")
        void findById_success() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setName("测试榜");
            when(leaderboardMapper.findById(1L)).thenReturn(lb);

            Leaderboard result = configService.findById(1L);

            assertThat(result.getName()).isEqualTo("测试榜");
        }

        @Test
        @DisplayName("排行榜不存在时抛出异常")
        void findById_notFound_throws() {
            when(leaderboardMapper.findById(99L)).thenReturn(null);

            assertThatThrownBy(() -> configService.findById(99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("排行榜不存在");
        }
    }

    @Nested
    @DisplayName("findAllWithPage")
    class FindWithPage {

        @Test
        @DisplayName("按租户筛选分页")
        void findAllWithPage_byTenant() {
            when(leaderboardMapper.findByTenantIdWithPage(TENANT_ID, 0, 20)).thenReturn(List.of());
            when(leaderboardMapper.countByTenantId(TENANT_ID)).thenReturn(0L);

            PageResult<Leaderboard> result = configService.findAllWithPage(1, 20, TENANT_ID);

            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("不限租户的全量分页")
        void findAllWithPage_all() {
            when(leaderboardMapper.findAllWithPage(0, 20)).thenReturn(List.of());
            when(leaderboardMapper.count()).thenReturn(0L);

            PageResult<Leaderboard> result = configService.findAllWithPage(1, 20, null);

            assertThat(result.getPage()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getRanking")
    class GetRanking {

        @Test
        @DisplayName("分页参数无效时抛出异常")
        void getRanking_invalidParams_throws() {
            assertThatThrownBy(() -> configService.getRanking(1L, 1L, 10, 5))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("分页参数无效");
        }

        @Test
        @DisplayName("排行榜未关联指标时抛出异常")
        void getRanking_noMetrics_throws() {
            when(leaderboardMetricMapper.findByLeaderboardId(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> configService.getRanking(1L, 1L, 0, 9))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未关联指标");
        }
    }

    // ===== helpers =====

    private LeaderboardCreateRequest validRequest() {
        LeaderboardCreateRequest req = new LeaderboardCreateRequest();
        req.setTenantId(TENANT_ID);
        req.setName("测试排行榜");
        req.setStartTime(1000000L);
        req.setMaxQueryUsers(1000);
        req.setRollStrategy("none");
        req.setMetrics(List.of(metricAssoc(1L, 1, "desc")));
        return req;
    }

    private LeaderboardCreateRequest.MetricAssociation metricAssoc(Long metricId, int priority, String sortOrder) {
        LeaderboardCreateRequest.MetricAssociation ma = new LeaderboardCreateRequest.MetricAssociation();
        ma.setMetricId(metricId);
        ma.setPriority(priority);
        ma.setSortOrder(sortOrder);
        return ma;
    }
}
