package com.skada.mng.service;

import com.skada.common.exception.BusinessException;
import com.skada.common.model.PageResult;
import com.skada.mng.mapper.MetricMapper;
import com.skada.mng.mapper.LeaderboardMetricMapper;
import com.skada.mng.model.Metric;
import com.skada.mng.model.Tenant;
import com.skada.mng.model.request.MetricCreateRequest;
import com.skada.mng.model.request.MetricUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricService 单元测试")
class MetricServiceTest {

    @Mock
    private MetricMapper metricMapper;

    @Mock
    private TenantService tenantService;

    @Mock
    private LeaderboardMetricMapper leaderboardMetricMapper;

    private MetricService metricService;

    private static final String ADMIN_ID = "ad_00000001";

    @BeforeEach
    void setUp() {
        metricService = new MetricService(metricMapper, tenantService, leaderboardMetricMapper);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("成功创建指标")
        void create_success() {
            when(tenantService.findByTenantId("tn_abc12345")).thenReturn(new Tenant());

            MetricCreateRequest req = new MetricCreateRequest();
            req.setTenantId("tn_abc12345");
            req.setName("击杀数");
            req.setDescription("玩家击杀敌人数量");

            metricService.create(req, ADMIN_ID);

            ArgumentCaptor<Metric> captor = ArgumentCaptor.forClass(Metric.class);
            verify(metricMapper).insert(captor.capture());
            Metric saved = captor.getValue();
            assertThat(saved.getMetricId()).startsWith("mt_");
            assertThat(saved.getTenantId()).isEqualTo("tn_abc12345");
            assertThat(saved.getName()).isEqualTo("击杀数");
            assertThat(saved.getDescription()).isEqualTo("玩家击杀敌人数量");
            assertThat(saved.getCreateBy()).isEqualTo(ADMIN_ID);
        }

        @Test
        @DisplayName("租户ID为空时抛出异常")
        void create_emptyTenantId_throws() {
            MetricCreateRequest req = new MetricCreateRequest();
            req.setTenantId("");
            req.setName("击杀数");

            assertThatThrownBy(() -> metricService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("租户ID不能为空");
        }

        @Test
        @DisplayName("指标名称为空时抛出异常")
        void create_emptyName_throws() {
            MetricCreateRequest req = new MetricCreateRequest();
            req.setTenantId("tn_abc12345");
            req.setName("");

            assertThatThrownBy(() -> metricService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("指标名称不能为空");
        }

        @Test
        @DisplayName("租户不存在时抛出异常")
        void create_tenantNotFound_throws() {
            when(tenantService.findByTenantId("tn_notexist")).thenReturn(null);

            MetricCreateRequest req = new MetricCreateRequest();
            req.setTenantId("tn_notexist");
            req.setName("击杀数");

            assertThatThrownBy(() -> metricService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("租户不存在");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("成功更新指标名称和描述")
        void update_success() {
            Metric existing = new Metric();
            existing.setId(1L);
            existing.setName("旧名称");
            existing.setDescription("旧描述");
            when(metricMapper.findById(1L)).thenReturn(existing);

            MetricUpdateRequest req = new MetricUpdateRequest();
            req.setId(1L);
            req.setName("新名称");
            req.setDescription("新描述");

            metricService.update(req, ADMIN_ID);

            verify(metricMapper).update(existing);
            assertThat(existing.getName()).isEqualTo("新名称");
            assertThat(existing.getDescription()).isEqualTo("新描述");
            assertThat(existing.getUpdateBy()).isEqualTo(ADMIN_ID);
        }

        @Test
        @DisplayName("指标不存在时抛出异常")
        void update_notFound_throws() {
            when(metricMapper.findById(99L)).thenReturn(null);

            MetricUpdateRequest req = new MetricUpdateRequest();
            req.setId(99L);

            assertThatThrownBy(() -> metricService.update(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("指标不存在");
        }

        @Test
        @DisplayName("部分更新仅修改传入字段")
        void update_partial() {
            Metric existing = new Metric();
            existing.setId(1L);
            existing.setName("旧名称");
            when(metricMapper.findById(1L)).thenReturn(existing);

            MetricUpdateRequest req = new MetricUpdateRequest();
            req.setId(1L);
            req.setDescription("仅更新描述");

            metricService.update(req, ADMIN_ID);

            verify(metricMapper).update(existing);
            assertThat(existing.getName()).isEqualTo("旧名称");
            assertThat(existing.getDescription()).isEqualTo("仅更新描述");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("成功删除无关联的指标")
        void delete_success() {
            Metric metric = new Metric();
            metric.setId(1L);
            when(metricMapper.findById(1L)).thenReturn(metric);
            when(leaderboardMetricMapper.countByMetricId(1L)).thenReturn(0);

            metricService.delete(1L);

            verify(metricMapper).deleteById(1L);
        }

        @Test
        @DisplayName("指标不存在时抛出异常")
        void delete_notFound_throws() {
            when(metricMapper.findById(99L)).thenReturn(null);

            assertThatThrownBy(() -> metricService.delete(99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("指标不存在");
        }

        @Test
        @DisplayName("指标被排行榜引用时抛出异常")
        void delete_referenced_throws() {
            Metric metric = new Metric();
            metric.setId(1L);
            when(metricMapper.findById(1L)).thenReturn(metric);
            when(leaderboardMetricMapper.countByMetricId(1L)).thenReturn(3);

            assertThatThrownBy(() -> metricService.delete(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("3 个排行榜计划引用");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("指标存在时返回")
        void findById_success() {
            Metric metric = new Metric();
            metric.setId(1L);
            metric.setName("击杀数");
            when(metricMapper.findById(1L)).thenReturn(metric);

            Metric result = metricService.findById(1L);

            assertThat(result.getName()).isEqualTo("击杀数");
        }

        @Test
        @DisplayName("指标不存在时抛出异常")
        void findById_notFound_throws() {
            when(metricMapper.findById(99L)).thenReturn(null);

            assertThatThrownBy(() -> metricService.findById(99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("指标不存在");
        }
    }

    @Nested
    @DisplayName("findAllWithPage")
    class FindWithPage {

        @Test
        @DisplayName("正常分页查询")
        void findAllWithPage_normal() {
            when(metricMapper.findAllWithPage(0, 20)).thenReturn(List.of());
            when(metricMapper.count()).thenReturn(0L);

            PageResult<Metric> result = metricService.findAllWithPage(1, 20);

            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("非法page参数自动修正")
        void findAllWithPage_invalidPage() {
            when(metricMapper.findAllWithPage(0, 20)).thenReturn(List.of());
            when(metricMapper.count()).thenReturn(0L);

            metricService.findAllWithPage(-1, 20);

            verify(metricMapper).findAllWithPage(0, 20);
        }
    }
}
