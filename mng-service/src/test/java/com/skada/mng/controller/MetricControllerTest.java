package com.skada.mng.controller;

import com.skada.common.exception.BusinessException;
import com.skada.common.model.BaseResponse;
import com.skada.common.model.PageResult;
import com.skada.mng.model.Metric;
import com.skada.mng.model.request.MetricCreateRequest;
import com.skada.mng.model.request.MetricUpdateRequest;
import com.skada.mng.service.MetricService;
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
@DisplayName("MetricController 单元测试")
class MetricControllerTest {

    @Mock
    private MetricService metricService;

    @Mock
    private HttpServletRequest httpRequest;

    private MetricController controller;

    private static final String ADMIN_ID = "ad_00000001";

    @BeforeEach
    void setUp() {
        controller = new MetricController(metricService);
    }

    @Nested
    @DisplayName("POST /create")
    class Create {

        @Test
        @DisplayName("成功创建指标")
        void create_success() {
            when(httpRequest.getAttribute("adminId")).thenReturn(ADMIN_ID);
            Metric metric = new Metric();
            metric.setId(1L);
            metric.setName("击杀数");
            when(metricService.create(any(), eq(ADMIN_ID))).thenReturn(metric);

            MetricCreateRequest req = new MetricCreateRequest();
            req.setTenantId("tn_abc12345");
            req.setName("击杀数");
            BaseResponse<Metric> resp = controller.create(req, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getName()).isEqualTo("击杀数");
        }
    }

    @Nested
    @DisplayName("POST /update")
    class Update {

        @Test
        @DisplayName("成功更新指标")
        void update_success() {
            when(httpRequest.getAttribute("adminId")).thenReturn(ADMIN_ID);
            Metric metric = new Metric();
            metric.setId(1L);
            metric.setName("新名称");
            when(metricService.update(any(), eq(ADMIN_ID))).thenReturn(metric);

            MetricUpdateRequest req = new MetricUpdateRequest();
            req.setId(1L);
            req.setName("新名称");
            BaseResponse<Metric> resp = controller.update(req, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getName()).isEqualTo("新名称");
        }
    }

    @Nested
    @DisplayName("POST /delete")
    class Delete {

        @Test
        @DisplayName("成功删除指标")
        void delete_success() {
            Metric req = new Metric();
            req.setId(1L);

            BaseResponse<Void> resp = controller.delete(req);

            assertThat(resp.getCode()).isEqualTo(200);
            verify(metricService).delete(1L);
        }
    }

    @Nested
    @DisplayName("GET /list")
    class List_ {

        @Test
        @DisplayName("按租户查询指标列表")
        void list_success() {
            when(metricService.findByTenantId("tn_abc12345")).thenReturn(List.of());

            BaseResponse<List<Metric>> resp = controller.list("tn_abc12345");

            assertThat(resp.getCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("GET /page")
    class Page_ {

        @Test
        @DisplayName("分页查询指标")
        void page_success() {
            when(metricService.findAllWithPage(1, 20))
                    .thenReturn(new PageResult<>(List.of(), 0, 1, 20));

            BaseResponse<PageResult<Metric>> resp = controller.page(1, 20);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getTotal()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("GET /detail")
    class Detail {

        @Test
        @DisplayName("查询指标详情")
        void detail_success() {
            Metric metric = new Metric();
            metric.setId(1L);
            metric.setName("指标详情");
            when(metricService.findById(1L)).thenReturn(metric);

            BaseResponse<Metric> resp = controller.detail(1L);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getName()).isEqualTo("指标详情");
        }
    }
}
