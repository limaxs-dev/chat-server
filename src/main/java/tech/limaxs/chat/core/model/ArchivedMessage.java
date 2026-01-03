package tech.limaxs.chat.core.model;

import jakarta.persistence.*;
import tech.limaxs.chat.infra.converter.JsonbStringConverter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "archived_messages")
public class ArchivedMessage {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "archived_room_id", nullable = false)
    private UUID archivedRoomId;

    @Column(name = "original_room_id", nullable = false)
    private UUID originalRoomId;

    @Column(name = "original_message_id", nullable = false)
    private UUID originalMessageId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "content_meta")
    @Convert(converter = JsonbStringConverter.class)
    private String contentMeta;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "archived_at", nullable = false, updatable = false)
    private LocalDateTime archivedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (archivedAt == null) {
            archivedAt = LocalDateTime.now();
        }
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
