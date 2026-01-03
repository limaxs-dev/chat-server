package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.limaxs.chat.core.model.Message;

import java.time.LocalDateTime;
import java.util.UUID;

@RegisterForReflection
public class MessageResponse {

    private UUID id;
    private UUID roomId;
    private UUID senderId;
    private String type;
    private String contentText;
    private String contentMeta;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;

    public static MessageResponse from(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setRoomId(message.getRoomId());
        response.setSenderId(message.getSenderId());
        response.setType(message.getType().name());
        response.setContentText(message.getContentText());
        response.setContentMeta(message.getContentMeta());
        response.setCreatedAt(message.getCreatedAt());
        response.setEditedAt(message.getEditedAt());
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }
}
