package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.Room;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoomRepository implements PanacheRepositoryBase<Room, UUID> {

    public List<Room> findByTenantId(String tenantId, int page, int size) {
        return find("tenantId = ?1 order by updatedAt desc", tenantId)
                .page(page, size)
                .list();
    }

    public Optional<Room> findByIdAndTenantId(UUID id, String tenantId) {
        return find("id = ?1 and tenantId = ?2", id, tenantId).firstResultOptional();
    }

    public List<Room> findDirectRoomBetweenUsers(UUID user1Id, UUID user2Id, String tenantId) {
        return list("""
            SELECT DISTINCT r FROM Room r
            JOIN r.participants p1
            JOIN r.participants p2
            WHERE r.type = 'DIRECT'
            AND r.tenantId = ?3
            AND p1.user.id = ?1
            AND p2.user.id = ?2
            """, user1Id, user2Id, tenantId);
    }
}
