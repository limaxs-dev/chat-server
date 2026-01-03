package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.ChatUser;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ChatUserRepository implements PanacheRepositoryBase<ChatUser, UUID> {

    public Optional<ChatUser> findByIdAndTenantId(UUID id, String tenantId) {
        return find("id = ?1 and tenantId = ?2", id, tenantId).firstResultOptional();
    }

    public ChatUser findOrCreate(UUID id, String tenantId, String name) {
        Optional<ChatUser> existing = findByIdAndTenantId(id, tenantId);
        if (existing.isPresent()) {
            return existing.get();
        }

        ChatUser user = new ChatUser();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setName(name);
        persist(user);
        return user;
    }
}
