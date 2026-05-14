package com.skada.mng.mapper;

import com.skada.mng.model.Metric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MetricMapper {

    List<Metric> findByTenantId(@Param("tenantId") String tenantId);

    Metric findById(@Param("id") Long id);

    Metric findByMetricId(@Param("metricId") String metricId);

    int insert(Metric metric);

    int update(Metric metric);

    int deleteById(@Param("id") Long id);

    List<Metric> findAllWithPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    long count();

    List<Metric> findByIds(@Param("ids") List<Long> ids);
}
