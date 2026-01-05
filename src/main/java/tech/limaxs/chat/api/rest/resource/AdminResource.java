package tech.limaxs.chat.api.rest.resource;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import tech.limaxs.chat.api.rest.dto.ArchiveSummaryResponse;
import tech.limaxs.chat.core.service.RoomArchiveService;
import tech.limaxs.chat.infra.auth.JwtPrincipal;

import java.util.UUID;

@Path("/api/back/admin")
@RunOnVirtualThread
@ApplicationScoped
public class AdminResource {

    private final RoomArchiveService archiveService;
    private final JwtPrincipal jwtPrincipal;

    public AdminResource(
            RoomArchiveService archiveService,
            JwtPrincipal jwtPrincipal) {
        this.archiveService = archiveService;
        this.jwtPrincipal = jwtPrincipal;
    }

    // POST /api/admin/archive/{roomId} - Trigger archive
    @POST
    @Path("/archive/{roomId}")
    public Response archiveRoom(@PathParam("roomId") UUID roomId) {
        String adminUserId = jwtPrincipal.getUserId().toString();
        ArchiveSummaryResponse summary = archiveService.archiveRoom(roomId, adminUserId);
        return Response.status(Response.Status.CREATED).entity(summary).build();
    }

    // GET /api/admin/archived/{roomId} - Check if room is archived
    @GET
    @Path("/archived/{roomId}")
    public Response isRoomArchived(@PathParam("roomId") UUID roomId) {
        boolean archived = archiveService.isRoomArchived(roomId);
        return Response.ok(new ArchivedCheckResponse(roomId, archived)).build();
    }

    // Simple response for archive check
    @io.quarkus.runtime.annotations.RegisterForReflection
    public static class ArchivedCheckResponse {
        private UUID roomId;
        private boolean archived;

        public ArchivedCheckResponse(UUID roomId, boolean archived) {
            this.roomId = roomId;
            this.archived = archived;
        }

        public UUID getRoomId() {
            return roomId;
        }

        public void setRoomId(UUID roomId) {
            this.roomId = roomId;
        }

        public boolean isArchived() {
            return archived;
        }

        public void setArchived(boolean archived) {
            this.archived = archived;
        }
    }
}
