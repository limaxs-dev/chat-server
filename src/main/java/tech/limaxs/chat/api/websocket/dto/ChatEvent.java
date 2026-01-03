package tech.limaxs.chat.api.websocket.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.UUID;

@RegisterForReflection
public class ChatEvent {
    private String event;
    private UUID traceId;
    private Object data;

    public ChatEvent() {
        this.traceId = UUID.randomUUID();
    }

    public ChatEvent(String event, Object data) {
        this.event = event;
        this.traceId = UUID.randomUUID();
        this.data = data;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public UUID getTraceId() {
        return traceId;
    }

    public void setTraceId(UUID traceId) {
        this.traceId = traceId;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
