package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Date;

@RegisterForReflection
public class TokenResponse {
    private String token;
    private String userId;
    private String name;
    private String tenantId;
    private Date expiresAt;

    public TokenResponse() {
    }

    public TokenResponse(String token, String userId, String name, String tenantId, Date expiresAt) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.tenantId = tenantId;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }
}
