package tech.limaxs.chat.core.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RoomReadStateId implements Serializable {

    private UUID roomId;
    private UUID userId;

    public RoomReadStateId() {
    }

    public RoomReadStateId(UUID roomId, UUID userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomReadStateId)) {
            return false;
        }
        RoomReadStateId that = (RoomReadStateId) o;
        return Objects.equals(roomId, that.roomId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId);
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
