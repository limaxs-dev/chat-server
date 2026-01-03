package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.ArchivedRoom;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ArchivedRoomRepository implements PanacheRepositoryBase<ArchivedRoom, UUID> {

    public List<ArchivedRoom> findByTenantIdOrderByArchivedAtDesc(String tenantId, int page, int size) {
        return find("tenantId = ?1 ORDER BY archivedAt DESC", tenantId)
                .page(page, size)
                .list();
    }

    public ArchivedRoom findByOriginalRoomId(UUID originalRoomId) {
        return find("originalRoomId = ?1", originalRoomId).firstResult();
    }

    public long countByTenantId(String tenantId) {
        return count("tenantId = ?1", tenantId);
    }
}
