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

@Path("/api/archive")
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

    // GET /api/archive/rooms - List archived rooms
    @GET
    @Path("/rooms")
    public Response listArchivedRooms(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        String tenantId = jwtPrincipal.getTenantId();
        List<ArchivedRoom> archivedRooms = archivedRoomRepository
                .findByTenantIdOrderByArchivedAtDesc(tenantId, page, size);

        List<ArchivedRoomResponse> responses = archivedRooms.stream()
                .map(ArchivedRoomResponse::from)
                .collect(Collectors.toList());

        return Response.ok(responses).build();
    }

    // GET /api/archive/rooms/{archivedRoomId} - Get archived room details
    @GET
    @Path("/rooms/{archivedRoomId}")
    public Response getArchivedRoom(@PathParam("archivedRoomId") UUID archivedRoomId) {
        ArchivedRoom archivedRoom = archivedRoomRepository.findById(archivedRoomId);
        if (archivedRoom == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Archived room not found").build();
        }

        // Verify tenant access
        String tenantId = jwtPrincipal.getTenantId();
        if (!archivedRoom.getTenantId().equals(tenantId)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
        }

        return Response.ok(ArchivedRoomResponse.from(archivedRoom)).build();
    }

    // GET /api/archive/rooms/{archivedRoomId}/messages - Get archived messages
    @GET
    @Path("/rooms/{archivedRoomId}/messages")
    public Response getArchivedMessages(
            @PathParam("archivedRoomId") UUID archivedRoomId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {

        // Verify archived room exists and tenant has access
        ArchivedRoom archivedRoom = archivedRoomRepository.findById(archivedRoomId);
        if (archivedRoom == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Archived room not found").build();
        }

        String tenantId = jwtPrincipal.getTenantId();
        if (!archivedRoom.getTenantId().equals(tenantId)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
        }

        List<ArchivedMessage> messages = archivedMessageRepository
                .findByArchivedRoomIdOrderByCreatedAtAsc(archivedRoomId, page, size);

        List<ArchivedMessageResponse> responses = messages.stream()
                .map(ArchivedMessageResponse::from)
                .collect(Collectors.toList());

        return Response.ok(responses).build();
    }

    // GET /api/archive/rooms/{archivedRoomId}/participants - Get archived participants
    @GET
    @Path("/rooms/{archivedRoomId}/participants")
    public Response getArchivedParticipants(@PathParam("archivedRoomId") UUID archivedRoomId) {
        // Verify archived room exists and tenant has access
        ArchivedRoom archivedRoom = archivedRoomRepository.findById(archivedRoomId);
        if (archivedRoom == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Archived room not found").build();
        }

        String tenantId = jwtPrincipal.getTenantId();
        if (!archivedRoom.getTenantId().equals(tenantId)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
        }

        List<ArchivedParticipant> participants = archivedParticipantRepository
                .findByArchivedRoomId(archivedRoomId);

        return Response.ok(participants).build();
    }

    // GET /api/archive/search/by-original-room/{originalRoomId} - Find archived room by original ID
    @GET
    @Path("/search/by-original-room/{originalRoomId}")
    public Response findByOriginalRoomId(@PathParam("originalRoomId") UUID originalRoomId) {
        String tenantId = jwtPrincipal.getTenantId();

        ArchivedRoom archivedRoom = archivedRoomRepository.findByOriginalRoomId(originalRoomId);
        if (archivedRoom == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Room not found in archive (may not be archived yet)").build();
        }

        // Verify tenant access
        if (!archivedRoom.getTenantId().equals(tenantId)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
        }

        return Response.ok(ArchivedRoomResponse.from(archivedRoom)).build();
    }
}
