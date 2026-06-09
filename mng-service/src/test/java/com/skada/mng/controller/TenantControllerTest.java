package com.skada.mng.controller;

import com.skada.common.exception.BusinessException;
import com.skada.common.model.BaseResponse;
import com.skada.common.model.PageResult;
import com.skada.mng.model.Tenant;
import com.skada.mng.model.request.TenantCreateRequest;
import com.skada.mng.model.request.TenantUpdateRequest;
import com.skada.mng.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantController 单元测试")
class TenantControllerTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private HttpServletRequest httpRequest;

    private TenantController controller;

    private static final String ADMIN_ID = "ad_00000001";

    @BeforeEach
    void setUp() {
        controller = new TenantController(tenantService);
    }

    @Nested
    @DisplayName("POST /create")
    class Create {

        @Test
        @DisplayName("成功创建租户")
        void create_success() {
            when(httpRequest.getAttribute("adminId")).thenReturn(ADMIN_ID);
            Tenant created = new Tenant();
            created.setId(1L);
            created.setName("新租户");
            when(tenantService.create(any(), eq(ADMIN_ID))).thenReturn(created);

            TenantCreateRequest req = new TenantCreateRequest();
            req.setName("新租户");
            BaseResponse<Tenant> resp = controller.create(req, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getName()).isEqualTo("新租户");
        }

        @Test
        @DisplayName("参数校验失败返回业务异常")
        void create_validationFailed() {
            when(httpRequest.getAttribute("adminId")).thenReturn(ADMIN_ID);
            when(tenantService.create(any(), eq(ADMIN_ID)))
                    .thenThrow(new BusinessException("租户名称不能为空"));

            TenantCreateRequest req = new TenantCreateRequest();
            assertThatThrownBy(() -> controller.create(req, httpRequest))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("POST /update")
    class Update {

        @Test
        @DisplayName("成功更新租户")
        void update_success() {
            when(httpRequest.getAttribute("adminId")).thenReturn(ADMIN_ID);
            Tenant updated = new Tenant();
            updated.setId(1L);
            updated.setName("更新后的名称");
            when(tenantService.update(any(), eq(ADMIN_ID))).thenReturn(updated);

            TenantUpdateRequest req = new TenantUpdateRequest();
            req.setId(1L);
            req.setName("更新后的名称");
            BaseResponse<Tenant> resp = controller.update(req, httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getName()).isEqualTo("更新后的名称");
        }
    }

    @Nested
    @DisplayName("GET /list")
    class List_ {

        @Test
        @DisplayName("成功分页查询租户列表")
        void list_success() {
            when(tenantService.findAllWithPage(1, 20))
                    .thenReturn(new PageResult<>(java.util.List.of(), 0, 1, 20));

            BaseResponse<PageResult<Tenant>> resp = controller.list(1, 20);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("使用默认分页参数")
        void list_defaultParams() {
            when(tenantService.findAllWithPage(1, 20))
                    .thenReturn(new PageResult<>(java.util.List.of(), 0, 1, 20));

            controller.list(1, 20);

            verify(tenantService).findAllWithPage(1, 20);
        }
    }

    @Nested
    @DisplayName("GET /get")
    class Get {

        @Test
        @DisplayName("成功查询租户详情")
        void get_success() {
            Tenant tenant = new Tenant();
            tenant.setId(1L);
            tenant.setName("租户详情");
            when(tenantService.findById(1L)).thenReturn(tenant);

            BaseResponse<Tenant> resp = controller.get(1L);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getName()).isEqualTo("租户详情");
        }
    }
}
