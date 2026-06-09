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

import java.util.ArrayList;
import java.util.Arrays;
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

        @Test
        @DisplayName("正常查询排名（降序）")
        void getRanking_descending() {
            LeaderboardMetric metric = new LeaderboardMetric();
            metric.setMetricId(10L);
            metric.setPriority(1);
            metric.setSortOrder("desc");
            when(leaderboardMetricMapper.findByLeaderboardId(1L)).thenReturn(new ArrayList<>(Arrays.asList(metric)));

            com.skada.mng.model.ScoreRecord r1 = new com.skada.mng.model.ScoreRecord();
            r1.setUserId("user1");
            r1.setMetricId(10L);
            r1.setScore(new java.math.BigDecimal("100"));
            r1.setPayload("{}");

            com.skada.mng.model.ScoreRecord r2 = new com.skada.mng.model.ScoreRecord();
            r2.setUserId("user2");
            r2.setMetricId(10L);
            r2.setScore(new java.math.BigDecimal("200"));
            r2.setPayload("{}");

            when(scoreRecordMapper.findByInstance(1L, 1L)).thenReturn(List.of(r1, r2));

            var result = configService.getRanking(1L, 1L, 0, 9);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUserId()).isEqualTo("user2"); // 200 > 100, desc排序
            assertThat(result.get(0).getRank()).isEqualTo(1);
            assertThat(result.get(1).getUserId()).isEqualTo("user1");
            assertThat(result.get(1).getRank()).isEqualTo(2);
        }

        @Test
        @DisplayName("正常查询排名（升序）")
        void getRanking_ascending() {
            LeaderboardMetric metric = new LeaderboardMetric();
            metric.setMetricId(10L);
            metric.setPriority(1);
            metric.setSortOrder("asc");
            when(leaderboardMetricMapper.findByLeaderboardId(1L)).thenReturn(new ArrayList<>(Arrays.asList(metric)));

            com.skada.mng.model.ScoreRecord r1 = new com.skada.mng.model.ScoreRecord();
            r1.setUserId("user1");
            r1.setMetricId(10L);
            r1.setScore(new java.math.BigDecimal("100"));

            com.skada.mng.model.ScoreRecord r2 = new com.skada.mng.model.ScoreRecord();
            r2.setUserId("user2");
            r2.setMetricId(10L);
            r2.setScore(new java.math.BigDecimal("50"));

            when(scoreRecordMapper.findByInstance(1L, 1L)).thenReturn(List.of(r1, r2));

            var result = configService.getRanking(1L, 1L, 0, 9);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUserId()).isEqualTo("user2"); // 50 < 100, asc排序
        }

        @Test
        @DisplayName("空记录时返回空列表")
        void getRanking_empty() {
            LeaderboardMetric metric = new LeaderboardMetric();
            metric.setMetricId(10L);
            metric.setPriority(1);
            metric.setSortOrder("desc");
            when(leaderboardMetricMapper.findByLeaderboardId(1L)).thenReturn(new ArrayList<>(Arrays.asList(metric)));
            when(scoreRecordMapper.findByInstance(1L, 1L)).thenReturn(List.of());

            var result = configService.getRanking(1L, 1L, 0, 9);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByTenantId")
    class FindByTenantId {

        @Test
        @DisplayName("查询租户下的排行榜")
        void findByTenantId_success() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            when(leaderboardMapper.findByTenantId(TENANT_ID)).thenReturn(List.of(lb));

            var result = configService.findByTenantId(TENANT_ID);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("查询所有排行榜")
        void findAll_success() {
            when(leaderboardMapper.findAll()).thenReturn(List.of());

            var result = configService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getInstances")
    class GetInstances {

        @Test
        @DisplayName("查询排行榜的所有实例")
        void getInstances_success() {
            LeaderboardInstance inst = new LeaderboardInstance();
            inst.setId(1L);
            when(instanceMapper.findByLeaderboardId(1L)).thenReturn(List.of(inst));

            var result = configService.getInstances(1L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("validateCreateRequest 边缘情况")
    class ValidateCreateRequestEdgeCases {

        @Test
        @DisplayName("租户ID为空字符串时抛出异常")
        void create_blankTenantId_throws() {
            LeaderboardCreateRequest req = validRequest();
            req.setTenantId("  ");
            when(tenantService.findByTenantId(any())).thenReturn(new Tenant());

            assertThatThrownBy(() -> configService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("租户ID不能为空");
        }

        @Test
        @DisplayName("排行榜名称为空时抛出异常")
        void create_blankName_throws() {
            LeaderboardCreateRequest req = validRequest();
            req.setName("");
            when(tenantService.findByTenantId(TENANT_ID)).thenReturn(new Tenant());

            assertThatThrownBy(() -> configService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("排行榜名称不能为空");
        }

        @Test
        @DisplayName("开始时间为null时抛出异常")
        void create_nullStartTime_throws() {
            LeaderboardCreateRequest req = validRequest();
            req.setStartTime(null);
            when(tenantService.findByTenantId(TENANT_ID)).thenReturn(new Tenant());

            assertThatThrownBy(() -> configService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("开始时间不能为空");
        }

        @Test
        @DisplayName("周期性滚动时间单位无效时抛出异常")
        void create_invalidRollUnit_throws() {
            LeaderboardCreateRequest req = validRequest();
            req.setRollStrategy("periodic");
            req.setRollIntervalValue(1);
            req.setRollIntervalUnit("second");
            when(tenantService.findByTenantId(TENANT_ID)).thenReturn(new Tenant());

            assertThatThrownBy(() -> configService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("时间单位必须为 minute/hour/day");
        }
    }

    @Nested
    @DisplayName("roll/stop 补充")
    class RollStopAdditional {

        @Test
        @DisplayName("roll 获取锁后发现无活跃实例抛出异常")
        void roll_noActiveInstanceAfterLock_throws() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            when(leaderboardMapper.findById(1L)).thenReturn(lb);
            when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS)))
                    .thenReturn(true);
            when(instanceMapper.findActiveByLeaderboardId(1L)).thenReturn(null);

            assertThatThrownBy(() -> configService.roll(1L, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("没有活跃实例");
            verify(distributedLock).unlock(anyString(), anyString());
        }

        @Test
        @DisplayName("stop 获取分布式锁失败时抛出异常")
        void stop_lockFailed_throws() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            when(leaderboardMapper.findById(1L)).thenReturn(lb);
            when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS)))
                    .thenReturn(false);

            assertThatThrownBy(() -> configService.stop(1L, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("操作进行中");
        }

        @Test
        @DisplayName("stop 有活跃实例时关闭该实例")
        void stop_withActiveInstance_closesIt() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setStatus("active");
            when(leaderboardMapper.findById(1L)).thenReturn(lb);
            when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS)))
                    .thenReturn(true);

            LeaderboardInstance active = new LeaderboardInstance();
            active.setId(100L);
            when(instanceMapper.findActiveByLeaderboardId(1L)).thenReturn(active);

            configService.stop(1L, ADMIN_ID);

            verify(instanceMapper).closeInstance(eq(100L), anyLong());
            assertThat(lb.getStatus()).isEqualTo("stopped");
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
