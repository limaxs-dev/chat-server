package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.ChatUser;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ChatUserRepository implements PanacheRepositoryBase<ChatUser, UUID> {

    public ChatUser findOrCreate(UUID id, String name) {
        ChatUser existing = findById(id);
        if (existing != null) {
            return existing;
        }

        ChatUser user = new ChatUser();
        user.setId(id);
        user.setName(name);
        persist(user);
        return user;
    }
}
