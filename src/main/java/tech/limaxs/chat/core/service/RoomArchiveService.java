package tech.limaxs.chat.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import tech.limaxs.chat.api.rest.dto.ArchiveSummaryResponse;
import tech.limaxs.chat.core.model.*;
import tech.limaxs.chat.core.repository.imperative.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class RoomArchiveService {

    private static final Logger LOG = Logger.getLogger(RoomArchiveService.class.getName());

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final RoomParticipantRepository participantRepository;
    private final ChatUserRepository userRepository;
    private final ArchivedRoomRepository archivedRoomRepository;
    private final ArchivedMessageRepository archivedMessageRepository;
    private final ArchivedParticipantRepository archivedParticipantRepository;
    private final EntityManager entityManager;

    public RoomArchiveService(
            RoomRepository roomRepository,
            MessageRepository messageRepository,
            RoomParticipantRepository participantRepository,
            ChatUserRepository userRepository,
            ArchivedRoomRepository archivedRoomRepository,
            ArchivedMessageRepository archivedMessageRepository,
            ArchivedParticipantRepository archivedParticipantRepository,
            EntityManager entityManager) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.archivedRoomRepository = archivedRoomRepository;
        this.archivedMessageRepository = archivedMessageRepository;
        this.archivedParticipantRepository = archivedParticipantRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public ArchiveSummaryResponse archiveRoom(UUID roomId, String adminUserId) {
        LOG.info("Starting archive for room: " + roomId + " by admin: " + adminUserId);

        // 1. Get room with all data
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new jakarta.ws.rs.NotFoundException("Room not found: " + roomId);
        }

        // 2. Get all messages for statistics
        List<Message> messages = messageRepository.findByRoomId(roomId);
        LocalDateTime firstMessageAt = messages.stream()
                .map(Message::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime lastMessageAt = messages.stream()
                .map(Message::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // 3. Get all participants
        List<RoomParticipant> participants = participantRepository.findByRoomId(roomId);

        // Extract data we need before detaching
        String roomType = room.getType().name();
        String roomName = room.getName();
        UUID roomUuid = room.getId();

        // Clear entity references to avoid cascade issues
        entityManager.clear();
        LOG.info("Cleared EntityManager to avoid cascade issues");

        // 4. Create archived_room record
        ArchivedRoom archivedRoom = new ArchivedRoom();
        archivedRoom.setOriginalRoomId(roomUuid);
        archivedRoom.setType(roomType);
        archivedRoom.setName(roomName);
        archivedRoom.setParticipantCount(participants.size());
        archivedRoom.setMessageCount(messages.size());
        archivedRoom.setFirstMessageAt(firstMessageAt);
        archivedRoom.setLastMessageAt(lastMessageAt);
        archivedRoom.setArchivedBy(adminUserId);
        archivedRoomRepository.persist(archivedRoom);
        LOG.info("Created archived room record: " + archivedRoom.getId());

        // 5. Copy all messages to archived_messages
        for (Message message : messages) {
            ArchivedMessage archivedMessage = new ArchivedMessage();
            archivedMessage.setArchivedRoomId(archivedRoom.getId());
            archivedMessage.setOriginalRoomId(roomUuid);
            archivedMessage.setOriginalMessageId(message.getId());
            archivedMessage.setSenderId(message.getSenderId());
            archivedMessage.setSenderName(getUserName(message.getSenderId()));
            archivedMessage.setType(message.getType().name());
            archivedMessage.setContentText(message.getContentText());
            archivedMessage.setContentMeta(message.getContentMeta());
            archivedMessage.setCreatedAt(message.getCreatedAt());
            archivedMessageRepository.persist(archivedMessage);
        }
        LOG.info("Archived " + messages.size() + " messages");

        // 6. Copy all participants to archived_participants
        for (RoomParticipant participant : participants) {
            ArchivedParticipant archivedParticipant = new ArchivedParticipant();
            archivedParticipant.setArchivedRoomId(archivedRoom.getId());
            UUID userId = participant.getId().getUserId();
            archivedParticipant.setUserId(userId);
            archivedParticipant.setUserName(getUserName(userId));
            archivedParticipant.setRole(participant.getRole().name());
            archivedParticipant.setJoinedAt(participant.getJoinedAt());
            archivedParticipantRepository.persist(archivedParticipant);
        }
        LOG.info("Archived " + participants.size() + " participants");

        // 7. Delete from main tables in proper order (respect FK constraints)
        // First delete all messages for this room
        messageRepository.deleteByRoomId(roomUuid);
        LOG.info("Deleted messages from main table for room: " + roomUuid);

        // Then delete participants
        participantRepository.deleteByRoomId(roomUuid);
        LOG.info("Deleted participants from main table for room: " + roomUuid);

        // Finally delete the room
        roomRepository.deleteById(roomUuid);
        LOG.info("Deleted room from main table: " + roomUuid);

        // 8. Return archive summary
        ArchiveSummaryResponse summary = new ArchiveSummaryResponse();
        summary.setArchivedRoomId(archivedRoom.getId());
        summary.setOriginalRoomId(roomUuid);
        summary.setMessagesArchived(messages.size());
        summary.setParticipantsArchived(participants.size());
        summary.setArchivedAt(archivedRoom.getArchivedAt());

        LOG.info("Archive complete for room: " + roomUuid);
        return summary;
    }

    public boolean isRoomArchived(UUID roomId) {
        return archivedRoomRepository.findByOriginalRoomId(roomId) != null;
    }

    private String getUserName(UUID userId) {
        ChatUser user = userRepository.findById(userId);
        if (user != null) {
            return user.getName();
        }
        return "Unknown User (" + userId.toString().substring(0, 8) + ")";
    }
}
