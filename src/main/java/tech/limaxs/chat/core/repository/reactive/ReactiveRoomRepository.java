package tech.limaxs.chat.core.repository.reactive;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.Room;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReactiveRoomRepository implements PanacheRepositoryBase<Room, UUID> {

    public Uni<List<Room>> findByTenantId(String tenantId, int page, int size) {
        return find("tenantId = ?1 order by updatedAt desc", tenantId)
                .page(page, size)
                .list();
    }
}
