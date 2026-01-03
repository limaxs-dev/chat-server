package tech.limaxs.chat.core.repository.reactive;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.Message;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReactiveMessageRepository implements PanacheRepositoryBase<Message, UUID> {

    public Uni<List<Message>> findByRoomIdOrderByCreatedAtDesc(UUID roomId, int page, int size) {
        return find("roomId = ?1 order by createdAt desc", roomId)
                .page(page, size)
                .list();
    }

    public Uni<Message> persistAndFetch(Message message) {
        return persist(message).replaceWith(message);
    }
}
