package tech.limaxs.chat.core.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "archived_rooms")
public class ArchivedRoom {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "original_room_id", nullable = false, unique = true)
    private UUID originalRoomId;

    @Column(nullable = false)
    private String type;

    private String name;

    @Column(name = "participant_count", nullable = false)
    private int participantCount = 0;

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @Column(name = "first_message_at")
    private LocalDateTime firstMessageAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "archived_at", nullable = false, updatable = false)
    private LocalDateTime archivedAt;

    @Column(name = "archived_by", nullable = false)
    private String archivedBy;

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

    public UUID getOriginalRoomId() {
        return originalRoomId;
    }

    public void setOriginalRoomId(UUID originalRoomId) {
        this.originalRoomId = originalRoomId;
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

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public LocalDateTime getFirstMessageAt() {
        return firstMessageAt;
    }

    public void setFirstMessageAt(LocalDateTime firstMessageAt) {
        this.firstMessageAt = firstMessageAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public String getArchivedBy() {
        return archivedBy;
    }

    public void setArchivedBy(String archivedBy) {
        this.archivedBy = archivedBy;
    }
}
