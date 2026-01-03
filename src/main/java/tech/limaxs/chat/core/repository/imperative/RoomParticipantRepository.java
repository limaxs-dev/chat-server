package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.RoomParticipant;
import tech.limaxs.chat.core.model.RoomParticipantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoomParticipantRepository implements PanacheRepositoryBase<RoomParticipant, RoomParticipantId> {

    public List<RoomParticipant> findByRoomId(UUID roomId) {
        return list("id.roomId = ?1", roomId);
    }

    public List<RoomParticipant> findByUserId(UUID userId) {
        return list("id.userId = ?1", userId);
    }

    public Optional<RoomParticipant> findById(UUID roomId, UUID userId) {
        return Optional.ofNullable(findById(new RoomParticipantId(roomId, userId)));
    }

    public void deleteByRoomId(UUID roomId) {
        delete("id.roomId = ?1", roomId);
    }
}
