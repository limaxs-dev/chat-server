package tech.limaxs.chat.core.repository.reactive;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.Room;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReactiveRoomRepository implements PanacheRepositoryBase<Room, UUID> {

    public Uni<List<Room>> findByParticipantUserId(UUID userId, int page, int size) {
        return find("""
            SELECT DISTINCT r FROM Room r
            JOIN r.participants p
            WHERE p.id.userId = ?1
            ORDER BY r.updatedAt DESC
            """, userId).page(page, size).list();
    }

    public Uni<List<Room>> findDirectRoomBetweenUsers(UUID user1Id, UUID user2Id) {
        return list("""
            SELECT DISTINCT r FROM Room r
            JOIN r.participants p1
            JOIN r.participants p2
            WHERE r.type = 'DIRECT'
            AND p1.id.userId = ?1
            AND p2.id.userId = ?2
            """, user1Id, user2Id);
    }
}
