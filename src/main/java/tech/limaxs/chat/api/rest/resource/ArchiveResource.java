package tech.limaxs.chat.api.rest.resource;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import tech.limaxs.chat.api.rest.dto.ArchivedMessageResponse;
import tech.limaxs.chat.api.rest.dto.ArchivedRoomResponse;
import tech.limaxs.chat.core.model.ArchivedMessage;
import tech.limaxs.chat.core.model.ArchivedParticipant;
import tech.limaxs.chat.core.model.ArchivedRoom;
import tech.limaxs.chat.core.repository.imperative.ArchivedMessageRepository;
import tech.limaxs.chat.core.repository.imperative.ArchivedParticipantRepository;
import tech.limaxs.chat.core.repository.imperative.ArchivedRoomRepository;
import tech.limaxs.chat.infra.auth.JwtPrincipal;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/back/archive")
@RunOnVirtualThread
@ApplicationScoped
public class ArchiveResource {

    private final ArchivedRoomRepository archivedRoomRepository;
    private final ArchivedMessageRepository archivedMessageRepository;
    private final ArchivedParticipantRepository archivedParticipantRepository;
    private final JwtPrincipal jwtPrincipal;

    public ArchiveResource(
            ArchivedRoomRepository archivedRoomRepository,
            ArchivedMessageRepository archivedMessageRepository,
            ArchivedParticipantRepository archivedParticipantRepository,
            JwtPrincipal jwtPrincipal) {
        this.archivedRoomRepository = archivedRoomRepository;
        this.archivedMessageRepository = archivedMessageRepository;
        this.archivedParticipantRepository = archivedParticipantRepository;
        this.jwtPrincipal = jwtPrincipal;
    }

    // GET /api/back/archive/rooms - List archived rooms
    @GET
    @Path("/rooms")
    public Response listArchivedRooms(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        List<ArchivedRoom> archivedRooms = archivedRoomRepository
                .findAllOrderByArchivedAtDesc(page, size);

        List<ArchivedRoomResponse> responses = archivedRooms.stream()
                .map(ArchivedRoomResponse::from)
                .collect(Collectors.toList());

        return Response.ok(responses).build();
    }

    // GET /api/back/archive/rooms/{archivedRoomId} - Get archived room details
    @GET
    @Path("/rooms/{archivedRoomId}")
    public Response getArchivedRoom(@PathParam("archivedRoomId") UUID archivedRoomId) {
        ArchivedRoom archivedRoom = archivedRoomRepository.findById(archivedRoomId);
        if (archivedRoom == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Archived room not found").build();
        }

        return Response.ok(ArchivedRoomResponse.from(archivedRoom)).build();
    }

    // GET /api/back/archive/rooms/{archivedRoomId}/messages - Get archived messages
    @GET
    @Path("/rooms/{archivedRoomId}/messages")
    public Response getArchivedMessages(
            @PathParam("archivedRoomId") UUID archivedRoomId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {

        // Verify archived room exists
        ArchivedRoom archivedRoom = archivedRoomRepository.findById(archivedRoomId);
        if (archivedRoom == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Archived room not found").build();
        }

        List<ArchivedMessage> messages = archivedMessageRepository
                .findByArchivedRoomIdOrderByCreatedAtAsc(archivedRoomId, page, size);

        List<ArchivedMessageResponse> responses = messages.stream()
                .map(ArchivedMessageResponse::from)
                .collect(Collectors.toList());

        return Response.ok(responses).build();
    }

    // GET /api/back/archive/rooms/{archivedRoomId}/participants - Get archived participants
    @GET
    @Path("/rooms/{archivedRoomId}/participants")
    public Response getArchivedParticipants(@PathParam("archivedRoomId") UUID archivedRoomId) {
        // Verify archived room exists
        ArchivedRoom archivedRoom = archivedRoomRepository.findById(archivedRoomId);
        if (archivedRoom == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Archived room not found").build();
        }

        List<ArchivedParticipant> participants = archivedParticipantRepository
                .findByArchivedRoomId(archivedRoomId);

        return Response.ok(participants).build();
    }

    // GET /api/back/archive/search/by-original-room/{originalRoomId} - Find archived room by original ID
    @GET
    @Path("/search/by-original-room/{originalRoomId}")
    public Response findByOriginalRoomId(@PathParam("originalRoomId") UUID originalRoomId) {
        ArchivedRoom archivedRoom = archivedRoomRepository.findByOriginalRoomId(originalRoomId);
        if (archivedRoom == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Room not found in archive (may not be archived yet)").build();
        }

        return Response.ok(ArchivedRoomResponse.from(archivedRoom)).build();
    }
}
