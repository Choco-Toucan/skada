package com.skada.mng.mapper;

import com.skada.mng.model.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TenantMapper {

    int insert(Tenant tenant);

    int update(Tenant tenant);

    Tenant findById(@Param("id") Long id);

    Tenant findByTenantId(@Param("tenantId") String tenantId);

    List<Tenant> findAll();

    List<Tenant> findAllWithPage(@Param("offset") int offset, @Param("limit") int limit);

    long count();
}
