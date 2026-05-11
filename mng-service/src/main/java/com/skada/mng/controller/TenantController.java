package com.skada.mng.controller;

import com.skada.common.annotation.RequirePermission;
import com.skada.common.model.BaseResponse;
import com.skada.mng.model.Tenant;
import com.skada.mng.model.request.TenantCreateRequest;
import com.skada.mng.model.request.TenantUpdateRequest;
import com.skada.mng.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户管理接口
 */
@RestController
@RequestMapping("/api/v1/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @RequirePermission("admin")
    @PostMapping("/create")
    public BaseResponse<Tenant> create(@RequestBody TenantCreateRequest request,
                                       HttpServletRequest httpRequest) {
        String adminId = (String) httpRequest.getAttribute("adminId");
        Tenant tenant = tenantService.create(request, adminId);
        return BaseResponse.success(tenant);
    }

    @RequirePermission("admin")
    @PostMapping("/update")
    public BaseResponse<Tenant> update(@RequestBody TenantUpdateRequest request,
                                       HttpServletRequest httpRequest) {
        String adminId = (String) httpRequest.getAttribute("adminId");
        Tenant tenant = tenantService.update(request, adminId);
        return BaseResponse.success(tenant);
    }

    @GetMapping("/list")
    public BaseResponse<List<Tenant>> list() {
        return BaseResponse.success(tenantService.findAll());
    }

    @GetMapping("/get")
    public BaseResponse<Tenant> get(Long id) {
        return BaseResponse.success(tenantService.findById(id));
    }
}
