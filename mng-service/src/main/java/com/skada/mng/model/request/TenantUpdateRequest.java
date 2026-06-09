package com.skada.mng.model.request;

/**
 * 更新租户请求
 */
public class TenantUpdateRequest {

    private Long id;
    private String name;
    private Boolean allowAnonymousQuery;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getAllowAnonymousQuery() { return allowAnonymousQuery; }
    public void setAllowAnonymousQuery(Boolean allowAnonymousQuery) { this.allowAnonymousQuery = allowAnonymousQuery; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
