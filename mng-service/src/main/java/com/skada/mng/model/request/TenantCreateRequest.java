package com.skada.mng.model.request;

/**
 * 创建租户请求
 */
public class TenantCreateRequest {

    private String name;
    private Boolean allowAnonymousQuery;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getAllowAnonymousQuery() { return allowAnonymousQuery; }
    public void setAllowAnonymousQuery(Boolean allowAnonymousQuery) { this.allowAnonymousQuery = allowAnonymousQuery; }
}
