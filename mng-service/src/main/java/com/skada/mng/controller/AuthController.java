package com.skada.mng.controller;

import com.skada.common.annotation.SkipLoginCheck;
import com.skada.common.model.BaseResponse;
import com.skada.mng.model.request.LoginRequest;
import com.skada.mng.model.response.LoginResponse;
import com.skada.mng.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员认证接口
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AdminService adminService;

    public AuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    @SkipLoginCheck
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = adminService.login(request.getUsername(), request.getPassword());
        return BaseResponse.success(response);
    }

    @PostMapping("/logout")
    public BaseResponse<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        adminService.logout(token);
        return BaseResponse.success();
    }

    @SkipLoginCheck
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return BaseResponse.success("ok");
    }
}
