package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@RegisterForReflection
public class SendMessageRequest {

    @NotNull
    private UUID roomId;

    @NotBlank
    private String type;

    private String contentText;

    private String contentMeta;

    private UUID clientRef;

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getContentMeta() {
        return contentMeta;
    }

    public void setContentMeta(String contentMeta) {
        this.contentMeta = contentMeta;
    }

    public UUID getClientRef() {
        return clientRef;
    }

    public void setClientRef(UUID clientRef) {
        this.clientRef = clientRef;
    }
}
