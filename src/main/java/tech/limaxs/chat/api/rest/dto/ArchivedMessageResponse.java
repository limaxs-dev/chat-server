package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.limaxs.chat.core.model.ArchivedMessage;

import java.time.LocalDateTime;
import java.util.UUID;

@RegisterForReflection
public class ArchivedMessageResponse {
    private UUID id;
    private UUID archivedRoomId;
    private UUID originalRoomId;
    private UUID originalMessageId;
    private UUID senderId;
    private String senderName;
    private String type;
    private String contentText;
    private String contentMeta;
    private LocalDateTime createdAt;
    private LocalDateTime archivedAt;

    public ArchivedMessageResponse() {
    }

    public static ArchivedMessageResponse from(ArchivedMessage archivedMessage) {
        ArchivedMessageResponse response = new ArchivedMessageResponse();
        response.id = archivedMessage.getId();
        response.archivedRoomId = archivedMessage.getArchivedRoomId();
        response.originalRoomId = archivedMessage.getOriginalRoomId();
        response.originalMessageId = archivedMessage.getOriginalMessageId();
        response.senderId = archivedMessage.getSenderId();
        response.senderName = archivedMessage.getSenderName();
        response.type = archivedMessage.getType();
        response.contentText = archivedMessage.getContentText();
        response.contentMeta = archivedMessage.getContentMeta();
        response.createdAt = archivedMessage.getCreatedAt();
        response.archivedAt = archivedMessage.getArchivedAt();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getArchivedRoomId() {
        return archivedRoomId;
    }

    public void setArchivedRoomId(UUID archivedRoomId) {
        this.archivedRoomId = archivedRoomId;
    }

    public UUID getOriginalRoomId() {
        return originalRoomId;
    }

    public void setOriginalRoomId(UUID originalRoomId) {
        this.originalRoomId = originalRoomId;
    }

    public UUID getOriginalMessageId() {
        return originalMessageId;
    }

    public void setOriginalMessageId(UUID originalMessageId) {
        this.originalMessageId = originalMessageId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
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

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }
}
