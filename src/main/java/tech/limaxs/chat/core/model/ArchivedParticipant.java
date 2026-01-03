package tech.limaxs.chat.core.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "archived_participants")
public class ArchivedParticipant {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "archived_room_id", nullable = false)
    private UUID archivedRoomId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
