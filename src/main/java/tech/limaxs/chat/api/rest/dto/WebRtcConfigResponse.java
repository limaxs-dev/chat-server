package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

/**
 * WebRTC configuration response containing ICE server configurations.
 * Returns STUN and TURN servers for WebRTC peer connection establishment.
 */
@RegisterForReflection
public class WebRtcConfigResponse {
    private List<IceServerConfig> iceServers;

    public WebRtcConfigResponse() {
    }

    public WebRtcConfigResponse(List<IceServerConfig> iceServers) {
        this.iceServers = iceServers;
    }

    public List<IceServerConfig> getIceServers() {
        return iceServers;
    }

    public void setIceServers(List<IceServerConfig> iceServers) {
        this.iceServers = iceServers;
    }
}
