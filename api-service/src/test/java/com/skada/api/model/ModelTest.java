package com.skada.api.model;

import com.skada.api.model.request.BatchScoreSubmitRequest;
import com.skada.api.model.request.RollLeaderboardRequest;
import com.skada.api.model.request.ScoreSubmitRequest;
import com.skada.api.model.response.RollLeaderboardResponse;
import com.skada.common.enums.TenantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Model 单元测试")
class ModelTest {

    @Nested
    @DisplayName("Leaderboard")
    class LeaderboardTest {
        @Test
        @DisplayName("getter/setter 及默认值")
        void getterSetter() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setPlanId("plan-001");
            lb.setTenantId("tenant-001");
            lb.setName("测试排行榜");
            lb.setStartTime(1000L);
            lb.setEndTime(2000L);
            lb.setStatus("active");
            lb.setRollStrategy("none");
            lb.setRollUserCount(10);
            lb.setCurrentInstanceId(100L);

            assertThat(lb.getId()).isEqualTo(1L);
            assertThat(lb.getPlanId()).isEqualTo("plan-001");
            assertThat(lb.getTenantId()).isEqualTo("tenant-001");
            assertThat(lb.getName()).isEqualTo("测试排行榜");
            assertThat(lb.getStartTime()).isEqualTo(1000L);
            assertThat(lb.getEndTime()).isEqualTo(2000L);
            assertThat(lb.getStatus()).isEqualTo("active");
            assertThat(lb.getRollStrategy()).isEqualTo("none");
            assertThat(lb.getRollUserCount()).isEqualTo(10);
            assertThat(lb.getCurrentInstanceId()).isEqualTo(100L);

            // 默认值
            assertThat(lb.getMaxQueryUsers()).isEqualTo(1000);
            assertThat(lb.getAllowDuplicateReport()).isEqualTo(0);
            assertThat(lb.getAllowHistoryQuery()).isEqualTo(1);
        }

        @Test
        @DisplayName("自定义maxQueryUsers")
        void customMaxQueryUsers() {
            Leaderboard lb = new Leaderboard();
            lb.setMaxQueryUsers(500);
            assertThat(lb.getMaxQueryUsers()).isEqualTo(500);
        }

        @Test
        @DisplayName("自定义allowDuplicateReport")
        void customAllowDuplicateReport() {
            Leaderboard lb = new Leaderboard();
            lb.setAllowDuplicateReport(1);
            assertThat(lb.getAllowDuplicateReport()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("LeaderboardInstance")
    class LeaderboardInstanceTest {
        @Test
        @DisplayName("getter/setter 正常工作")
        void getterSetter() {
            LeaderboardInstance inst = new LeaderboardInstance();
            inst.setId(1L);
            inst.setInstanceId("li-001");
            inst.setLeaderboardId(100L);
            inst.setInstanceSeq(5);
            inst.setStartTime(1000L);
            inst.setEndTime(2000L);
            inst.setStatus("active");
            inst.setCreateBy("admin");
            inst.setUpdateBy("admin");

            assertThat(inst.getId()).isEqualTo(1L);
            assertThat(inst.getInstanceId()).isEqualTo("li-001");
            assertThat(inst.getLeaderboardId()).isEqualTo(100L);
            assertThat(inst.getInstanceSeq()).isEqualTo(5);
            assertThat(inst.getStartTime()).isEqualTo(1000L);
            assertThat(inst.getEndTime()).isEqualTo(2000L);
            assertThat(inst.getStatus()).isEqualTo("active");
            assertThat(inst.getCreateBy()).isEqualTo("admin");
            assertThat(inst.getUpdateBy()).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("LeaderboardMetric")
    class LeaderboardMetricTest {
        @Test
        @DisplayName("getter/setter 正常工作")
        void getterSetter() {
            LeaderboardMetric lm = new LeaderboardMetric();
            lm.setId(1L);
            lm.setLeaderboardId(100L);
            lm.setMetricId(10L);
            lm.setPriority(3);
            lm.setSortOrder("desc");
            lm.setCreateBy("admin");
            lm.setUpdateBy("admin");

            assertThat(lm.getId()).isEqualTo(1L);
            assertThat(lm.getLeaderboardId()).isEqualTo(100L);
            assertThat(lm.getMetricId()).isEqualTo(10L);
            assertThat(lm.getPriority()).isEqualTo(3);
            assertThat(lm.getSortOrder()).isEqualTo("desc");
            assertThat(lm.getCreateBy()).isEqualTo("admin");
            assertThat(lm.getUpdateBy()).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("Metric")
    class MetricTest {
        @Test
        @DisplayName("getter/setter 正常工作")
        void getterSetter() {
            Metric m = new Metric();
            m.setId(1L);
            m.setMetricId("metric-001");
            m.setTenantId("tenant-001");
            m.setName("指标名称");
            m.setDescription("指标描述");

            assertThat(m.getId()).isEqualTo(1L);
            assertThat(m.getMetricId()).isEqualTo("metric-001");
            assertThat(m.getTenantId()).isEqualTo("tenant-001");
            assertThat(m.getName()).isEqualTo("指标名称");
            assertThat(m.getDescription()).isEqualTo("指标描述");
        }
    }

    @Nested
    @DisplayName("ScoreRecord")
    class ScoreRecordTest {
        @Test
        @DisplayName("getter/setter 正常工作")
        void getterSetter() {
            ScoreRecord sr = new ScoreRecord();
            sr.setId(1L);
            sr.setTenantId("tenant-001");
            sr.setLeaderboardId(100L);
            sr.setInstanceId(200L);
            sr.setMetricId(10L);
            sr.setUserId("user-001");
            sr.setScore(BigDecimal.valueOf(99.5));
            sr.setPayload("{\"key\":\"val\"}");
            sr.setCreateTime("2024-01-01");
            sr.setUpdateTime("2024-01-02");

            assertThat(sr.getId()).isEqualTo(1L);
            assertThat(sr.getTenantId()).isEqualTo("tenant-001");
            assertThat(sr.getLeaderboardId()).isEqualTo(100L);
            assertThat(sr.getInstanceId()).isEqualTo(200L);
            assertThat(sr.getMetricId()).isEqualTo(10L);
            assertThat(sr.getUserId()).isEqualTo("user-001");
            assertThat(sr.getScore()).isEqualByComparingTo(BigDecimal.valueOf(99.5));
            assertThat(sr.getPayload()).isEqualTo("{\"key\":\"val\"}");
        }
    }

    @Nested
    @DisplayName("Tenant")
    class TenantTest {
        @Test
        @DisplayName("getter/setter 正常工作")
        void getterSetter() {
            Tenant t = new Tenant();
            t.setId(1L);
            t.setTenantId("tenant-001");
            t.setName("测试租户");
            t.setSecretKey("secret-123");
            t.setAllowAnonymousQuery(1);
            t.setStatus(1);

            assertThat(t.getId()).isEqualTo(1L);
            assertThat(t.getTenantId()).isEqualTo("tenant-001");
            assertThat(t.getName()).isEqualTo("测试租户");
            assertThat(t.getSecretKey()).isEqualTo("secret-123");
            assertThat(t.getAllowAnonymousQuery()).isEqualTo(1);
            assertThat(t.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("isEnabled: 启用状态返回true")
        void isEnabled_true() {
            Tenant t = new Tenant();
            t.setStatus(1);
            assertThat(t.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("isEnabled: 停用状态返回false")
        void isEnabled_false() {
            Tenant t = new Tenant();
            t.setStatus(0);
            assertThat(t.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("getStatusEnum: 返回正确枚举")
        void getStatusEnum() {
            Tenant t = new Tenant();
            t.setStatus(1);
            assertThat(t.getStatusEnum()).isEqualTo(TenantStatus.ENABLED);

            t.setStatus(0);
            assertThat(t.getStatusEnum()).isEqualTo(TenantStatus.DISABLED);
        }

        @Test
        @DisplayName("setStatusEnum: 正确设置状态值")
        void setStatusEnum() {
            Tenant t = new Tenant();
            t.setStatusEnum(TenantStatus.ENABLED);
            assertThat(t.getStatus()).isEqualTo(1);

            t.setStatusEnum(TenantStatus.DISABLED);
            assertThat(t.getStatus()).isEqualTo(0);
        }
    }

    // ==================== Request DTOs ====================

    @Nested
    @DisplayName("ScoreSubmitRequest")
    class ScoreSubmitRequestTest {
        @Test
        @DisplayName("getter/setter 正常工作")
        void getterSetter() {
            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId("user-001");

            ScoreSubmitRequest.MetricValueRequest mv = new ScoreSubmitRequest.MetricValueRequest();
            mv.setMetricId("m1");
            mv.setValue(BigDecimal.TEN);
            mv.setPayload("payload");
            req.setMetrics(List.of(mv));

            assertThat(req.getUserId()).isEqualTo("user-001");
            assertThat(req.getMetrics()).hasSize(1);
            assertThat(req.getMetrics().get(0).getMetricId()).isEqualTo("m1");
            assertThat(req.getMetrics().get(0).getValue()).isEqualByComparingTo(BigDecimal.TEN);
            assertThat(req.getMetrics().get(0).getPayload()).isEqualTo("payload");
        }
    }

    @Nested
    @DisplayName("BatchScoreSubmitRequest")
    class BatchScoreSubmitRequestTest {
        @Test
        @DisplayName("getter/setter 正常工作")
        void getterSetter() {
            BatchScoreSubmitRequest req = new BatchScoreSubmitRequest();

            BatchScoreSubmitRequest.BatchItem item = new BatchScoreSubmitRequest.BatchItem();
            item.setUserId("user-001");
            ScoreSubmitRequest.MetricValueRequest mv = new ScoreSubmitRequest.MetricValueRequest();
            mv.setMetricId("m1");
            mv.setValue(BigDecimal.ONE);
            item.setMetrics(List.of(mv));
            req.setScores(List.of(item));

            assertThat(req.getScores()).hasSize(1);
            assertThat(req.getScores().get(0).getUserId()).isEqualTo("user-001");
        }
    }

    @Nested
    @DisplayName("RollLeaderboardRequest")
    class RollLeaderboardRequestTest {
        @Test
        @DisplayName("getter/setter 正常工作")
        void getterSetter() {
            RollLeaderboardRequest req = new RollLeaderboardRequest();
            req.setPlanId("plan-001");
            req.setInstanceId("li-001");

            assertThat(req.getPlanId()).isEqualTo("plan-001");
            assertThat(req.getInstanceId()).isEqualTo("li-001");
        }
    }

    @Nested
    @DisplayName("RollLeaderboardResponse")
    class RollLeaderboardResponseTest {
        @Test
        @DisplayName("构造函数和getter正常工作")
        void constructorAndGetter() {
            RollLeaderboardResponse resp = new RollLeaderboardResponse("plan-001", "li-002", 5);

            assertThat(resp.getPlanId()).isEqualTo("plan-001");
            assertThat(resp.getInstanceId()).isEqualTo("li-002");
            assertThat(resp.getInstanceSeq()).isEqualTo(5);
        }
    }
}
