package com.skada.api.controller;

import com.skada.api.model.LeaderboardInstance;
import com.skada.api.model.request.RollLeaderboardRequest;
import com.skada.api.model.response.RollLeaderboardResponse;
import com.skada.api.service.LeaderboardQueryService;
import com.skada.api.service.LeaderboardRollService;
import com.skada.common.exception.BusinessException;
import com.skada.common.model.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardController 单元测试")
class LeaderboardControllerTest {

    @Mock private LeaderboardQueryService queryService;
    @Mock private LeaderboardRollService rollService;
    @Mock private HttpServletRequest httpRequest;

    private LeaderboardController controller;

    private static final String PLAN_ID = "plan-001";
    private static final String TENANT_ID = "tenant-001";

    @BeforeEach
    void setUp() {
        controller = new LeaderboardController(queryService, rollService);
    }

    // ==================== getRanking ====================

    @Nested
    @DisplayName("GET /ranking")
    class GetRanking {

        @Test
        @DisplayName("成功返回排名数据")
        void getRanking_success() {
            LeaderboardQueryService.RankEntry entry = new LeaderboardQueryService.RankEntry();
            entry.setRank(1);
            entry.setUserId("user-001");
            when(queryService.getRanking(eq(PLAN_ID), isNull(), eq(0), eq(9), any()))
                    .thenReturn(List.of(entry));

            BaseResponse<List<LeaderboardQueryService.RankEntry>> resp =
                    controller.getRanking(PLAN_ID, null, 0, 9, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData()).hasSize(1);
            assertThat(resp.getData().get(0).getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("带tenantId的匿名查询")
        void getRanking_withTenantId() {
            when(httpRequest.getAttribute("saasTenantId")).thenReturn(TENANT_ID);
            when(queryService.getRanking(eq(PLAN_ID), isNull(), eq(0), eq(9), eq(TENANT_ID)))
                    .thenReturn(List.of());

            BaseResponse<List<LeaderboardQueryService.RankEntry>> resp =
                    controller.getRanking(PLAN_ID, null, 0, 9, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("指定instanceId查询")
        void getRanking_withInstanceId() {
            when(queryService.getRanking(eq(PLAN_ID), eq("li-001"), eq(0), eq(4), any()))
                    .thenReturn(List.of());

            BaseResponse<List<LeaderboardQueryService.RankEntry>> resp =
                    controller.getRanking(PLAN_ID, "li-001", 0, 4, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
        }
    }

    // ==================== getInstances ====================

    @Nested
    @DisplayName("GET /instances")
    class GetInstances {

        @Test
        @DisplayName("成功返回实例列表")
        void getInstances_success() {
            LeaderboardInstance inst1 = new LeaderboardInstance();
            inst1.setId(1L); inst1.setInstanceId("li-001");
            LeaderboardInstance inst2 = new LeaderboardInstance();
            inst2.setId(2L); inst2.setInstanceId("li-002");
            when(queryService.getInstances(PLAN_ID)).thenReturn(List.of(inst1, inst2));

            BaseResponse<List<LeaderboardInstance>> resp = controller.getInstances(PLAN_ID);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData()).hasSize(2);
        }

        @Test
        @DisplayName("返回空列表")
        void getInstances_empty() {
            when(queryService.getInstances(PLAN_ID)).thenReturn(List.of());

            BaseResponse<List<LeaderboardInstance>> resp = controller.getInstances(PLAN_ID);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData()).isEmpty();
        }
    }

    // ==================== roll ====================

    @Nested
    @DisplayName("POST /roll")
    class Roll {

        @Test
        @DisplayName("成功触发滚动")
        void roll_success() {
            when(httpRequest.getAttribute("saasTenantId")).thenReturn(TENANT_ID);

            RollLeaderboardRequest req = new RollLeaderboardRequest();
            req.setPlanId(PLAN_ID);
            req.setInstanceId("li-001");

            RollLeaderboardResponse rollResp = new RollLeaderboardResponse(PLAN_ID, "li-002", 2);
            when(rollService.roll(any(RollLeaderboardRequest.class), eq(TENANT_ID)))
                    .thenReturn(rollResp);

            BaseResponse<RollLeaderboardResponse> resp = controller.roll(req, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getPlanId()).isEqualTo(PLAN_ID);
            assertThat(resp.getData().getInstanceSeq()).isEqualTo(2);
        }

        @Test
        @DisplayName("缺少租户鉴权信息时抛出异常")
        void roll_missingTenant() {
            when(httpRequest.getAttribute("saasTenantId")).thenReturn(null);

            RollLeaderboardRequest req = new RollLeaderboardRequest();
            req.setPlanId(PLAN_ID);
            req.setInstanceId("li-001");

            assertThatThrownBy(() -> controller.roll(req, httpRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("缺少租户鉴权信息");

            verifyNoInteractions(rollService);
        }

        @Test
        @DisplayName("planId为空时抛出参数异常")
        void roll_nullPlanId() {
            when(httpRequest.getAttribute("saasTenantId")).thenReturn(TENANT_ID);

            RollLeaderboardRequest req = new RollLeaderboardRequest();
            req.setPlanId(null);
            req.setInstanceId("li-001");

            assertThatThrownBy(() -> controller.roll(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("planId");
        }

        @Test
        @DisplayName("instanceId为null时抛出参数异常")
        void roll_nullInstanceId() {
            when(httpRequest.getAttribute("saasTenantId")).thenReturn(TENANT_ID);

            RollLeaderboardRequest req = new RollLeaderboardRequest();
            req.setPlanId(PLAN_ID);
            req.setInstanceId(null);

            assertThatThrownBy(() -> controller.roll(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("instanceId");
        }

        @Test
        @DisplayName("租户ID为空字符串时抛出异常")
        void roll_blankTenant() {
            when(httpRequest.getAttribute("saasTenantId")).thenReturn("");

            RollLeaderboardRequest req = new RollLeaderboardRequest();
            req.setPlanId(PLAN_ID);
            req.setInstanceId("li-001");

            assertThatThrownBy(() -> controller.roll(req, httpRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("缺少租户鉴权信息");
        }
    }
}
