package tech.limaxs.chat.core.repository.reactive;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.ChatUser;

import java.util.UUID;

@ApplicationScoped
public class ReactiveChatUserRepository implements PanacheRepositoryBase<ChatUser, UUID> {

    public Uni<ChatUser> findOrCreate(UUID id, String name) {
        return findById(id)
                .onItem().ifNull().switchTo(() -> {
                    ChatUser user = new ChatUser();
                    user.setId(id);
                    user.setName(name);
                    return persist(user).replaceWith(user);
                });
    }
}
