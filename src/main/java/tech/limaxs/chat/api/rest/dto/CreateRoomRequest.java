package tech.limaxs.chat.api.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@RegisterForReflection
public class CreateRoomRequest {

    @JsonProperty("type")
    @NotBlank
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("participantIds")
    @NotNull
    private List<UUID> participantIds;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UUID> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<UUID> participantIds) {
        this.participantIds = participantIds;
    }
}
