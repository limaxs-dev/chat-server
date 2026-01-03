package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.ArchivedMessage;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ArchivedMessageRepository implements PanacheRepositoryBase<ArchivedMessage, UUID> {

    public List<ArchivedMessage> findByArchivedRoomIdOrderByCreatedAtAsc(UUID archivedRoomId, int page, int size) {
        return find("archivedRoomId = ?1 ORDER BY createdAt ASC", archivedRoomId)
                .page(page, size)
                .list();
    }

    public List<ArchivedMessage> findByArchivedRoomId(UUID archivedRoomId) {
        return list("archivedRoomId = ?1", archivedRoomId);
    }

    public List<ArchivedMessage> findBySenderId(UUID senderId) {
        return list("senderId = ?1", senderId);
    }

    public long countByArchivedRoomId(UUID archivedRoomId) {
        return count("archivedRoomId = ?1", archivedRoomId);
    }
}
