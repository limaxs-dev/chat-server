package tech.limaxs.chat.api.websocket.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.UUID;

/**
 * DTO for CALL_REJECTED WebSocket event.
 * Sent by server when rejecting a call offer due to the target user being busy.
 */
@RegisterForReflection
public class CallRejectedData {
    private UUID callerId;
    private UUID targetId;
    private String status; // "busy"
    private String reason;

    public UUID getCallerId() {
        return callerId;
    }

    public void setCallerId(UUID callerId) {
        this.callerId = callerId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
