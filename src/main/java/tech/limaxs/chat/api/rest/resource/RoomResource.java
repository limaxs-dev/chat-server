package tech.limaxs.chat.api.rest.resource;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import tech.limaxs.chat.api.rest.dto.CreateRoomRequest;
import tech.limaxs.chat.api.rest.dto.RoomResponse;
import tech.limaxs.chat.api.rest.dto.MessageResponse;
import tech.limaxs.chat.core.model.Room;
import tech.limaxs.chat.core.model.RoomParticipant;
import tech.limaxs.chat.core.model.RoomParticipantId;
import tech.limaxs.chat.core.model.ChatUser;
import tech.limaxs.chat.core.model.Message;
import tech.limaxs.chat.core.repository.imperative.ChatUserRepository;
import tech.limaxs.chat.core.repository.imperative.RoomRepository;
import tech.limaxs.chat.core.repository.imperative.RoomParticipantRepository;
import tech.limaxs.chat.core.repository.imperative.MessageRepository;
import tech.limaxs.chat.infra.auth.JwtPrincipal;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/rooms")
@RunOnVirtualThread
@ApplicationScoped
public class RoomResource {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final ChatUserRepository userRepository;
    private final MessageRepository messageRepository;
    private final JwtPrincipal jwtPrincipal;

    public RoomResource(
            RoomRepository roomRepository,
            RoomParticipantRepository participantRepository,
            ChatUserRepository userRepository,
            MessageRepository messageRepository,
            JwtPrincipal jwtPrincipal) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.jwtPrincipal = jwtPrincipal;
    }

    @GET
    public Response listRooms(@QueryParam("page") @DefaultValue("0") int page,
                              @QueryParam("size") @DefaultValue("20") int size) {
        String tenantId = jwtPrincipal.getTenantId();
        List<Room> rooms = roomRepository.findByTenantId(tenantId, page, size);
        List<RoomResponse> responses = rooms.stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
        return Response.ok(responses).build();
    }

    @POST
    @Transactional
    public Response createRoom(@Valid CreateRoomRequest request) {
        String tenantId = jwtPrincipal.getTenantId();
        UUID currentUserId = jwtPrincipal.getUserId();

        Room room = new Room();
        room.setType(Room.RoomType.valueOf(request.getType()));
        room.setName(request.getName());
        room.setTenantId(tenantId);
        roomRepository.persist(room);

        // Add participants including creator
        List<UUID> allParticipants = new java.util.ArrayList<>(request.getParticipantIds());
        if (!allParticipants.contains(currentUserId)) {
            allParticipants.add(currentUserId);
        }

        for (UUID participantId : allParticipants) {
            // Find or create user
            ChatUser user = Optional.ofNullable(userRepository.findById(participantId))
                    .orElseGet(() -> {
                        ChatUser newUser = new ChatUser();
                        newUser.setId(participantId);
                        newUser.setName("User " + participantId.toString().substring(0, 8));
                        newUser.setTenantId(tenantId);
                        userRepository.persist(newUser);
                        return newUser;
                    });

            RoomParticipant participant = new RoomParticipant();
            participant.setId(new RoomParticipantId(room.getId(), user.getId()));
            participant.setRoom(room);
            participant.setUser(user);
            participant.setRole(RoomParticipant.ParticipantRole.MEMBER);
            participantRepository.persist(participant);
        }

        return Response.created(URI.create("/api/rooms/" + room.getId()))
                .entity(RoomResponse.from(room))
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getRoom(@PathParam("id") UUID id) {
        String tenantId = jwtPrincipal.getTenantId();
        Room room = roomRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Room not found"));
        return Response.ok(RoomResponse.from(room)).build();
    }

    /**
     * GET /api/rooms/{roomId}/messages?page=0&size=50
     * Returns paginated message history for a specific room.
     * Messages are ordered by creation time (newest first).
     *
     * @param roomId The room ID
     * @param page Page number (0-indexed)
     * @param size Number of messages per page
     * @return List of messages in the room
     */
    @GET
    @Path("/{roomId}/messages")
    public Response getRoomMessages(
            @PathParam("roomId") UUID roomId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {
        String tenantId = jwtPrincipal.getTenantId();

        // Verify room belongs to tenant
        roomRepository.findByIdAndTenantId(roomId, tenantId)
                .orElseThrow(() -> new NotFoundException("Room not found"));

        // Get messages for the room
        List<Message> messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, page, size);
        List<MessageResponse> responses = messages.stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
        return Response.ok(responses).build();
    }
}
