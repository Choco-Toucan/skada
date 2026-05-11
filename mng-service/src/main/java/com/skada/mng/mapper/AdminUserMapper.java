package com.skada.mng.mapper;

import com.skada.mng.model.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminUserMapper {

    AdminUser findByUsername(@Param("username") String username);
}
