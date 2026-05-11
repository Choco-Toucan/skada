package com.skada.api.mapper;

import com.skada.api.model.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantMapper {

    Tenant findByTenantId(@Param("tenantId") String tenantId);
}
