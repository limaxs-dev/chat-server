package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.Room;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoomRepository implements PanacheRepositoryBase<Room, UUID> {

    public List<Room> findByParticipantUserId(UUID userId, int page, int size) {
        return find("""
            SELECT DISTINCT r FROM Room r
            JOIN r.participants p
            WHERE p.id.userId = ?1
            ORDER BY r.updatedAt DESC
            """, userId).page(page, size).list();
    }

    public Optional<Room> findByIdAndParticipantUserId(UUID roomId, UUID userId) {
        return list("""
            SELECT DISTINCT r FROM Room r
            JOIN r.participants p
            WHERE r.id = ?1
            AND p.id.userId = ?2
            """, roomId, userId).stream().findFirst();
    }

    public boolean existsByIdAndParticipantUserId(UUID roomId, UUID userId) {
        long count = count("""
            SELECT COUNT(r) FROM Room r
            JOIN r.participants p
            WHERE r.id = ?1
            AND p.id.userId = ?2
            """, roomId, userId);
        return count > 0;
    }

    public List<Room> findDirectRoomBetweenUsers(UUID user1Id, UUID user2Id) {
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
