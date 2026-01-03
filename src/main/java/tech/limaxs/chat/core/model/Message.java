package tech.limaxs.chat.core.model;

import jakarta.persistence.*;
import tech.limaxs.chat.infra.converter.JsonbStringConverter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "room_id", insertable = false, updatable = false)
    private UUID roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private ChatUser sender;

    @Column(name = "sender_id", insertable = false, updatable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(name = "content_text")
    private String contentText;

    @Column(name = "content_meta")
    @Convert(converter = JsonbStringConverter.class)
    private String contentMeta;

    @Column(name = "client_ref", unique = true)
    private UUID clientRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }

    public enum MessageType {
        TEXT, IMAGE, FILE, AUDIO, VIDEO, SYSTEM, VOICE_CALL
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public ChatUser getSender() {
        return sender;
    }

    public void setSender(ChatUser sender) {
        this.sender = sender;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }
}
