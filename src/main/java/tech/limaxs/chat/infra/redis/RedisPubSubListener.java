package tech.limaxs.chat.infra.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import tech.limaxs.chat.api.websocket.handler.ChatWebSocketHandler;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Redis Pub/Sub listener for cross-node message distribution.
 * Subscribes to Redis channels and forwards messages to WebSocket clients.
 *
 * Channels:
 * - chat:room:{room_id} - Messages, edits, deletes for room members
 * - signal:user:{user_id} - WebRTC signaling (SDP/ICE) for P2P calls
 * - typing:room:{room_id} - Typing indicators for room members
 * - presence:room:{room_id} - User online/offline events for room members
 */
@ApplicationScoped
public class RedisPubSubListener {

    private static final Logger LOG = Logger.getLogger(RedisPubSubListener.class.getName());

    private final ReactiveRedisDataSource redisDataSource;
    private final ObjectMapper objectMapper;

    public RedisPubSubListener(ReactiveRedisDataSource redisDataSource) {
        this.redisDataSource = redisDataSource;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Start Redis subscriptions on application startup.
     * Uses pattern matching to subscribe to relevant channels.
     */
    void onStart(@Observes StartupEvent event) {
        LOG.info("Starting Redis Pub/Sub subscriptions...");

        // Subscribe to all chat room channels
        redisDataSource.pubsub(String.class)
                .subscribe("chat:room:*")
                .subscribe()
                .with(
                        message -> {
                            LOG.fine("Received room message on channel: " + message);
                            handleRoomMessage(message);
                        },
                        failure -> {
                            LOG.severe("Failed to subscribe to room channels: " + failure.getMessage());
                            failure.printStackTrace();
                        },
                        () -> LOG.info("Successfully subscribed to room channels")
                );

        // Subscribe to all user signaling channels
        redisDataSource.pubsub(String.class)
                .subscribe("signal:user:*")
                .subscribe()
                .with(
                        message -> {
                            LOG.fine("Received signal message on channel: " + message);
                            handleUserSignal(message);
                        },
                        failure -> {
                            LOG.severe("Failed to subscribe to signal channels: " + failure.getMessage());
                            failure.printStackTrace();
                        },
                        () -> LOG.info("Successfully subscribed to signal channels")
                );

        // Subscribe to all typing channels
        redisDataSource.pubsub(String.class)
                .subscribe("typing:room:*")
                .subscribe()
                .with(
                        message -> {
                            LOG.fine("Received typing message on channel: " + message);
                            handleTypingMessage(message);
                        },
                        failure -> {
                            LOG.severe("Failed to subscribe to typing channels: " + failure.getMessage());
                            failure.printStackTrace();
                        },
                        () -> LOG.info("Successfully subscribed to typing channels")
                );

        // Subscribe to all presence channels
        redisDataSource.pubsub(String.class)
                .subscribe("presence:room:*")
                .subscribe()
                .with(
                        message -> {
                            LOG.fine("Received presence message on channel: " + message);
                            handlePresenceMessage(message);
                        },
                        failure -> {
                            LOG.severe("Failed to subscribe to presence channels: " + failure.getMessage());
                            failure.printStackTrace();
                        },
                        () -> LOG.info("Successfully subscribed to presence channels")
                );

        LOG.info("Redis Pub/Sub listener started");
    }

    /**
     * Handle messages from room channels (chat:room:{room_id}).
     * These include NEW_MESSAGE, MESSAGE_EDITED, MESSAGE_DELETED events.
     * Forward to all WebSocket connections in the room.
     */
    private void handleRoomMessage(String message) {
        try {
            // Parse the message to extract room ID
            JsonNode json = objectMapper.readTree(message);
            if (json.has("data") && json.get("data").has("roomId")) {
                String roomId = json.get("data").get("roomId").asText();
                LOG.info("Forwarding room message to room: " + roomId);
                ChatWebSocketHandler.broadcastToRoom(UUID.fromString(roomId), message);
            }
        } catch (Exception e) {
            LOG.severe("Error handling room message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle WebRTC signaling messages (signal:user:{user_id}).
     * These include SIGNAL_SDP and SIGNAL_ICE events.
     * Forward to the specific target user's WebSocket connections.
     */
    private void handleUserSignal(String message) {
        try {
            // Parse the message to get the event type and target user
            JsonNode json = objectMapper.readTree(message);
            String eventType = json.has("event") ? json.get("event").asText() : "";

            // For SIGNAL_SDP and SIGNAL_ICE, the target is in data.targetId
            if (json.has("data") && json.get("data").has("targetId")) {
                String userId = json.get("data").get("targetId").asText();
                LOG.info("Forwarding " + eventType + " to user: " + userId);
                ChatWebSocketHandler.sendToUser(UUID.fromString(userId), message);
            }
        } catch (Exception e) {
            LOG.severe("Error handling user signal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle typing indicator messages (typing:room:{room_id}).
     * Forward TYPING events to all WebSocket connections in the room.
     */
    private void handleTypingMessage(String message) {
        try {
            // Parse the message to extract room ID
            JsonNode json = objectMapper.readTree(message);
            if (json.has("data") && json.get("data").has("roomId")) {
                String roomId = json.get("data").get("roomId").asText();
                LOG.fine("Forwarding typing indicator to room: " + roomId);
                ChatWebSocketHandler.broadcastToRoom(UUID.fromString(roomId), message);
            }
        } catch (Exception e) {
            LOG.severe("Error handling typing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle presence messages (presence:room:{room_id}).
     * Forward PRESENCE events to all WebSocket connections in the room.
     */
    private void handlePresenceMessage(String message) {
        try {
            // Parse the message to extract room ID
            JsonNode json = objectMapper.readTree(message);
            if (json.has("data") && json.get("data").has("roomId")) {
                String roomId = json.get("data").get("roomId").asText();
                LOG.info("Forwarding PRESENCE event to room: " + roomId);
                ChatWebSocketHandler.broadcastToRoom(UUID.fromString(roomId), message);
            }
        } catch (Exception e) {
            LOG.severe("Error handling presence message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
