package com.skada.mng.service;

import com.skada.common.exception.BusinessException;
import com.skada.mng.mapper.AdminUserMapper;
import com.skada.mng.model.AdminUser;
import com.skada.mng.model.response.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService 单元测试")
class AdminServiceTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(adminUserMapper, redisTemplate, passwordEncoder);
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("成功登录返回Token和用户信息")
        void login_success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            AdminUser admin = new AdminUser();
            admin.setId(1L);
            admin.setUsername("admin");
            admin.setPasswordHash("hashed_password");
            admin.setDisplayId("ad_00000001");
            admin.setRole("admin");
            admin.setStatus(1);
            when(adminUserMapper.findByUsername("admin")).thenReturn(admin);
            when(passwordEncoder.matches("correct_password", "hashed_password")).thenReturn(true);

            LoginResponse resp = adminService.login("admin", "correct_password");

            assertThat(resp.getToken()).isNotNull().hasSize(32);
            assertThat(resp.getDisplayId()).isEqualTo("ad_00000001");
            assertThat(resp.getRole()).isEqualTo("admin");
            verify(valueOperations).set(startsWith("skada:token:"), eq("1:admin"), eq(7200L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("用户名为空时抛出异常")
        void login_emptyUsername_throws() {
            assertThatThrownBy(() -> adminService.login("", "password"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名不能为空");
        }

        @Test
        @DisplayName("密码为空时抛出异常")
        void login_emptyPassword_throws() {
            assertThatThrownBy(() -> adminService.login("admin", ""))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("密码不能为空");
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void login_userNotFound_throws() {
            when(adminUserMapper.findByUsername("nobody")).thenReturn(null);

            assertThatThrownBy(() -> adminService.login("nobody", "password"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名或密码错误");
        }

        @Test
        @DisplayName("密码错误时抛出异常")
        void login_wrongPassword_throws() {
            AdminUser admin = new AdminUser();
            admin.setId(1L);
            admin.setPasswordHash("hashed_password");
            admin.setStatus(1);
            when(adminUserMapper.findByUsername("admin")).thenReturn(admin);
            when(passwordEncoder.matches("wrong", "hashed_password")).thenReturn(false);

            assertThatThrownBy(() -> adminService.login("admin", "wrong"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名或密码错误");
        }

        @Test
        @DisplayName("账号已停用时抛出异常")
        void login_disabled_throws() {
            AdminUser admin = new AdminUser();
            admin.setId(1L);
            admin.setPasswordHash("hashed_password");
            admin.setStatus(0);
            when(adminUserMapper.findByUsername("admin")).thenReturn(admin);

            assertThatThrownBy(() -> adminService.login("admin", "password"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("账号已被停用");
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("有Token时删除Redis中的Token")
        void logout_withToken() {
            when(redisTemplate.delete("skada:token:my-token")).thenReturn(true);

            adminService.logout("my-token");

            verify(redisTemplate).delete("skada:token:my-token");
        }

        @Test
        @DisplayName("Token为null时不执行删除")
        void logout_nullToken() {
            adminService.logout(null);

            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("Token为空字符串时不执行删除")
        void logout_emptyToken() {
            adminService.logout("");

            verifyNoInteractions(redisTemplate);
        }
    }
}
