package com.skada.mng.service;

import com.skada.common.exception.BusinessException;
import com.skada.common.model.PageResult;
import com.skada.mng.mapper.TenantMapper;
import com.skada.mng.model.Tenant;
import com.skada.mng.model.request.TenantCreateRequest;
import com.skada.mng.model.request.TenantUpdateRequest;
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
@DisplayName("TenantService 单元测试")
class TenantServiceTest {

    @Mock
    private TenantMapper tenantMapper;

    private TenantService tenantService;

    private static final String ADMIN_ID = "ad_00000001";

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantMapper);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("成功创建租户，自动生成租户ID和密钥")
        void create_success() {
            TenantCreateRequest req = new TenantCreateRequest();
            req.setName("测试租户");
            req.setAllowAnonymousQuery(true);

            tenantService.create(req, ADMIN_ID);

            ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
            verify(tenantMapper).insert(captor.capture());
            Tenant saved = captor.getValue();
            assertThat(saved.getTenantId()).startsWith("tn_");
            assertThat(saved.getSecretKey()).startsWith("sk_");
            assertThat(saved.getName()).isEqualTo("测试租户");
            assertThat(saved.getAllowAnonymousQuery()).isTrue();
            assertThat(saved.getCreateBy()).isEqualTo(ADMIN_ID);
        }

        @Test
        @DisplayName("名称为空时抛出异常")
        void create_emptyName_throws() {
            TenantCreateRequest req = new TenantCreateRequest();
            req.setName("");

            assertThatThrownBy(() -> tenantService.create(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("名称不能为空");
        }

        @Test
        @DisplayName("allowAnonymousQuery未设置时默认为false")
        void create_defaultAnonymousQuery() {
            TenantCreateRequest req = new TenantCreateRequest();
            req.setName("测试租户");

            tenantService.create(req, ADMIN_ID);

            ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
            verify(tenantMapper).insert(captor.capture());
            assertThat(captor.getValue().getAllowAnonymousQuery()).isFalse();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("成功更新租户名称和状态")
        void update_success() {
            Tenant existing = new Tenant();
            existing.setId(1L);
            existing.setName("原名称");
            existing.setStatus(1);
            when(tenantMapper.findById(1L)).thenReturn(existing);
            when(tenantMapper.findById(1L)).thenReturn(existing); // 第二次查询返回更新后的

            TenantUpdateRequest req = new TenantUpdateRequest();
            req.setId(1L);
            req.setName("新名称");
            req.setAllowAnonymousQuery(false);
            req.setStatus(0);

            tenantService.update(req, ADMIN_ID);

            verify(tenantMapper).update(existing);
            assertThat(existing.getName()).isEqualTo("新名称");
            assertThat(existing.getAllowAnonymousQuery()).isFalse();
            assertThat(existing.getUpdateBy()).isEqualTo(ADMIN_ID);
        }

        @Test
        @DisplayName("租户不存在时抛出异常")
        void update_notFound_throws() {
            when(tenantMapper.findById(99L)).thenReturn(null);

            TenantUpdateRequest req = new TenantUpdateRequest();
            req.setId(99L);

            assertThatThrownBy(() -> tenantService.update(req, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("租户不存在");
        }

        @Test
        @DisplayName("部分更新只修改传入字段")
        void update_partial() {
            Tenant existing = new Tenant();
            existing.setId(1L);
            existing.setName("原名称");
            existing.setStatus(1);
            when(tenantMapper.findById(1L)).thenReturn(existing);

            TenantUpdateRequest req = new TenantUpdateRequest();
            req.setId(1L);
            req.setStatus(0);

            tenantService.update(req, ADMIN_ID);

            verify(tenantMapper).update(existing);
            assertThat(existing.getStatus()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("租户存在时返回")
        void findById_success() {
            Tenant tenant = new Tenant();
            tenant.setId(1L);
            tenant.setName("租户1");
            when(tenantMapper.findById(1L)).thenReturn(tenant);

            Tenant result = tenantService.findById(1L);

            assertThat(result.getName()).isEqualTo("租户1");
        }

        @Test
        @DisplayName("租户不存在时抛出异常")
        void findById_notFound_throws() {
            when(tenantMapper.findById(99L)).thenReturn(null);

            assertThatThrownBy(() -> tenantService.findById(99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("租户不存在");
        }
    }

    @Nested
    @DisplayName("findAllWithPage")
    class FindWithPage {

        @Test
        @DisplayName("默认分页参数")
        void findAllWithPage_default() {
            when(tenantMapper.findAllWithPage(0, 20)).thenReturn(List.of());
            when(tenantMapper.count()).thenReturn(0L);

            PageResult<Tenant> result = tenantService.findAllWithPage(1, 20);

            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(20);
            assertThat(result.getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("非法page参数自动修正为1")
        void findAllWithPage_invalidPage() {
            when(tenantMapper.findAllWithPage(0, 20)).thenReturn(List.of());
            when(tenantMapper.count()).thenReturn(0L);

            tenantService.findAllWithPage(0, 20);

            verify(tenantMapper).findAllWithPage(0, 20);
        }

        @Test
        @DisplayName("pageSize超过100时限制为100")
        void findAllWithPage_maxPageSize() {
            when(tenantMapper.findAllWithPage(eq(0), eq(100))).thenReturn(List.of());
            when(tenantMapper.count()).thenReturn(0L);

            tenantService.findAllWithPage(1, 200);

            verify(tenantMapper).findAllWithPage(0, 100);
        }
    }

    @Nested
    @DisplayName("findByTenantId")
    class FindByTenantId {

        @Test
        @DisplayName("找到租户")
        void findByTenantId_success() {
            Tenant tenant = new Tenant();
            tenant.setTenantId("tn_abc12345");
            when(tenantMapper.findByTenantId("tn_abc12345")).thenReturn(tenant);

            Tenant result = tenantService.findByTenantId("tn_abc12345");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("租户不存在返回null")
        void findByTenantId_notFound() {
            when(tenantMapper.findByTenantId("tn_notexist")).thenReturn(null);

            Tenant result = tenantService.findByTenantId("tn_notexist");

            assertThat(result).isNull();
        }
    }
}
