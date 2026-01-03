package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import tech.limaxs.chat.core.model.ArchivedRoom;

import java.time.LocalDateTime;
import java.util.UUID;

@RegisterForReflection
public class ArchivedRoomResponse {
    private UUID id;
    private UUID originalRoomId;
    private String type;
    private String name;
    private int participantCount;
    private int messageCount;
    private LocalDateTime firstMessageAt;
    private LocalDateTime lastMessageAt;
    private LocalDateTime archivedAt;
    private String archivedBy;

    public ArchivedRoomResponse() {
    }

    public static ArchivedRoomResponse from(ArchivedRoom archivedRoom) {
        ArchivedRoomResponse response = new ArchivedRoomResponse();
        response.id = archivedRoom.getId();
        response.originalRoomId = archivedRoom.getOriginalRoomId();
        response.type = archivedRoom.getType();
        response.name = archivedRoom.getName();
        response.participantCount = archivedRoom.getParticipantCount();
        response.messageCount = archivedRoom.getMessageCount();
        response.firstMessageAt = archivedRoom.getFirstMessageAt();
        response.lastMessageAt = archivedRoom.getLastMessageAt();
        response.archivedAt = archivedRoom.getArchivedAt();
        response.archivedBy = archivedRoom.getArchivedBy();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOriginalRoomId() {
        return originalRoomId;
    }

    public void setOriginalRoomId(UUID originalRoomId) {
        this.originalRoomId = originalRoomId;
    }

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

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public LocalDateTime getFirstMessageAt() {
        return firstMessageAt;
    }

    public void setFirstMessageAt(LocalDateTime firstMessageAt) {
        this.firstMessageAt = firstMessageAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public String getArchivedBy() {
        return archivedBy;
    }

    public void setArchivedBy(String archivedBy) {
        this.archivedBy = archivedBy;
    }
}
