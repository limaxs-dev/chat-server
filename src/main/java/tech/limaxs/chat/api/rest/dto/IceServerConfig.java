package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Represents a single ICE server configuration for WebRTC.
 * Can be a STUN server (urls only) or TURN server (urls + credential).
 */
@RegisterForReflection
public class IceServerConfig {
    private String[] urls;
    private String username;
    private String credential;

    public IceServerConfig() {
    }

    public IceServerConfig(String[] urls) {
        this.urls = urls;
    }

    public IceServerConfig(String[] urls, String username, String credential) {
        this.urls = urls;
        this.username = username;
        this.credential = credential;
    }

    public String[] getUrls() {
        return urls;
    }

    public void setUrls(String[] urls) {
        this.urls = urls;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}
