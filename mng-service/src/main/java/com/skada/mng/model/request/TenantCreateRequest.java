package com.skada.mng.model.request;

/**
 * 创建租户请求
 */
public class TenantCreateRequest {

    private String name;
    private Integer allowAnonymousQuery;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAllowAnonymousQuery() { return allowAnonymousQuery; }
    public void setAllowAnonymousQuery(Integer allowAnonymousQuery) { this.allowAnonymousQuery = allowAnonymousQuery; }
}
