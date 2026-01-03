package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MessageRepository implements PanacheRepositoryBase<Message, UUID> {

    public List<Message> findByRoomIdOrderByCreatedAtDesc(UUID roomId, int page, int size) {
        return find("roomId = ?1 order by createdAt desc", roomId)
                .page(page, size)
                .list();
    }

    public Optional<Message> findByClientRef(UUID clientRef) {
        return find("clientRef = ?1", clientRef).firstResultOptional();
    }

    public Optional<Message> findByIdAndRoomId(UUID messageId, UUID roomId) {
        return find("id = ?1 and roomId = ?2", messageId, roomId).firstResultOptional();
    }

    public List<Message> findByRoomId(UUID roomId) {
        return list("roomId = ?1", roomId);
    }

    public void deleteByRoomId(UUID roomId) {
        delete("roomId = ?1", roomId);
    }
}
