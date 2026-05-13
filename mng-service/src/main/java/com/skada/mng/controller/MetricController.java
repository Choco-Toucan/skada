package com.skada.mng.controller;

import com.skada.common.annotation.RequirePermission;
import com.skada.common.model.BaseResponse;
import com.skada.common.model.PageResult;
import com.skada.mng.model.Metric;
import com.skada.mng.model.request.MetricCreateRequest;
import com.skada.mng.model.request.MetricUpdateRequest;
import com.skada.mng.service.MetricService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 指标管理接口
 */
@RestController
@RequestMapping("/api/v1/metric")
public class MetricController {

    private final MetricService metricService;

    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }

    @RequirePermission("admin")
    @PostMapping("/create")
    public BaseResponse<Metric> create(@RequestBody MetricCreateRequest request,
                                        HttpServletRequest httpRequest) {
        String adminId = (String) httpRequest.getAttribute("adminId");
        return BaseResponse.success(metricService.create(request, adminId));
    }

    @RequirePermission("admin")
    @PostMapping("/update")
    public BaseResponse<Metric> update(@RequestBody MetricUpdateRequest request,
                                        HttpServletRequest httpRequest) {
        String adminId = (String) httpRequest.getAttribute("adminId");
        return BaseResponse.success(metricService.update(request, adminId));
    }

    @RequirePermission("admin")
    @PostMapping("/delete")
    public BaseResponse<Void> delete(@RequestBody Metric request) {
        metricService.delete(request.getId());
        return BaseResponse.success();
    }

    @GetMapping("/list")
    public BaseResponse<List<Metric>> list(@RequestParam String tenantId) {
        return BaseResponse.success(metricService.findByTenantId(tenantId));
    }

    @GetMapping("/page")
    public BaseResponse<PageResult<Metric>> page(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize) {
        return BaseResponse.success(metricService.findAllWithPage(page, pageSize));
    }

    @GetMapping("/detail")
    public BaseResponse<Metric> detail(@RequestParam Long id) {
        return BaseResponse.success(metricService.findById(id));
    }
}
