package tech.limaxs.chat.api.websocket.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.UUID;

@RegisterForReflection
public class SendMessageData {
    private UUID roomId;
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
