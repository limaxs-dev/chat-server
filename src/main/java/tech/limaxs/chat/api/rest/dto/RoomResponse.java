package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.limaxs.chat.core.model.Room;

import java.time.LocalDateTime;
import java.util.UUID;

@RegisterForReflection
public class RoomResponse {

    private UUID id;
    private String type;
    private String name;
    private String lastMessagePreview;
    private LocalDateTime updatedAt;

    public static RoomResponse from(Room room) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setType(room.getType().name());
        response.setName(room.getName());
        response.setLastMessagePreview(room.getLastMessagePreview());
        response.setUpdatedAt(room.getUpdatedAt());
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
