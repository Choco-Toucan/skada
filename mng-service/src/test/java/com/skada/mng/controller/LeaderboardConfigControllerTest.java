package com.skada.mng.controller;

import com.skada.common.model.BaseResponse;
import com.skada.common.model.PageResult;
import com.skada.mng.model.Leaderboard;
import com.skada.mng.model.LeaderboardInstance;
import com.skada.mng.model.request.LeaderboardCreateRequest;
import com.skada.mng.model.request.LeaderboardUpdateRequest;
import com.skada.mng.model.response.LeaderboardRankEntry;
import com.skada.mng.service.LeaderboardConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardConfigController 单元测试")
class LeaderboardConfigControllerTest {

    @Mock
    private LeaderboardConfigService configService;

    @Mock
    private HttpServletRequest httpRequest;

    private LeaderboardConfigController controller;

    private static final String ADMIN_ID = "ad_00000001";

    @BeforeEach
    void setUp() {
        controller = new LeaderboardConfigController(configService);
    }

    @Nested
    @DisplayName("POST /create")
    class Create {

        @Test
        @DisplayName("成功创建排行榜")
        void create_success() {
            when(httpRequest.getAttribute("adminId")).thenReturn(ADMIN_ID);
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setName("排行榜1");
            when(configService.create(any(), eq(ADMIN_ID))).thenReturn(lb);

            LeaderboardCreateRequest req = new LeaderboardCreateRequest();
            req.setName("排行榜1");
            BaseResponse<Leaderboard> resp = controller.create(req, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getName()).isEqualTo("排行榜1");
        }
    }

    @Nested
    @DisplayName("POST /update")
    class Update {

        @Test
        @DisplayName("成功更新排行榜")
        void update_success() {
            when(httpRequest.getAttribute("adminId")).thenReturn(ADMIN_ID);
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            when(configService.update(any(), eq(ADMIN_ID))).thenReturn(lb);

            LeaderboardUpdateRequest req = new LeaderboardUpdateRequest();
            req.setId(1L);
            BaseResponse<Leaderboard> resp = controller.update(req, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("GET /list")
    class List_ {

        @Test
        @DisplayName("分页查询排行榜")
        void list_success() {
            when(configService.findAllWithPage(1, 20, null))
                    .thenReturn(new PageResult<>(List.of(), 0, 1, 20));

            BaseResponse<PageResult<Leaderboard>> resp = controller.list(1, 20, null);

            assertThat(resp.getCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("按租户筛选")
        void list_byTenant() {
            when(configService.findAllWithPage(1, 20, "tn_abc"))
                    .thenReturn(new PageResult<>(List.of(), 0, 1, 20));

            BaseResponse<PageResult<Leaderboard>> resp = controller.list(1, 20, "tn_abc");

            assertThat(resp.getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("GET /get")
    class Get {

        @Test
        @DisplayName("查询排行榜详情")
        void get_success() {
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            lb.setName("详情");
            when(configService.findById(1L)).thenReturn(lb);

            BaseResponse<Leaderboard> resp = controller.get(1L);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getName()).isEqualTo("详情");
        }
    }

    @Nested
    @DisplayName("POST /roll")
    class Roll {

        @Test
        @DisplayName("成功手动滚动")
        void roll_success() {
            when(httpRequest.getAttribute("adminId")).thenReturn(ADMIN_ID);
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            when(configService.roll(1L, ADMIN_ID)).thenReturn(lb);

            BaseResponse<Leaderboard> resp = controller.roll(Map.of("leaderboardId", 1L), httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("排行榜ID为空时抛出异常")
        void roll_nullId_throws() {
            assertThatThrownBy(() -> controller.roll(Map.of(), httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("排行榜ID不能为空");
        }
    }

    @Nested
    @DisplayName("POST /stop")
    class Stop {

        @Test
        @DisplayName("成功终止排行榜")
        void stop_success() {
            when(httpRequest.getAttribute("adminId")).thenReturn(ADMIN_ID);
            Leaderboard lb = new Leaderboard();
            lb.setId(1L);
            when(configService.stop(1L, ADMIN_ID)).thenReturn(lb);

            BaseResponse<Leaderboard> resp = controller.stop(Map.of("leaderboardId", 1L), httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("GET /instances")
    class Instances {

        @Test
        @DisplayName("查询排行榜实例列表")
        void instances_success() {
            when(configService.getInstances(1L)).thenReturn(List.of());

            BaseResponse<List<LeaderboardInstance>> resp = controller.getInstances(1L);

            assertThat(resp.getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("GET /ranking")
    class Ranking {

        @Test
        @DisplayName("查询排名数据")
        void ranking_success() {
            when(configService.getRanking(1L, 100L, 0, 99))
                    .thenReturn(List.of());

            BaseResponse<List<LeaderboardRankEntry>> resp = controller.getRanking(1L, 100L, 0, 99);

            assertThat(resp.getCode()).isEqualTo(200);
        }
    }
}
