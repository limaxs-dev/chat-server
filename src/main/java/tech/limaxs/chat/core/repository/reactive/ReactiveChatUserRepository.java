package tech.limaxs.chat.core.repository.reactive;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.ChatUser;

import java.util.UUID;

@ApplicationScoped
public class ReactiveChatUserRepository implements PanacheRepositoryBase<ChatUser, UUID> {

    public Uni<ChatUser> findByIdAndTenantId(UUID id, String tenantId) {
        return find("id = ?1 and tenantId = ?2", id, tenantId).firstResult();
    }

    public Uni<ChatUser> findOrCreate(UUID id, String tenantId, String name) {
        return findByIdAndTenantId(id, tenantId)
                .onItem().ifNull().switchTo(() -> {
                    ChatUser user = new ChatUser();
                    user.setId(id);
                    user.setTenantId(tenantId);
                    user.setName(name);
                    return persist(user).replaceWith(user);
                });
    }
}
