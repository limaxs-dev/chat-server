package tech.limaxs.chat.api.websocket.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.UUID;

@RegisterForReflection
public class TypingData {
    private UUID roomId;
    private boolean isTyping;

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public boolean isTyping() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        isTyping = typing;
    }
}
