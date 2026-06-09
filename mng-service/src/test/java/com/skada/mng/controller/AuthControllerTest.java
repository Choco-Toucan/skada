package com.skada.mng.controller;

import com.skada.common.exception.BusinessException;
import com.skada.common.model.BaseResponse;
import com.skada.mng.model.request.LoginRequest;
import com.skada.mng.model.response.LoginResponse;
import com.skada.mng.service.AdminService;
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
@DisplayName("AuthController 单元测试")
class AuthControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private HttpServletRequest httpRequest;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(adminService);
    }

    @Nested
    @DisplayName("POST /login")
    class Login {

        @Test
        @DisplayName("登录成功返回Token")
        void login_success() {
            LoginResponse loginResp = new LoginResponse("token-abc", "ad_00000001", "admin");
            when(adminService.login("admin", "password123")).thenReturn(loginResp);

            LoginRequest req = new LoginRequest();
            req.setUsername("admin");
            req.setPassword("password123");
            BaseResponse<LoginResponse> resp = controller.login(req);

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData().getToken()).isEqualTo("token-abc");
            assertThat(resp.getData().getRole()).isEqualTo("admin");
        }

        @Test
        @DisplayName("登录失败抛出异常")
        void login_failed() {
            when(adminService.login("admin", "wrong"))
                    .thenThrow(new BusinessException("用户名或密码错误"));

            LoginRequest req = new LoginRequest();
            req.setUsername("admin");
            req.setPassword("wrong");

            assertThatThrownBy(() -> controller.login(req))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("POST /logout")
    class Logout {

        @Test
        @DisplayName("成功登出清除Token")
        void logout_success() {
            when(httpRequest.getHeader("Authorization")).thenReturn("Bearer token-abc");

            BaseResponse<Void> resp = controller.logout(httpRequest);

            assertThat(resp.getCode()).isEqualTo(200);
            verify(adminService).logout("token-abc");
        }

        @Test
        @DisplayName("无Authorization头时传入null")
        void logout_noAuthHeader() {
            when(httpRequest.getHeader("Authorization")).thenReturn(null);

            controller.logout(httpRequest);

            verify(adminService).logout(null);
        }
    }

    @Nested
    @DisplayName("GET /health")
    class Health {

        @Test
        @DisplayName("健康检查返回ok")
        void health() {
            BaseResponse<String> resp = controller.health();

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getData()).isEqualTo("ok");
        }
    }
}
