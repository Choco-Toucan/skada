package com.skada.mng.model.response;

/**
 * 登录响应
 */
public class LoginResponse {

    private String token;
    private String displayId;
    private String role;

    public LoginResponse(String token, String displayId, String role) {
        this.token = token;
        this.displayId = displayId;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getDisplayId() { return displayId; }
    public String getRole() { return role; }
}
