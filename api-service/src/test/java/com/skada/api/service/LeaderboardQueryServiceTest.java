package com.skada.api.service;

import com.skada.api.mapper.*;
import com.skada.api.model.*;
import com.skada.common.enums.BizCode;
import com.skada.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardQueryService 单元测试")
class LeaderboardQueryServiceTest {

    @Mock private LeaderboardMapper leaderboardMapper;
    @Mock private LeaderboardInstanceMapper instanceMapper;
    @Mock private LeaderboardMetricMapper leaderboardMetricMapper;
    @Mock private MetricMapper metricMapper;
    @Mock private ScoreRecordMapper scoreRecordMapper;
    @Mock private TenantMapper tenantMapper;
    @Mock private StringRedisTemplate redisTemplate;

    private LeaderboardQueryService queryService;

    private static final String PLAN_ID = "plan-001";
    private static final String TENANT_ID = "tenant-001";
    private static final Long LEADERBOARD_ID = 100L;
    private static final Long INSTANCE_ID = 200L;
    private static final String INSTANCE_EXT_ID = "li-001";

    @BeforeEach
    void setUp() {
        queryService = new LeaderboardQueryService(leaderboardMapper, instanceMapper,
                leaderboardMetricMapper, metricMapper, scoreRecordMapper,
                tenantMapper, redisTemplate);
    }

    /** 构造基础排行榜计划 */
    private Leaderboard buildLeaderboard() {
        Leaderboard lb = new Leaderboard();
        lb.setId(LEADERBOARD_ID);
        lb.setPlanId(PLAN_ID);
        lb.setTenantId(TENANT_ID);
        lb.setMaxQueryUsers(1000);
        return lb;
    }

    /** Mock Redis ZSet 操作 */
    @SuppressWarnings("unchecked")
    private ZSetOperations<String, String> mockZSetOps() {
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        return zSetOps;
    }

    /** 构造租户和排行榜指标 */
    private void mockPlanAndTenant(Leaderboard lb) {
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        Tenant tenant = new Tenant();
        tenant.setTenantId(TENANT_ID);
        tenant.setStatus(1);
        tenant.setAllowAnonymousQuery(1);
        when(tenantMapper.findByTenantId(TENANT_ID)).thenReturn(tenant);
    }

    private void mockInstance() {
        LeaderboardInstance inst = new LeaderboardInstance();
        inst.setId(INSTANCE_ID);
        inst.setInstanceId(INSTANCE_EXT_ID);
        inst.setLeaderboardId(LEADERBOARD_ID);
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(inst);
    }

    private List<LeaderboardMetric> buildMetrics(Long... metricIds) {
        List<LeaderboardMetric> list = new ArrayList<>();
        int priority = 0;
        for (Long mid : metricIds) {
            LeaderboardMetric lm = new LeaderboardMetric();
            lm.setLeaderboardId(LEADERBOARD_ID);
            lm.setMetricId(mid);
            lm.setPriority(priority++);
            lm.setSortOrder("desc");
            list.add(lm);
        }
        return list;
    }

    // ==================== getRanking 参数校验 ====================

    @Test
    @DisplayName("getRanking: from > to 时抛出异常")
    void getRanking_invalidRange() {
        assertThatThrownBy(() -> queryService.getRanking(PLAN_ID, null, 10, 5, TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分页参数无效");
    }

    @Test
    @DisplayName("getRanking: from 负数时抛出异常")
    void getRanking_negativeFrom() {
        assertThatThrownBy(() -> queryService.getRanking(PLAN_ID, null, -1, 5, TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分页参数无效");
    }

    @Test
    @DisplayName("getRanking: 排行榜计划不存在时抛出异常")
    void getRanking_planNotFound() {
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(null);

        assertThatThrownBy(() -> queryService.getRanking(PLAN_ID, null, 0, 9, TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("排行榜计划不存在");
    }

    @Test
    @DisplayName("getRanking: 租户不存在或已停用时抛出异常")
    void getRanking_tenantDisabled() {
        Leaderboard lb = buildLeaderboard();
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);
        when(tenantMapper.findByTenantId(TENANT_ID)).thenReturn(null);

        assertThatThrownBy(() -> queryService.getRanking(PLAN_ID, null, 0, 9, TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("租户不存在或已停用");
    }

    @Test
    @DisplayName("getRanking: 不允许匿名查询且未提供租户凭证时抛出异常")
    void getRanking_anonymousDenied() {
        Leaderboard lb = buildLeaderboard();
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        Tenant tenant = new Tenant();
        tenant.setTenantId(TENANT_ID);
        tenant.setStatus(1);
        tenant.setAllowAnonymousQuery(0);
        when(tenantMapper.findByTenantId(TENANT_ID)).thenReturn(tenant);

        assertThatThrownBy(() -> queryService.getRanking(PLAN_ID, null, 0, 9, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("租户凭证无效或无权查询");
    }

    @Test
    @DisplayName("getRanking: 指定的实例不存在时抛出异常")
    void getRanking_instanceNotFound() {
        Leaderboard lb = buildLeaderboard();
        mockPlanAndTenant(lb);
        when(instanceMapper.findByInstanceId("bad-instance")).thenReturn(null);

        assertThatThrownBy(() -> queryService.getRanking(PLAN_ID, "bad-instance", 0, 9, TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实例不存在");
    }

    @Test
    @DisplayName("getRanking: 无活跃实例时抛出异常")
    void getRanking_noActiveInstance() {
        Leaderboard lb = buildLeaderboard();
        mockPlanAndTenant(lb);
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(null);

        assertThatThrownBy(() -> queryService.getRanking(PLAN_ID, null, 0, 9, TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有活跃实例");
    }

    @Test
    @DisplayName("getRanking: 未关联指标时抛出异常")
    void getRanking_noMetrics() {
        Leaderboard lb = buildLeaderboard();
        mockPlanAndTenant(lb);
        mockInstance();
        when(leaderboardMetricMapper.findByLeaderboardId(LEADERBOARD_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> queryService.getRanking(PLAN_ID, null, 0, 9, TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未关联指标");
    }

    // ==================== getRanking Redis命中 ====================

    @Test
    @DisplayName("getRanking: Redis命中返回排名数据")
    @SuppressWarnings("unchecked")
    void getRanking_redisHit() {
        Leaderboard lb = buildLeaderboard();
        mockPlanAndTenant(lb);
        mockInstance();

        List<LeaderboardMetric> lbMetrics = buildMetrics(10L, 20L);
        when(leaderboardMetricMapper.findByLeaderboardId(LEADERBOARD_ID)).thenReturn(lbMetrics);

        List<Metric> metricList = new ArrayList<>();
        Metric m1 = new Metric(); m1.setId(10L); m1.setMetricId("ext-10"); m1.setName("指标1");
        Metric m2 = new Metric(); m2.setId(20L); m2.setMetricId("ext-20"); m2.setName("指标2");
        metricList.add(m1); metricList.add(m2);
        when(metricMapper.findByIds(List.of(10L, 20L))).thenReturn(metricList);

        ZSetOperations<String, String> zSetOps = mockZSetOps();
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn("user-001");
        when(tuple.getScore()).thenReturn(100.0);
        Set<ZSetOperations.TypedTuple<String>> redisResult = new LinkedHashSet<>();
        redisResult.add(tuple);
        when(zSetOps.reverseRangeWithScores(anyString(), eq(0L), eq(9L))).thenReturn(redisResult);

        // 次要指标
        when(zSetOps.score(anyString(), eq("user-001"))).thenReturn(50.0);

        // payload
        when(scoreRecordMapper.findPayloadsByUsers(eq(LEADERBOARD_ID), eq(INSTANCE_ID), anyList()))
                .thenReturn(List.of());

        List<LeaderboardQueryService.RankEntry> result =
                queryService.getRanking(PLAN_ID, null, 0, 9, TENANT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user-001");
        assertThat(result.get(0).getRank()).isEqualTo(1);
    }

    // ==================== getRanking MySQL兜底 ====================

    @Test
    @DisplayName("getRanking: Redis无数据时从MySQL兜底")
    @SuppressWarnings("unchecked")
    void getRanking_mysqlFallback() {
        Leaderboard lb = buildLeaderboard();
        mockPlanAndTenant(lb);
        mockInstance();

        List<LeaderboardMetric> lbMetrics = buildMetrics(10L);
        when(leaderboardMetricMapper.findByLeaderboardId(LEADERBOARD_ID)).thenReturn(lbMetrics);

        Metric m = new Metric(); m.setId(10L); m.setMetricId("ext-10"); m.setName("指标1");
        when(metricMapper.findByIds(List.of(10L))).thenReturn(List.of(m));

        ZSetOperations<String, String> zSetOps = mockZSetOps();
        when(zSetOps.reverseRangeWithScores(anyString(), eq(0L), eq(9L))).thenReturn(Set.of());

        ScoreRecord sr = new ScoreRecord();
        sr.setUserId("user-001");
        sr.setMetricId(10L);
        sr.setScore(BigDecimal.valueOf(100));
        when(scoreRecordMapper.findRanking(eq(LEADERBOARD_ID), eq(INSTANCE_ID), eq(10L),
                eq("DESC"), eq(0), eq(10))).thenReturn(List.of(sr));

        List<LeaderboardQueryService.RankEntry> result =
                queryService.getRanking(PLAN_ID, null, 0, 9, TENANT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user-001");
        assertThat(result.get(0).getRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("getRanking: 按指定instanceId查询")
    @SuppressWarnings("unchecked")
    void getRanking_byInstanceId() {
        Leaderboard lb = buildLeaderboard();
        mockPlanAndTenant(lb);

        LeaderboardInstance inst = new LeaderboardInstance();
        inst.setId(INSTANCE_ID);
        inst.setInstanceId(INSTANCE_EXT_ID);
        inst.setLeaderboardId(LEADERBOARD_ID);
        when(instanceMapper.findByInstanceId(INSTANCE_EXT_ID)).thenReturn(inst);

        List<LeaderboardMetric> lbMetrics = buildMetrics(10L);
        when(leaderboardMetricMapper.findByLeaderboardId(LEADERBOARD_ID)).thenReturn(lbMetrics);

        Metric m = new Metric(); m.setId(10L); m.setMetricId("ext-10"); m.setName("指标1");
        when(metricMapper.findByIds(List.of(10L))).thenReturn(List.of(m));

        ZSetOperations<String, String> zSetOps = mockZSetOps();
        when(zSetOps.reverseRangeWithScores(anyString(), eq(0L), eq(9L))).thenReturn(Set.of());

        ScoreRecord sr = new ScoreRecord();
        sr.setUserId("user-001"); sr.setMetricId(10L); sr.setScore(BigDecimal.valueOf(100));
        when(scoreRecordMapper.findRanking(anyLong(), anyLong(), anyLong(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(sr));

        List<LeaderboardQueryService.RankEntry> result =
                queryService.getRanking(PLAN_ID, INSTANCE_EXT_ID, 0, 9, TENANT_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getRanking: from超出maxQueryUsers时返回空列表")
    void getRanking_beyondMaxQuery() {
        Leaderboard lb = buildLeaderboard();
        lb.setMaxQueryUsers(10);
        mockPlanAndTenant(lb);
        mockInstance();

        List<LeaderboardMetric> lbMetrics = buildMetrics(10L);
        when(leaderboardMetricMapper.findByLeaderboardId(LEADERBOARD_ID)).thenReturn(lbMetrics);

        Metric m = new Metric(); m.setId(10L); m.setMetricId("ext-10"); m.setName("指标1");
        when(metricMapper.findByIds(List.of(10L))).thenReturn(List.of(m));

        List<LeaderboardQueryService.RankEntry> result =
                queryService.getRanking(PLAN_ID, null, 10, 19, TENANT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRanking: asc排序")
    @SuppressWarnings("unchecked")
    void getRanking_ascSort() {
        Leaderboard lb = buildLeaderboard();
        mockPlanAndTenant(lb);
        mockInstance();

        List<LeaderboardMetric> lbMetrics = buildMetrics(10L);
        lbMetrics.get(0).setSortOrder("asc");
        when(leaderboardMetricMapper.findByLeaderboardId(LEADERBOARD_ID)).thenReturn(lbMetrics);

        Metric m = new Metric(); m.setId(10L); m.setMetricId("ext-10"); m.setName("指标1");
        when(metricMapper.findByIds(List.of(10L))).thenReturn(List.of(m));

        ZSetOperations<String, String> zSetOps = mockZSetOps();
        when(zSetOps.rangeWithScores(anyString(), eq(0L), eq(9L))).thenReturn(Set.of());

        ScoreRecord sr = new ScoreRecord();
        sr.setUserId("user-001"); sr.setMetricId(10L); sr.setScore(BigDecimal.valueOf(100));
        when(scoreRecordMapper.findRanking(eq(LEADERBOARD_ID), eq(INSTANCE_ID), eq(10L),
                eq("ASC"), eq(0), eq(10))).thenReturn(List.of(sr));

        List<LeaderboardQueryService.RankEntry> result =
                queryService.getRanking(PLAN_ID, null, 0, 9, TENANT_ID);

        assertThat(result).hasSize(1);
    }

    // ==================== getInstances ====================

    @Test
    @DisplayName("getInstances: 成功返回实例列表")
    void getInstances_success() {
        Leaderboard lb = buildLeaderboard();
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        LeaderboardInstance inst1 = new LeaderboardInstance();
        inst1.setId(1L); inst1.setInstanceId("li-001");
        LeaderboardInstance inst2 = new LeaderboardInstance();
        inst2.setId(2L); inst2.setInstanceId("li-002");
        when(instanceMapper.findByLeaderboardId(LEADERBOARD_ID)).thenReturn(List.of(inst1, inst2));

        List<LeaderboardInstance> result = queryService.getInstances(PLAN_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getInstances: 排行榜不存在时抛出异常")
    void getInstances_planNotFound() {
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(null);

        assertThatThrownBy(() -> queryService.getInstances(PLAN_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("排行榜计划不存在");
    }

    // ==================== RankEntry / MetricValueEntry ====================

    @Test
    @DisplayName("RankEntry: getter/setter 正常工作")
    void rankEntry_getterSetter() {
        LeaderboardQueryService.RankEntry entry = new LeaderboardQueryService.RankEntry();
        entry.setRank(1);
        entry.setUserId("user-001");
        entry.setMetricValues(List.of());

        assertThat(entry.getRank()).isEqualTo(1);
        assertThat(entry.getUserId()).isEqualTo("user-001");
        assertThat(entry.getMetricValues()).isEmpty();
    }

    @Test
    @DisplayName("MetricValueEntry: getter/setter 正常工作")
    void metricValueEntry_getterSetter() {
        LeaderboardQueryService.MetricValueEntry entry = new LeaderboardQueryService.MetricValueEntry();
        entry.setMetricId("m1");
        entry.setMetricName("指标1");
        entry.setValue(BigDecimal.TEN);
        entry.setPayload("payload");

        assertThat(entry.getMetricId()).isEqualTo("m1");
        assertThat(entry.getMetricName()).isEqualTo("指标1");
        assertThat(entry.getValue()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(entry.getPayload()).isEqualTo("payload");
    }
}
