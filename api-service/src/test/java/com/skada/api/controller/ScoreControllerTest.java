package com.skada.api.controller;

import com.skada.api.model.request.BatchScoreSubmitRequest;
import com.skada.api.model.request.ScoreSubmitRequest;
import com.skada.api.service.ScoreService;
import com.skada.common.exception.BusinessException;
import com.skada.common.model.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreController 单元测试")
class ScoreControllerTest {

    @Mock private ScoreService scoreService;
    @Mock private HttpServletRequest httpRequest;

    private ScoreController controller;

    private static final String TENANT_ID = "tenant-001";

    @BeforeEach
    void setUp() {
        controller = new ScoreController(scoreService);
    }

    private void mockTenantId() {
        when(httpRequest.getAttribute("saasTenantId")).thenReturn(TENANT_ID);
    }

    // ==================== submit ====================

    @Nested
    @DisplayName("POST /submit")
    class Submit {

        @Test
        @DisplayName("成功提交单条分数")
        void submit_success() {
            mockTenantId();
            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId("user-001");

            ScoreSubmitRequest.MetricValueRequest mv = new ScoreSubmitRequest.MetricValueRequest();
            mv.setMetricId("m1");
            mv.setValue(BigDecimal.valueOf(100));
            req.setMetrics(List.of(mv));

            BaseResponse<Void> resp = controller.submit(req, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
            verify(scoreService).submit(eq(TENANT_ID), eq("user-001"), anyList());
        }

        @Test
        @DisplayName("缺少租户鉴权信息时抛出异常")
        void submit_missingTenant() {
            when(httpRequest.getAttribute("saasTenantId")).thenReturn(null);

            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId("user-001");
            req.setMetrics(List.of());

            assertThatThrownBy(() -> controller.submit(req, httpRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("缺少租户鉴权信息");

            verifyNoInteractions(scoreService);
        }

        @Test
        @DisplayName("租户ID为空字符串时抛出异常")
        void submit_blankTenant() {
            when(httpRequest.getAttribute("saasTenantId")).thenReturn("");

            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId("user-001");
            req.setMetrics(List.of());

            assertThatThrownBy(() -> controller.submit(req, httpRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("缺少租户鉴权信息");
        }

        @Test
        @DisplayName("userId为空时抛出参数异常")
        void submit_nullUserId() {
            mockTenantId();
            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId(null);
            req.setMetrics(List.of());

            assertThatThrownBy(() -> controller.submit(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("metrics为空时抛出参数异常")
        void submit_emptyMetrics() {
            mockTenantId();
            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId("user-001");
            req.setMetrics(null);

            assertThatThrownBy(() -> controller.submit(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("metrics");
        }

        @Test
        @DisplayName("metricId为空时抛出参数异常")
        void submit_nullMetricId() {
            mockTenantId();
            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId("user-001");

            ScoreSubmitRequest.MetricValueRequest mv = new ScoreSubmitRequest.MetricValueRequest();
            mv.setMetricId(null);
            mv.setValue(BigDecimal.ONE);
            req.setMetrics(List.of(mv));

            assertThatThrownBy(() -> controller.submit(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("metricId");
        }

        @Test
        @DisplayName("value为空时抛出参数异常")
        void submit_nullValue() {
            mockTenantId();
            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId("user-001");

            ScoreSubmitRequest.MetricValueRequest mv = new ScoreSubmitRequest.MetricValueRequest();
            mv.setMetricId("m1");
            mv.setValue(null);
            req.setMetrics(List.of(mv));

            assertThatThrownBy(() -> controller.submit(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value");
        }

        @Test
        @DisplayName("带payload的成功提交")
        void submit_withPayload() {
            mockTenantId();
            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId("user-001");

            ScoreSubmitRequest.MetricValueRequest mv = new ScoreSubmitRequest.MetricValueRequest();
            mv.setMetricId("m1");
            mv.setValue(BigDecimal.valueOf(200));
            mv.setPayload("{\"extra\":\"data\"}");
            req.setMetrics(List.of(mv));

            BaseResponse<Void> resp = controller.submit(req, httpRequest);
            assertThat(resp.getCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("增量模式成功提交")
        void submit_incMode() {
            mockTenantId();
            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId("user-001");

            ScoreSubmitRequest.MetricValueRequest mv = new ScoreSubmitRequest.MetricValueRequest();
            mv.setMetricId("m1");
            mv.setValue(BigDecimal.valueOf(5));
            mv.setMode("inc");
            req.setMetrics(List.of(mv));

            BaseResponse<Void> resp = controller.submit(req, httpRequest);
            assertThat(resp.getCode()).isEqualTo(200);
            verify(scoreService).submit(eq(TENANT_ID), eq("user-001"), anyList());
        }

        @Test
        @DisplayName("mode为非法值时抛出参数异常")
        void submit_invalidMode() {
            mockTenantId();
            ScoreSubmitRequest req = new ScoreSubmitRequest();
            req.setUserId("user-001");

            ScoreSubmitRequest.MetricValueRequest mv = new ScoreSubmitRequest.MetricValueRequest();
            mv.setMetricId("m1");
            mv.setValue(BigDecimal.ONE);
            mv.setMode("invalid");
            req.setMetrics(List.of(mv));

            assertThatThrownBy(() -> controller.submit(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mode");
        }
    }

    // ==================== batchSubmit ====================

    @Nested
    @DisplayName("POST /batch-submit")
    class BatchSubmit {

        @Test
        @DisplayName("成功批量提交")
        void batchSubmit_success() {
            mockTenantId();

            BatchScoreSubmitRequest req = new BatchScoreSubmitRequest();
            BatchScoreSubmitRequest.BatchItem item = new BatchScoreSubmitRequest.BatchItem();
            item.setUserId("user-001");

            ScoreSubmitRequest.MetricValueRequest mv = new ScoreSubmitRequest.MetricValueRequest();
            mv.setMetricId("m1");
            mv.setValue(BigDecimal.valueOf(100));
            item.setMetrics(List.of(mv));
            req.setScores(List.of(item));

            BaseResponse<Void> resp = controller.batchSubmit(req, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
            verify(scoreService).batchSubmit(eq(TENANT_ID), anyList());
        }

        @Test
        @DisplayName("scores为空时抛出参数异常")
        void batchSubmit_emptyScores() {
            mockTenantId();
            BatchScoreSubmitRequest req = new BatchScoreSubmitRequest();
            req.setScores(null);

            assertThatThrownBy(() -> controller.batchSubmit(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("scores");
        }

        @Test
        @DisplayName("超过最大批量大小时抛出参数异常")
        void batchSubmit_exceedsMaxSize() {
            mockTenantId();
            BatchScoreSubmitRequest req = new BatchScoreSubmitRequest();
            List<BatchScoreSubmitRequest.BatchItem> items = new ArrayList<>();
            for (int i = 0; i < 1001; i++) {
                BatchScoreSubmitRequest.BatchItem item = new BatchScoreSubmitRequest.BatchItem();
                item.setUserId("user-" + i);
                item.setMetrics(List.of());
                items.add(item);
            }
            req.setScores(items);

            assertThatThrownBy(() -> controller.batchSubmit(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不能超过1000条");
        }

        @Test
        @DisplayName("userId为空时抛出参数异常")
        void batchSubmit_nullUserId() {
            mockTenantId();
            BatchScoreSubmitRequest req = new BatchScoreSubmitRequest();
            BatchScoreSubmitRequest.BatchItem item = new BatchScoreSubmitRequest.BatchItem();
            item.setUserId(null);
            item.setMetrics(List.of());
            req.setScores(List.of(item));

            assertThatThrownBy(() -> controller.batchSubmit(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("metrics为空时抛出参数异常")
        void batchSubmit_nullMetrics() {
            mockTenantId();
            BatchScoreSubmitRequest req = new BatchScoreSubmitRequest();
            BatchScoreSubmitRequest.BatchItem item = new BatchScoreSubmitRequest.BatchItem();
            item.setUserId("user-001");
            item.setMetrics(null);
            req.setScores(List.of(item));

            assertThatThrownBy(() -> controller.batchSubmit(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("metrics");
        }

        @Test
        @DisplayName("metricId为空时抛出参数异常")
        void batchSubmit_nullMetricId() {
            mockTenantId();
            BatchScoreSubmitRequest req = new BatchScoreSubmitRequest();
            BatchScoreSubmitRequest.BatchItem item = new BatchScoreSubmitRequest.BatchItem();
            item.setUserId("user-001");
            ScoreSubmitRequest.MetricValueRequest mv = new ScoreSubmitRequest.MetricValueRequest();
            mv.setMetricId(null);
            mv.setValue(BigDecimal.ONE);
            item.setMetrics(List.of(mv));
            req.setScores(List.of(item));

            assertThatThrownBy(() -> controller.batchSubmit(req, httpRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("metricId");
        }
    }
}
