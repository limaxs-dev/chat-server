package tech.limaxs.chat.core.repository.reactive;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.RoomParticipant;
import tech.limaxs.chat.core.model.RoomParticipantId;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReactiveRoomParticipantRepository implements PanacheRepository<RoomParticipant> {

    public Uni<List<RoomParticipant>> findByRoomId(UUID roomId) {
        return list("id.roomId = ?1", roomId);
    }

    public Uni<List<RoomParticipant>> findByUserId(UUID userId) {
        return list("id.userId = ?1", userId);
    }

    public Uni<RoomParticipant> findById(UUID roomId, UUID userId) {
        return find("id.roomId = ?1 and id.userId = ?2", roomId, userId).firstResult();
    }

    public Uni<Long> deleteByRoomId(UUID roomId) {
        return delete("id.roomId = ?1", roomId);
    }
}
