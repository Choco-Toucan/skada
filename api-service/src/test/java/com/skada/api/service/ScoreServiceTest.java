package com.skada.api.service;

import com.skada.api.mapper.*;
import com.skada.api.model.*;
import com.skada.common.exception.BusinessException;
import com.skada.common.util.DistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreService 单元测试")
class ScoreServiceTest {

    @Mock private LeaderboardMapper leaderboardMapper;
    @Mock private LeaderboardInstanceMapper instanceMapper;
    @Mock private LeaderboardMetricMapper leaderboardMetricMapper;
    @Mock private MetricMapper metricMapper;
    @Mock private ScoreRecordMapper scoreRecordMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private DistributedLock distributedLock;

    private ScoreService scoreService;

    private static final String TENANT_ID = "tenant-001";
    private static final String USER_ID = "user-001";
    private static final String METRIC_EXT_ID = "metric-001";
    private static final Long METRIC_INTERNAL_ID = 10L;
    private static final Long LEADERBOARD_ID = 100L;
    private static final Long INSTANCE_ID = 200L;

    @BeforeEach
    void setUp() {
        scoreService = new ScoreService(leaderboardMapper, instanceMapper,
                leaderboardMetricMapper, metricMapper, scoreRecordMapper,
                redisTemplate, distributedLock);
    }

    /** 构造一个基础可用的 MetricValue */
    private ScoreService.MetricValue metricValue(String metricId, double value) {
        ScoreService.MetricValue mv = new ScoreService.MetricValue();
        mv.setMetricId(metricId);
        mv.setValue(BigDecimal.valueOf(value));
        return mv;
    }

    /** Mock 指标解析成功 */
    private void mockMetricResolve() {
        Metric m = new Metric();
        m.setId(METRIC_INTERNAL_ID);
        m.setMetricId(METRIC_EXT_ID);
        when(metricMapper.findByMetricId(METRIC_EXT_ID)).thenReturn(m);
    }

    /** Mock 排行榜计划匹配成功 */
    private Leaderboard mockLeaderboardMatch() {
        LeaderboardMetric lm = new LeaderboardMetric();
        lm.setLeaderboardId(LEADERBOARD_ID);
        lm.setMetricId(METRIC_INTERNAL_ID);
        when(leaderboardMetricMapper.findByMetricIds(anyList())).thenReturn(List.of(lm));

        Leaderboard lb = new Leaderboard();
        lb.setId(LEADERBOARD_ID);
        lb.setTenantId(TENANT_ID);
        lb.setStatus("active");
        lb.setStartTime(System.currentTimeMillis() - 60_000);
        lb.setEndTime(null);
        lb.setAllowDuplicateReport(1);
        when(leaderboardMapper.findById(LEADERBOARD_ID)).thenReturn(lb);

        LeaderboardInstance instance = new LeaderboardInstance();
        instance.setId(INSTANCE_ID);
        instance.setInstanceId("li-001");
        instance.setLeaderboardId(LEADERBOARD_ID);
        instance.setInstanceSeq(1);
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(instance);

        when(leaderboardMetricMapper.findByLeaderboardId(LEADERBOARD_ID)).thenReturn(List.of(lm));

        return lb;
    }

    /** Mock Redis ZSet 和 HyperLogLog 操作 */
    @SuppressWarnings("unchecked")
    private void mockRedisOps() {
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        HyperLogLogOperations<String, String> hllOps = mock(HyperLogLogOperations.class);

        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(redisTemplate.opsForHyperLogLog()).thenReturn(hllOps);
    }

    // ==================== submit 成功场景 ====================

    @Test
    @DisplayName("submit: 成功上报单条分数")
    void submit_success() {
        mockMetricResolve();
        mockLeaderboardMatch();
        mockRedisOps();

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatCode(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .doesNotThrowAnyException();

        verify(scoreRecordMapper).insertBatch(anyList());
    }

    @Test
    @DisplayName("submit: 成功上报带payload的分数")
    void submit_withPayload() {
        mockMetricResolve();
        mockLeaderboardMatch();
        mockRedisOps();

        ScoreService.MetricValue mv = new ScoreService.MetricValue();
        mv.setMetricId(METRIC_EXT_ID);
        mv.setValue(BigDecimal.valueOf(200.0));
        mv.setPayload("{\"extra\":\"data\"}");

        assertThatCode(() -> scoreService.submit(TENANT_ID, USER_ID, List.of(mv)))
                .doesNotThrowAnyException();
    }

    // ==================== submit 失败场景 ====================

    @Test
    @DisplayName("submit: 指标不存在时抛出异常")
    void submit_metricNotFound() {
        when(metricMapper.findByMetricId(METRIC_EXT_ID)).thenReturn(null);

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("指标不存在");
    }

    @Test
    @DisplayName("submit: 指标未关联任何活跃排行榜时抛出异常")
    void submit_noMatchingPlan() {
        mockMetricResolve();
        when(leaderboardMetricMapper.findByMetricIds(anyList())).thenReturn(List.of());

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未关联到任何活跃的排行榜计划");
    }

    @Test
    @DisplayName("submit: 指标关联多个排行榜计划时抛出异常")
    void submit_multipleMatchingPlans() {
        mockMetricResolve();

        LeaderboardMetric lm1 = new LeaderboardMetric();
        lm1.setLeaderboardId(100L);
        lm1.setMetricId(METRIC_INTERNAL_ID);
        LeaderboardMetric lm2 = new LeaderboardMetric();
        lm2.setLeaderboardId(200L);
        lm2.setMetricId(METRIC_INTERNAL_ID);
        when(leaderboardMetricMapper.findByMetricIds(anyList())).thenReturn(List.of(lm1, lm2));

        Leaderboard lb1 = new Leaderboard();
        lb1.setId(100L); lb1.setTenantId(TENANT_ID); lb1.setStatus("active");
        Leaderboard lb2 = new Leaderboard();
        lb2.setId(200L); lb2.setTenantId(TENANT_ID); lb2.setStatus("active");
        when(leaderboardMapper.findById(100L)).thenReturn(lb1);
        when(leaderboardMapper.findById(200L)).thenReturn(lb2);

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("关联了多个排行榜计划");
    }

    @Test
    @DisplayName("submit: 排行榜计划不属于该租户时抛出异常")
    void submit_wrongTenant() {
        mockMetricResolve();
        LeaderboardMetric lm = new LeaderboardMetric();
        lm.setLeaderboardId(LEADERBOARD_ID);
        lm.setMetricId(METRIC_INTERNAL_ID);
        when(leaderboardMetricMapper.findByMetricIds(anyList())).thenReturn(List.of(lm));

        // resolveLeaderboardIdByMetricIds 返回匹配的租户
        Leaderboard lb1 = new Leaderboard();
        lb1.setId(LEADERBOARD_ID);
        lb1.setTenantId(TENANT_ID);
        lb1.setStatus("active");

        // validateAndGetLeaderboard 返回不同租户（安全兜底校验）
        Leaderboard lb2 = new Leaderboard();
        lb2.setId(LEADERBOARD_ID);
        lb2.setTenantId("other-tenant");
        lb2.setStatus("active");

        when(leaderboardMapper.findById(LEADERBOARD_ID)).thenReturn(lb1, lb2);

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于该租户");
    }

    @Test
    @DisplayName("submit: 排行榜计划不存在或已终止时抛出异常")
    void submit_planNotActive() {
        mockMetricResolve();
        LeaderboardMetric lm = new LeaderboardMetric();
        lm.setLeaderboardId(LEADERBOARD_ID);
        lm.setMetricId(METRIC_INTERNAL_ID);
        when(leaderboardMetricMapper.findByMetricIds(anyList())).thenReturn(List.of(lm));

        // resolveLeaderboardIdByMetricIds 返回active的计划
        Leaderboard lb1 = new Leaderboard();
        lb1.setId(LEADERBOARD_ID);
        lb1.setTenantId(TENANT_ID);
        lb1.setStatus("active");

        // validateAndGetLeaderboard 返回已终止的计划（安全兜底）
        Leaderboard lb2 = new Leaderboard();
        lb2.setId(LEADERBOARD_ID);
        lb2.setTenantId(TENANT_ID);
        lb2.setStatus("stopped");

        when(leaderboardMapper.findById(LEADERBOARD_ID)).thenReturn(lb1, lb2);

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在或已终止");
    }

    @Test
    @DisplayName("submit: 排行榜计划尚未开始时抛出异常")
    void submit_notStarted() {
        mockMetricResolve();
        LeaderboardMetric lm = new LeaderboardMetric();
        lm.setLeaderboardId(LEADERBOARD_ID);
        lm.setMetricId(METRIC_INTERNAL_ID);
        when(leaderboardMetricMapper.findByMetricIds(anyList())).thenReturn(List.of(lm));

        Leaderboard lb = new Leaderboard();
        lb.setId(LEADERBOARD_ID);
        lb.setTenantId(TENANT_ID);
        lb.setStatus("active");
        lb.setStartTime(System.currentTimeMillis() + 3600_000);
        when(leaderboardMapper.findById(LEADERBOARD_ID)).thenReturn(lb);

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尚未开始");
    }

    @Test
    @DisplayName("submit: 排行榜已结束时抛出异常")
    void submit_alreadyEnded() {
        mockMetricResolve();
        LeaderboardMetric lm = new LeaderboardMetric();
        lm.setLeaderboardId(LEADERBOARD_ID);
        lm.setMetricId(METRIC_INTERNAL_ID);
        when(leaderboardMetricMapper.findByMetricIds(anyList())).thenReturn(List.of(lm));

        Leaderboard lb = new Leaderboard();
        lb.setId(LEADERBOARD_ID);
        lb.setTenantId(TENANT_ID);
        lb.setStatus("active");
        lb.setStartTime(System.currentTimeMillis() - 120_000);
        lb.setEndTime(System.currentTimeMillis() - 60_000);
        when(leaderboardMapper.findById(LEADERBOARD_ID)).thenReturn(lb);

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已结束");
    }

    @Test
    @DisplayName("submit: 无活跃实例时抛出异常")
    void submit_noActiveInstance() {
        mockMetricResolve();

        LeaderboardMetric lm = new LeaderboardMetric();
        lm.setLeaderboardId(LEADERBOARD_ID);
        lm.setMetricId(METRIC_INTERNAL_ID);
        when(leaderboardMetricMapper.findByMetricIds(anyList())).thenReturn(List.of(lm));

        Leaderboard lb = new Leaderboard();
        lb.setId(LEADERBOARD_ID);
        lb.setTenantId(TENANT_ID);
        lb.setStatus("active");
        lb.setStartTime(System.currentTimeMillis() - 60_000);
        lb.setEndTime(null);
        when(leaderboardMapper.findById(LEADERBOARD_ID)).thenReturn(lb);

        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(null);

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有活跃的排行榜实例");
    }

    @Test
    @DisplayName("submit: 指标不属于该排行榜计划时抛出异常")
    void submit_metricNotInPlan() {
        mockMetricResolve();
        mockLeaderboardMatch();

        // 返回的排行榜指标不包含上报的指标
        LeaderboardMetric otherLm = new LeaderboardMetric();
        otherLm.setLeaderboardId(LEADERBOARD_ID);
        otherLm.setMetricId(999L);
        when(leaderboardMetricMapper.findByLeaderboardId(LEADERBOARD_ID)).thenReturn(List.of(otherLm));

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于该排行榜计划");
    }

    @Test
    @DisplayName("submit: 禁止重复上报时已存在的用户抛出异常")
    void submit_duplicateNotAllowed() {
        mockMetricResolve();
        Leaderboard lb = mockLeaderboardMatch();
        lb.setAllowDuplicateReport(0);

        when(scoreRecordMapper.findByUserAndInstance(LEADERBOARD_ID, INSTANCE_ID, USER_ID))
                .thenReturn(new ScoreRecord());

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许重复上报");
    }

    @Test
    @DisplayName("submit: 数据库唯一键冲突时抛出重复上报异常")
    void submit_duplicateKeyInDb() {
        mockMetricResolve();
        mockLeaderboardMatch();

        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        doThrow(new DuplicateKeyException("duplicate")).when(scoreRecordMapper).insertBatch(anyList());

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatThrownBy(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许重复上报");
    }

    // ==================== 用户数滚动场景 ====================

    @Test
    @DisplayName("submit: 用户数达到阈值时触发自动滚动")
    void submit_userCountRollTriggered() {
        mockMetricResolve();
        Leaderboard lb = mockLeaderboardMatch();
        lb.setRollStrategy("user_count");
        lb.setRollUserCount(10);
        mockRedisOps();

        @SuppressWarnings("unchecked")
        HyperLogLogOperations<String, String> hllOps = redisTemplate.opsForHyperLogLog();
        when(hllOps.size(anyString())).thenReturn(15L);
        when(scoreRecordMapper.countDistinctUsersByInstance(eq(LEADERBOARD_ID), eq(INSTANCE_ID))).thenReturn(12);
        when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(true);

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatCode(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .doesNotThrowAnyException();

        verify(distributedLock).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("submit: 用户数滚动-获取锁失败不滚动")
    void submit_userCountRoll_lockFailed() {
        mockMetricResolve();
        Leaderboard lb = mockLeaderboardMatch();
        lb.setRollStrategy("user_count");
        lb.setRollUserCount(10);
        mockRedisOps();

        @SuppressWarnings("unchecked")
        HyperLogLogOperations<String, String> hllOps = redisTemplate.opsForHyperLogLog();
        when(hllOps.size(anyString())).thenReturn(15L);
        when(scoreRecordMapper.countDistinctUsersByInstance(eq(LEADERBOARD_ID), eq(INSTANCE_ID))).thenReturn(12);
        when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(false);

        var metrics = List.of(metricValue(METRIC_EXT_ID, 100.0));
        assertThatCode(() -> scoreService.submit(TENANT_ID, USER_ID, metrics))
                .doesNotThrowAnyException();

        verify(distributedLock, never()).unlock(anyString(), anyString());
    }

    // ==================== batchSubmit 测试 ====================

    @Test
    @DisplayName("batchSubmit: 超过最大批量大小时抛出异常")
    void batchSubmit_exceedsMaxSize() {
        List<ScoreService.BatchSubmitItem> items = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            ScoreService.BatchSubmitItem item = new ScoreService.BatchSubmitItem();
            item.setUserId("user-" + i);
            item.setMetrics(List.of(metricValue(METRIC_EXT_ID, 100.0)));
            items.add(item);
        }

        assertThatThrownBy(() -> scoreService.batchSubmit(TENANT_ID, items))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过1000条");
    }

    @Test
    @DisplayName("batchSubmit: 指标集合不一致时抛出异常")
    void batchSubmit_inconsistentMetrics() {
        // 只mock第一个item的指标解析（第二个item的指标不会被解析，因为不一致检查先触发）
        Metric m1 = new Metric();
        m1.setId(10L); m1.setMetricId("m1");
        when(metricMapper.findByMetricId("m1")).thenReturn(m1);

        ScoreService.BatchSubmitItem item1 = new ScoreService.BatchSubmitItem();
        item1.setUserId("user-1");
        item1.setMetrics(List.of(metricValue("m1", 100.0)));

        ScoreService.BatchSubmitItem item2 = new ScoreService.BatchSubmitItem();
        item2.setUserId("user-2");
        item2.setMetrics(List.of(metricValue("m2", 200.0)));

        assertThatThrownBy(() -> scoreService.batchSubmit(TENANT_ID, List.of(item1, item2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("指标集合必须一致");
    }

    @Test
    @DisplayName("batchSubmit: 成功批量上报")
    void batchSubmit_success() {
        Metric m1 = new Metric();
        m1.setId(10L); m1.setMetricId("m1");
        Metric m2 = new Metric();
        m2.setId(20L); m2.setMetricId("m2");
        when(metricMapper.findByMetricId("m1")).thenReturn(m1);
        when(metricMapper.findByMetricId("m2")).thenReturn(m2);

        LeaderboardMetric lm1 = new LeaderboardMetric();
        lm1.setLeaderboardId(LEADERBOARD_ID); lm1.setMetricId(10L);
        LeaderboardMetric lm2 = new LeaderboardMetric();
        lm2.setLeaderboardId(LEADERBOARD_ID); lm2.setMetricId(20L);
        when(leaderboardMetricMapper.findByMetricIds(anyList())).thenReturn(List.of(lm1, lm2));

        Leaderboard lb = new Leaderboard();
        lb.setId(LEADERBOARD_ID); lb.setTenantId(TENANT_ID); lb.setStatus("active");
        lb.setStartTime(System.currentTimeMillis() - 60_000); lb.setEndTime(null);
        lb.setAllowDuplicateReport(1);
        when(leaderboardMapper.findById(LEADERBOARD_ID)).thenReturn(lb);

        LeaderboardInstance instance = new LeaderboardInstance();
        instance.setId(INSTANCE_ID); instance.setInstanceId("li-001");
        instance.setLeaderboardId(LEADERBOARD_ID); instance.setInstanceSeq(1);
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(instance);

        mockRedisOps();

        ScoreService.BatchSubmitItem item = new ScoreService.BatchSubmitItem();
        item.setUserId("user-1");
        item.setMetrics(List.of(metricValue("m1", 100.0), metricValue("m2", 200.0)));

        assertThatCode(() -> scoreService.batchSubmit(TENANT_ID, List.of(item)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("batchSubmit: 禁止重复上报时检测到重复抛出异常")
    void batchSubmit_duplicateCheck() {
        Metric m = new Metric();
        m.setId(10L); m.setMetricId("m1");
        when(metricMapper.findByMetricId("m1")).thenReturn(m);

        LeaderboardMetric lm = new LeaderboardMetric();
        lm.setLeaderboardId(LEADERBOARD_ID); lm.setMetricId(10L);
        when(leaderboardMetricMapper.findByMetricIds(anyList())).thenReturn(List.of(lm));

        Leaderboard lb = new Leaderboard();
        lb.setId(LEADERBOARD_ID); lb.setTenantId(TENANT_ID); lb.setStatus("active");
        lb.setStartTime(System.currentTimeMillis() - 60_000); lb.setEndTime(null);
        lb.setAllowDuplicateReport(0);
        when(leaderboardMapper.findById(LEADERBOARD_ID)).thenReturn(lb);

        LeaderboardInstance instance = new LeaderboardInstance();
        instance.setId(INSTANCE_ID); instance.setInstanceId("li-001");
        instance.setLeaderboardId(LEADERBOARD_ID); instance.setInstanceSeq(1);
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(instance);

        when(scoreRecordMapper.findByUserAndInstance(LEADERBOARD_ID, INSTANCE_ID, "user-1"))
                .thenReturn(new ScoreRecord());

        ScoreService.BatchSubmitItem item = new ScoreService.BatchSubmitItem();
        item.setUserId("user-1");
        item.setMetrics(List.of(metricValue("m1", 100.0)));

        assertThatThrownBy(() -> scoreService.batchSubmit(TENANT_ID, List.of(item)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许重复上报");
    }

    // ==================== doRoll 测试 ====================

    @Test
    @DisplayName("doRoll: 成功执行滚动")
    void doRoll_success() {
        LeaderboardInstance active = new LeaderboardInstance();
        active.setId(INSTANCE_ID);
        active.setInstanceSeq(1);
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(active);
        when(instanceMapper.getMaxInstanceSeq(LEADERBOARD_ID)).thenReturn(1);
        when(instanceMapper.insert(any(LeaderboardInstance.class))).thenReturn(1);

        Leaderboard lb = new Leaderboard();
        lb.setId(LEADERBOARD_ID);
        when(leaderboardMapper.findById(LEADERBOARD_ID)).thenReturn(lb);
        when(leaderboardMapper.update(any(Leaderboard.class))).thenReturn(1);

        assertThatCode(() -> scoreService.doRoll(LEADERBOARD_ID)).doesNotThrowAnyException();

        verify(instanceMapper).closeInstance(eq(INSTANCE_ID), anyLong());
        verify(instanceMapper).insert(any(LeaderboardInstance.class));
        verify(leaderboardMapper).update(any(Leaderboard.class));
    }

    @Test
    @DisplayName("doRoll: 无活跃实例时只创建新实例")
    void doRoll_noActiveInstance() {
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(null);
        when(instanceMapper.getMaxInstanceSeq(LEADERBOARD_ID)).thenReturn(0);
        when(instanceMapper.insert(any(LeaderboardInstance.class))).thenReturn(1);

        Leaderboard lb = new Leaderboard();
        lb.setId(LEADERBOARD_ID);
        when(leaderboardMapper.findById(LEADERBOARD_ID)).thenReturn(lb);
        when(leaderboardMapper.update(any(Leaderboard.class))).thenReturn(1);

        assertThatCode(() -> scoreService.doRoll(LEADERBOARD_ID)).doesNotThrowAnyException();

        verify(instanceMapper, never()).closeInstance(anyLong(), anyLong());
    }

    // ==================== MetricValue / BatchSubmitItem 测试 ====================

    @Test
    @DisplayName("MetricValue: getter/setter 正常工作")
    void metricValue_getterSetter() {
        ScoreService.MetricValue mv = new ScoreService.MetricValue();
        mv.setMetricId("m1");
        mv.setValue(BigDecimal.TEN);
        mv.setPayload("payload");

        assertThat(mv.getMetricId()).isEqualTo("m1");
        assertThat(mv.getValue()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(mv.getPayload()).isEqualTo("payload");
    }

    @Test
    @DisplayName("BatchSubmitItem: getter/setter 正常工作")
    void batchSubmitItem_getterSetter() {
        ScoreService.BatchSubmitItem item = new ScoreService.BatchSubmitItem();
        item.setUserId("user-1");
        List<ScoreService.MetricValue> metrics = List.of(metricValue("m1", 100.0));
        item.setMetrics(metrics);

        assertThat(item.getUserId()).isEqualTo("user-1");
        assertThat(item.getMetrics()).hasSize(1);
    }
}
