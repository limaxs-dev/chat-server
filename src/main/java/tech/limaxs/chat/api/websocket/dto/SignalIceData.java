package tech.limaxs.chat.api.websocket.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.UUID;

/**
 * DTO for SIGNAL_ICE WebSocket event.
 * Used for ICE candidate exchange between peers during WebRTC connection establishment.
 * The server acts as a pure relay, forwarding the candidate from sender to targetId.
 */
@RegisterForReflection
public class SignalIceData {
    private UUID targetId;
    private String candidate;
    private String sdpMid;
    private int sdpMLineIndex;

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }

    public String getSdpMid() {
        return sdpMid;
    }

    public void setSdpMid(String sdpMid) {
        this.sdpMid = sdpMid;
    }

    public int getSdpMLineIndex() {
        return sdpMLineIndex;
    }

    public void setSdpMLineIndex(int sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }
}
