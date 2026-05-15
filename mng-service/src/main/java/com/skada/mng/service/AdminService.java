package com.skada.mng.service;

import com.skada.common.exception.BusinessException;
import com.skada.mng.mapper.AdminUserMapper;
import com.skada.mng.model.AdminUser;
import com.skada.mng.model.response.LoginResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 管理员服务
 * 负责登录认证和Token管理
 */
@Service
public class AdminService {

    private static final String TOKEN_PREFIX = "skada:token:";
    private static final long TOKEN_EXPIRE_SECONDS = 7200;

    private final AdminUserMapper adminUserMapper;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminService(AdminUserMapper adminUserMapper,
                        StringRedisTemplate redisTemplate,
                        BCryptPasswordEncoder passwordEncoder) {
        this.adminUserMapper = adminUserMapper;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 管理员登录
     * 校验用户名密码，成功后生成Token存入Redis
     */
    public LoginResponse login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException("密码不能为空");
        }

        AdminUser admin = adminUserMapper.findByUsername(username.trim());
        if (admin == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BusinessException("账号已被停用");
        }
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 生成Token，Redis中存储 adminId:role 供拦截器读取
        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenValue = admin.getId() + ":" + admin.getRole();
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + token,
                tokenValue,
                TOKEN_EXPIRE_SECONDS,
                TimeUnit.SECONDS);

        return new LoginResponse(token, admin.getDisplayId(), admin.getRole());
    }

    /**
     * 管理员登出，清除Token
     */
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            redisTemplate.delete(TOKEN_PREFIX + token);
        }
    }
}
