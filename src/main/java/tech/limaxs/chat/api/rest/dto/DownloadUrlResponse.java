package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DownloadUrlResponse {
    private String downloadUrl;

    public DownloadUrlResponse() {
    }

    public DownloadUrlResponse(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
