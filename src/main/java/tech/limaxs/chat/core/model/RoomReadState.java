package tech.limaxs.chat.core.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_read_states")
public class RoomReadState {

    @EmbeddedId
    private RoomReadStateId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomId")
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private ChatUser user;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public RoomReadStateId getId() {
        return id;
    }

    public void setId(RoomReadStateId id) {
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

    public UUID getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(UUID lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
