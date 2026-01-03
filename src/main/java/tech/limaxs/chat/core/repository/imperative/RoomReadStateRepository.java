package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.RoomReadState;
import tech.limaxs.chat.core.model.RoomReadStateId;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoomReadStateRepository implements PanacheRepositoryBase<RoomReadState, RoomReadStateId> {

    public Optional<RoomReadState> findById(UUID roomId, UUID userId) {
        return Optional.ofNullable(findById(new RoomReadStateId(roomId, userId)));
    }

    public RoomReadState findOrCreate(UUID roomId, UUID userId) {
        Optional<RoomReadState> existing = findById(roomId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        RoomReadState state = new RoomReadState();
        state.setId(new RoomReadStateId(roomId, userId));
        persist(state);
        return state;
    }
}
