package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.ArchivedParticipant;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ArchivedParticipantRepository implements PanacheRepositoryBase<ArchivedParticipant, UUID> {

    public List<ArchivedParticipant> findByArchivedRoomId(UUID archivedRoomId) {
        return list("archivedRoomId = ?1", archivedRoomId);
    }

    public long countByArchivedRoomId(UUID archivedRoomId) {
        return count("archivedRoomId = ?1", archivedRoomId);
    }
}
