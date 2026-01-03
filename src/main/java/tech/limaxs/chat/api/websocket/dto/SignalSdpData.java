package tech.limaxs.chat.api.websocket.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.UUID;

@RegisterForReflection
public class SignalSdpData {
    private UUID targetId;
    private String type;
    private String sdp;

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }
}
