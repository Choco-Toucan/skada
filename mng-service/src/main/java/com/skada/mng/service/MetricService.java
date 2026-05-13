package com.skada.mng.service;

import com.skada.common.exception.BusinessException;
import com.skada.common.model.PageResult;
import com.skada.mng.mapper.MetricMapper;
import com.skada.mng.model.Metric;
import com.skada.mng.model.request.MetricCreateRequest;
import com.skada.mng.model.request.MetricUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 指标管理服务
 */
@Service
public class MetricService {

    private final MetricMapper metricMapper;
    private final TenantService tenantService;

    public MetricService(MetricMapper metricMapper, TenantService tenantService) {
        this.metricMapper = metricMapper;
        this.tenantService = tenantService;
    }

    /**
     * 创建指标
     */
    public Metric create(MetricCreateRequest request, String adminId) {
        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new BusinessException("租户ID不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("指标名称不能为空");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BusinessException("指标编码不能为空");
        }
        if (tenantService.findByTenantId(request.getTenantId()) == null) {
            throw new BusinessException("租户不存在");
        }
        // 检查编码唯一性
        Metric existing = metricMapper.findByTenantIdAndCode(request.getTenantId(), request.getCode());
        if (existing != null) {
            throw new BusinessException("该租户下已存在相同编码的指标");
        }

        Metric metric = new Metric();
        metric.setTenantId(request.getTenantId());
        metric.setName(request.getName().trim());
        metric.setCode(request.getCode().trim());
        metric.setDescription(request.getDescription());
        metric.setCreateBy(adminId);
        metric.setUpdateBy(adminId);
        metricMapper.insert(metric);
        return metric;
    }

    /**
     * 更新指标
     */
    public Metric update(MetricUpdateRequest request, String adminId) {
        Metric metric = metricMapper.findById(request.getId());
        if (metric == null) {
            throw new BusinessException("指标不存在");
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            metric.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            metric.setDescription(request.getDescription());
        }
        metric.setUpdateBy(adminId);
        metricMapper.update(metric);
        return metricMapper.findById(metric.getId());
    }

    /**
     * 删除指标
     */
    public void delete(Long id) {
        Metric metric = metricMapper.findById(id);
        if (metric == null) {
            throw new BusinessException("指标不存在");
        }
        metricMapper.deleteById(id);
    }

    public Metric findById(Long id) {
        Metric metric = metricMapper.findById(id);
        if (metric == null) {
            throw new BusinessException("指标不存在");
        }
        return metric;
    }

    public List<Metric> findByTenantId(String tenantId) {
        return metricMapper.findByTenantId(tenantId);
    }

    public PageResult<Metric> findAllWithPage(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Metric> records = metricMapper.findAllWithPage(offset, pageSize);
        long total = metricMapper.count();
        return new PageResult<>(records, total, page, pageSize);
    }
}
