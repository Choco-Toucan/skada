package com.skada.api.mapper;

import com.skada.api.model.Metric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MetricMapper {

    Metric findById(@Param("id") Long id);

    Metric findByMetricId(@Param("metricId") String metricId);

    List<Metric> findByTenantId(@Param("tenantId") String tenantId);

    List<Metric> findByIds(@Param("ids") List<Long> ids);
}
