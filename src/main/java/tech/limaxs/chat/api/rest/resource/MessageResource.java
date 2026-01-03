package tech.limaxs.chat.api.rest.resource;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import tech.limaxs.chat.api.rest.dto.SendMessageRequest;
import tech.limaxs.chat.api.rest.dto.MessageResponse;
import tech.limaxs.chat.core.model.Message;
import tech.limaxs.chat.core.model.Room;
import tech.limaxs.chat.core.model.ChatUser;
import tech.limaxs.chat.core.repository.imperative.MessageRepository;
import tech.limaxs.chat.core.repository.imperative.RoomRepository;
import tech.limaxs.chat.core.repository.imperative.ChatUserRepository;
import tech.limaxs.chat.infra.auth.JwtPrincipal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/messages")
@RunOnVirtualThread
@ApplicationScoped
public class MessageResource {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final ChatUserRepository userRepository;
    private final JwtPrincipal jwtPrincipal;

    public MessageResource(
            MessageRepository messageRepository,
            RoomRepository roomRepository,
            ChatUserRepository userRepository,
            JwtPrincipal jwtPrincipal) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.jwtPrincipal = jwtPrincipal;
    }

    @GET
    @Path("/{roomId}")
    public Response getMessages(@PathParam("roomId") UUID roomId,
                                @QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("50") int size) {
        String tenantId = jwtPrincipal.getTenantId();

        // Verify room belongs to tenant
        roomRepository.findByIdAndTenantId(roomId, tenantId)
                .orElseThrow(() -> new NotFoundException("Room not found"));

        List<Message> messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, page, size);
        List<MessageResponse> responses = messages.stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
        return Response.ok(responses).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response sendMessage(SendMessageRequest request) {
        UUID currentUserId = jwtPrincipal.getUserId();

        Room room = Optional.ofNullable(roomRepository.findById(request.getRoomId()))
                .orElseThrow(() -> new NotFoundException("Room not found"));

        // Find or create sender
        ChatUser sender = Optional.ofNullable(userRepository.findById(currentUserId))
                .orElseGet(() -> {
                    ChatUser newUser = new ChatUser();
                    newUser.setId(currentUserId);
                    newUser.setName("User " + currentUserId.toString().substring(0, 8));
                    newUser.setTenantId(room.getTenantId());
                    userRepository.persist(newUser);
                    return newUser;
                });

        // Check idempotency
        if (request.getClientRef() != null) {
            messageRepository.findByClientRef(request.getClientRef())
                    .ifPresent(existing -> {
                        throw new WebApplicationException("Duplicate client ref", 409);
                    });
        }

        Message message = new Message();
        message.setRoom(room);
        message.setSender(sender);
        message.setType(Message.MessageType.valueOf(request.getType()));
        message.setContentText(request.getContentText());
        message.setContentMeta(request.getContentMeta());
        message.setClientRef(request.getClientRef());
        messageRepository.persist(message);

        return Response.status(Response.Status.CREATED)
                .entity(MessageResponse.from(message))
                .build();
    }
}
