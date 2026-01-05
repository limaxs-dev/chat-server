package tech.limaxs.chat.api.rest.resource;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.limaxs.chat.api.rest.dto.IceServerConfig;
import tech.limaxs.chat.api.rest.dto.WebRtcConfigResponse;

import java.util.List;

/**
 * Configuration endpoints for client applications.
 */
@Path("/api/front/config")
@RunOnVirtualThread
@ApplicationScoped
public class ConfigResource {

    /**
     * GET /api/config/webrtc
     * Returns WebRTC ICE server configuration (STUN/TURN servers).
     *
     * Currently hardcoded with Google's public STUN server.
     * Structure is ready for future CoTurn integration with credentials.
     *
     * @return WebRtcConfigResponse containing ICE server configurations
     */
    @GET
    @Path("/webrtc")
    @Produces(MediaType.APPLICATION_JSON)
    public WebRtcConfigResponse getWebRtcConfig() {
        // Google STUN server (public, no credentials required)
        IceServerConfig stunServer = new IceServerConfig(
            new String[]{"stun:stun1.l.google.com:19302"}
        );

        // Additional public STUN servers for redundancy
        IceServerConfig stunServer2 = new IceServerConfig(
            new String[]{"stun:stun2.l.google.com:19302"}
        );

        // TURN server example (commented out - ready for CoTurn integration)
        // IceServerConfig turnServer = new IceServerConfig(
        //     new String[]{"turn:your-turn-server.com:3478"},
        //     "username",
        //     "credential"
        // );

        return new WebRtcConfigResponse(
            List.of(stunServer, stunServer2)
        );
    }
}
