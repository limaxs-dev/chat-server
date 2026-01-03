package tech.limaxs.chat.core.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_participants")
public class RoomParticipant {

    @EmbeddedId
    private RoomParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomId")
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private ChatUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    public enum ParticipantRole {
        MEMBER, ADMIN
    }

    public RoomParticipantId getId() {
        return id;
    }

    public void setId(RoomParticipantId id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public ChatUser getUser() {
        return user;
    }

    public void setUser(ChatUser user) {
        this.user = user;
    }

    public ParticipantRole getRole() {
        return role;
    }

    public void setRole(ParticipantRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
