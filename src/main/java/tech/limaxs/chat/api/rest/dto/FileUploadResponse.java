package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.UUID;

@RegisterForReflection
public class FileUploadResponse {

    private UUID fileId;
    private String uploadUrl;
    private String objectKey;

    public static FileUploadResponse create(UUID fileId, String uploadUrl, String objectKey) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(fileId);
        response.setUploadUrl(uploadUrl);
        response.setObjectKey(objectKey);
        return response;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }
}
