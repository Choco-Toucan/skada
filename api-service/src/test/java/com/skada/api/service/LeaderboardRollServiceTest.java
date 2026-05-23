package com.skada.api.service;

import com.skada.api.mapper.LeaderboardInstanceMapper;
import com.skada.api.mapper.LeaderboardMapper;
import com.skada.api.model.Leaderboard;
import com.skada.api.model.LeaderboardInstance;
import com.skada.api.model.request.RollLeaderboardRequest;
import com.skada.api.model.response.RollLeaderboardResponse;
import com.skada.common.enums.BizCode;
import com.skada.common.exception.BusinessException;
import com.skada.common.util.DistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardRollService 单元测试")
class LeaderboardRollServiceTest {

    @Mock private LeaderboardMapper leaderboardMapper;
    @Mock private LeaderboardInstanceMapper instanceMapper;
    @Mock private DistributedLock distributedLock;

    private LeaderboardRollService rollService;

    private static final String PLAN_ID = "plan-001";
    private static final String TENANT_ID = "tenant-001";
    private static final String INSTANCE_ID = "li-001";
    private static final Long LEADERBOARD_ID = 100L;
    private static final Long DB_INSTANCE_ID = 200L;

    @BeforeEach
    void setUp() {
        rollService = new LeaderboardRollService(leaderboardMapper, instanceMapper, distributedLock);
    }

    private RollLeaderboardRequest buildRequest() {
        RollLeaderboardRequest req = new RollLeaderboardRequest();
        req.setPlanId(PLAN_ID);
        req.setInstanceId(INSTANCE_ID);
        return req;
    }

    private Leaderboard buildLeaderboard() {
        Leaderboard lb = new Leaderboard();
        lb.setId(LEADERBOARD_ID);
        lb.setPlanId(PLAN_ID);
        lb.setTenantId(TENANT_ID);
        lb.setStatus("active");
        return lb;
    }

    private LeaderboardInstance buildActiveInstance() {
        LeaderboardInstance inst = new LeaderboardInstance();
        inst.setId(DB_INSTANCE_ID);
        inst.setInstanceId(INSTANCE_ID);
        inst.setLeaderboardId(LEADERBOARD_ID);
        inst.setInstanceSeq(1);
        return inst;
    }

    // ==================== 成功场景 ====================

    @Test
    @DisplayName("roll: 成功执行手动滚动")
    void roll_success() {
        Leaderboard lb = buildLeaderboard();
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        LeaderboardInstance active = buildActiveInstance();
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(active);

        when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        when(instanceMapper.getMaxInstanceSeq(LEADERBOARD_ID)).thenReturn(1);
        when(instanceMapper.insert(any(LeaderboardInstance.class))).thenReturn(1);
        when(leaderboardMapper.update(any(Leaderboard.class))).thenReturn(1);

        RollLeaderboardResponse result = rollService.roll(buildRequest(), TENANT_ID);

        assertThat(result.getPlanId()).isEqualTo(PLAN_ID);
        assertThat(result.getInstanceSeq()).isEqualTo(2);

        verify(instanceMapper).closeInstance(eq(DB_INSTANCE_ID), anyLong());
        verify(distributedLock).unlock(anyString(), anyString());
    }

    // ==================== 失败场景 ====================

    @Test
    @DisplayName("roll: 排行榜不存在或不属于该租户时抛出异常")
    void roll_notFoundOrDenied() {
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(null);

        assertThatThrownBy(() -> rollService.roll(buildRequest(), TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("排行榜不存在或无权操作");
    }

    @Test
    @DisplayName("roll: 排行榜属于其他租户时抛出异常")
    void roll_wrongTenant() {
        Leaderboard lb = buildLeaderboard();
        lb.setTenantId("other-tenant");
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        assertThatThrownBy(() -> rollService.roll(buildRequest(), TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("排行榜不存在或无权操作");
    }

    @Test
    @DisplayName("roll: 排行榜已终止时抛出异常")
    void roll_planStopped() {
        Leaderboard lb = buildLeaderboard();
        lb.setStatus("stopped");
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        assertThatThrownBy(() -> rollService.roll(buildRequest(), TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已终止");
    }

    @Test
    @DisplayName("roll: 有活跃实例但未提供instanceId时抛出异常")
    void roll_missingInstanceId() {
        Leaderboard lb = buildLeaderboard();
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        LeaderboardInstance active = buildActiveInstance();
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(active);

        RollLeaderboardRequest req = new RollLeaderboardRequest();
        req.setPlanId(PLAN_ID);
        req.setInstanceId(null);

        assertThatThrownBy(() -> rollService.roll(req, TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请提供当前活跃实例ID");
    }

    @Test
    @DisplayName("roll: 有活跃实例但提供空instanceId时抛出异常")
    void roll_emptyInstanceId() {
        Leaderboard lb = buildLeaderboard();
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        LeaderboardInstance active = buildActiveInstance();
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(active);

        RollLeaderboardRequest req = new RollLeaderboardRequest();
        req.setPlanId(PLAN_ID);
        req.setInstanceId("");

        assertThatThrownBy(() -> rollService.roll(req, TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请提供当前活跃实例ID");
    }

    @Test
    @DisplayName("roll: 无活跃实例但提供了instanceId时抛出异常")
    void roll_noActiveButInstanceProvided() {
        Leaderboard lb = buildLeaderboard();
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(null);

        assertThatThrownBy(() -> rollService.roll(buildRequest(), TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前无活跃实例");
    }

    @Test
    @DisplayName("roll: 实例ID不匹配时抛出异常")
    void roll_instanceIdMismatch() {
        Leaderboard lb = buildLeaderboard();
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        LeaderboardInstance active = buildActiveInstance();
        active.setInstanceId("li-wrong");
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(active);

        assertThatThrownBy(() -> rollService.roll(buildRequest(), TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实例已变更");
    }

    @Test
    @DisplayName("roll: 获取分布式锁失败时抛出并发冲突异常")
    void roll_lockFailed() {
        Leaderboard lb = buildLeaderboard();
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        LeaderboardInstance active = buildActiveInstance();
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID)).thenReturn(active);

        when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        assertThatThrownBy(() -> rollService.roll(buildRequest(), TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正在滚动中");
    }

    @Test
    @DisplayName("roll: 双重检查时实例已变更")
    void roll_doubleCheckInstanceChanged() {
        Leaderboard lb = buildLeaderboard();
        when(leaderboardMapper.findByPlanId(PLAN_ID)).thenReturn(lb);

        LeaderboardInstance active = buildActiveInstance();
        when(instanceMapper.findActiveByLeaderboardId(LEADERBOARD_ID))
                .thenReturn(active)   // 第一次
                .thenReturn(null);    // 双重检查

        when(distributedLock.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        assertThatThrownBy(() -> rollService.roll(buildRequest(), TENANT_ID))
                .isInstanceOf(BusinessException.class);

        verify(distributedLock).unlock(anyString(), anyString());
    }
}
