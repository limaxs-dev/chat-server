package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.ArchivedRoom;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ArchivedRoomRepository implements PanacheRepositoryBase<ArchivedRoom, UUID> {

    public List<ArchivedRoom> findAllOrderByArchivedAtDesc(int page, int size) {
        return findAll()
                .page(page, size)
                .list();
    }

    public ArchivedRoom findByOriginalRoomId(UUID originalRoomId) {
        return find("originalRoomId = ?1", originalRoomId).firstResult();
    }
}
