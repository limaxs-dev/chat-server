package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDateTime;
import java.util.UUID;

@RegisterForReflection
public class ArchiveSummaryResponse {
    private UUID archivedRoomId;
    private UUID originalRoomId;
    private int messagesArchived;
    private int participantsArchived;
    private LocalDateTime archivedAt;

    public ArchiveSummaryResponse() {
    }

    public UUID getArchivedRoomId() {
        return archivedRoomId;
    }

    public void setArchivedRoomId(UUID archivedRoomId) {
        this.archivedRoomId = archivedRoomId;
    }

    public UUID getOriginalRoomId() {
        return originalRoomId;
    }

    public void setOriginalRoomId(UUID originalRoomId) {
        this.originalRoomId = originalRoomId;
    }

    public int getMessagesArchived() {
        return messagesArchived;
    }

    public void setMessagesArchived(int messagesArchived) {
        this.messagesArchived = messagesArchived;
    }

    public int getParticipantsArchived() {
        return participantsArchived;
    }

    public void setParticipantsArchived(int participantsArchived) {
        this.participantsArchived = participantsArchived;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }
}
