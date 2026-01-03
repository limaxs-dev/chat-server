package tech.limaxs.chat.api.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import tech.limaxs.chat.api.websocket.dto.*;
import tech.limaxs.chat.core.repository.reactive.ReactiveRoomParticipantRepository;
import tech.limaxs.chat.infra.redis.RedisService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@WebSocket(path = "/ws/chat")
@ApplicationScoped
public class ChatWebSocketHandler {

    private static final Logger LOG = Logger.getLogger(ChatWebSocketHandler.class.getName());

    private static final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private static final Map<UUID, WebSocketConnection> userConnections = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, WebSocketConnection>> roomConnections = new ConcurrentHashMap<>();

    @Inject
    RedisService redisService;

    @Inject
    ReactiveRoomParticipantRepository roomParticipantRepository;

    @Inject
    PgPool pgPool;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @OnOpen
    public Uni<Void> onOpen(WebSocketConnection connection) {
        String token = extractTokenFromQuery(connection);

        if (token == null || token.isEmpty()) {
            LOG.warning("WebSocket connection rejected: No token provided");
            connection.sendText("{\"error\":\"No token provided\"}");
            connection.close();
            return Uni.createFrom().voidItem();
        }

        try {
            io.jsonwebtoken.JwtParser parser = io.jsonwebtoken.Jwts.parser()
                .verifyWith(readPublicKey())
                .build();

            io.jsonwebtoken.Jws<io.jsonwebtoken.Claims> jws = parser.parseSignedClaims(token);
            io.jsonwebtoken.Claims claims = jws.getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String tenantId = claims.get("tenantId", String.class);
            String name = claims.get("name", String.class);

            UserSession session = new UserSession(userId, tenantId, name);
            sessions.put(connection.id(), session);
            userConnections.put(userId, connection);

            LOG.info("WebSocket opened for user: " + userId + " (" + name + ")");

            sendPresenceEvent(connection, userId, name, true);

            return redisService.updatePresence(userId)
                    .invoke(() -> LOG.info("Presence updated for user: " + userId))
                    .chain(() -> loadUserRooms(userId, connection))
                    .replaceWithVoid();

        } catch (Exception e) {
            LOG.severe("WebSocket connection rejected: Invalid token - " + e.getMessage());
            connection.sendText("{\"error\":\"Invalid token\"}");
            connection.close();
            return Uni.createFrom().voidItem();
        }
    }

    private String extractTokenFromQuery(WebSocketConnection connection) {
        try {
            String queryString = connection.handshakeRequest().query();
            if (queryString != null && !queryString.isEmpty()) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=", 2);
                    if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                        return java.net.URLDecoder.decode(keyValue[1], java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warning("Failed to extract token from query: " + e.getMessage());
        }
        return null;
    }

    private Uni<Void> loadUserRooms(UUID userId, WebSocketConnection connection) {
        return roomParticipantRepository.findByUserId(userId)
                .invoke(participants -> {
                    LOG.info("User " + userId + " is in " + participants.size() + " rooms");
                    for (var participant : participants) {
                        UUID roomId = participant.getId().getRoomId();
                        roomConnections.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                                .put(connection.id(), connection);
                        LOG.info("Added connection to room: " + roomId);
                    }
                })
                .replaceWithVoid();
    }

    private void sendPresenceEvent(WebSocketConnection connection, UUID userId, String name, boolean online) {
        try {
            ObjectNode eventNode = objectMapper.createObjectNode();
            eventNode.put("event", "PRESENCE");
            eventNode.put("traceId", UUID.randomUUID().toString());

            ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("userId", userId.toString());
            dataNode.put("userName", name);
            dataNode.put("status", online ? "online" : "offline");

            eventNode.set("data", dataNode);
            connection.sendText(objectMapper.writeValueAsString(eventNode));
        } catch (Exception e) {
            LOG.warning("Failed to send presence event: " + e.getMessage());
        }
    }

    private java.security.interfaces.RSAPublicKey readPublicKey() {
        try {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("keys/public-key.pem");
            if (is == null) {
                throw new RuntimeException("Public key not found");
            }
            String key = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            key = key.replace("-----BEGIN PUBLIC KEY-----", "")
                     .replace("-----END PUBLIC KEY-----", "")
                     .replaceAll("\\s", "");

            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(
                java.util.Base64.getDecoder().decode(key)
            );
            return (java.security.interfaces.RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read public key", e);
        }
    }

    private UserSession getSession(WebSocketConnection connection) {
        return sessions.get(connection.id());
    }

    @OnTextMessage
    public Uni<String> onMessage(String message, WebSocketConnection connection) {
        UserSession session = getSession(connection);

        if (session == null) {
            LOG.warning("Received message from unauthenticated connection");
            return Uni.createFrom().item("{\"error\":\"Not authenticated\"}");
        }

        UUID userId = session.userId;
        LOG.info("Received message from user: " + userId + ", message: " + message);

        try {
            JsonNode json = objectMapper.readTree(message);
            String eventType = json.has("event") ? json.get("event").asText() : "";
            JsonNode data = json.has("data") ? json.get("data") : null;

            switch (eventType) {
                case "SEND_MSG":
                    return handleSendMessage(data, userId, session.tenantId, connection);

                case "TYPING":
                    return handleTyping(data, userId, session.tenantId, connection)
                            .replaceWith("{\"status\":\"typing_processed\"}");

                case "SIGNAL_SDP":
                    return handleWebRTCSignal(data, userId, session.tenantId, connection)
                            .replaceWith("{\"status\":\"signal_processed\"}");

                case "SIGNAL_ICE":
                    return handleSignalIce(data, userId, session.tenantId, connection)
                            .replaceWith("{\"status\":\"ice_processed\"}");

                case "ACK":
                    return handleAck(data, userId, connection)
                            .replaceWith("{\"status\":\"ack_processed\"}");

                default:
                    LOG.warning("Unknown event type: " + eventType);
                    return Uni.createFrom().item("{\"error\":\"Unknown event\"}");
            }
        } catch (Exception e) {
            LOG.severe("Error parsing message: " + e.getMessage());
            e.printStackTrace();
            return Uni.createFrom().item("{\"error\":\"Invalid message\"}");
        }
    }

    private Uni<String> handleSendMessage(JsonNode data, UUID userId, String tenantId, WebSocketConnection connection) {
        try {
            if (data == null) {
                LOG.warning("SEND_MSG: data is null");
                return Uni.createFrom().item("{\"error\":\"No data\"}");
            }

            UUID roomId = data.has("roomId") ? UUID.fromString(data.get("roomId").asText()) : null;
            String type = data.has("type") ? data.get("type").asText() : "TEXT";
            String contentText = data.has("contentText") ? data.get("contentText").asText() : null;
            String contentMeta = data.has("contentMeta") && !data.get("contentMeta").isNull()
                ? data.get("contentMeta").toString() : null;

            if (roomId == null) {
                LOG.warning("SEND_MSG: roomId is required");
                return Uni.createFrom().item("{\"error\":\"roomId required\"}");
            }

            LOG.info("Handling SEND_MSG: roomId=" + roomId + ", type=" + type + ", userId=" + userId);

            UUID messageId = UUID.randomUUID();
            String insertSql = """
                INSERT INTO messages (id, room_id, sender_id, type, content_text, content_meta, created_at)
                VALUES ($1, $2, $3, $4, $5, $6, now())
                RETURNING id, room_id, sender_id, type, content_text, created_at
                """;

            Tuple params = Tuple.of(messageId, roomId, userId, type,
                contentText != null ? contentText : "", contentMeta);

            LOG.info("Executing SQL: " + insertSql);

            return pgPool.preparedQuery(insertSql).execute(params)
                    .onItem().transform(rowSet -> {
                        LOG.info("SQL executed, rowSet size: " + rowSet.size());
                        if (!rowSet.iterator().hasNext()) {
                            LOG.warning("No rows returned from SQL");
                            return "{\"error\":\"Failed to save message\"}";
                        }

                        var row = rowSet.iterator().next();
                        LOG.info("Message persisted: " + row.getUUID("id"));

                        String eventJson = createNewMessageEvent(
                            row.getUUID("id"),
                            row.getUUID("room_id"),
                            row.getUUID("sender_id"),
                            row.getString("type"),
                            row.getString("content_text"),
                            row.getLocalDateTime("created_at").toString()
                        );

                        // Broadcast to Redis async (fire and forget)
                        redisService.publishToRoom(roomId, eventJson)
                            .subscribe().with(
                                unused -> LOG.info("Published to Redis"),
                                failure -> LOG.severe("Redis publish failed: " + failure.getMessage())
                            );

                        return eventJson;
                    })
                    .onFailure().invoke(e -> {
                        LOG.severe("SQL execution failed: " + e.getMessage());
                        e.printStackTrace();
                    })
                    .onFailure().recoverWithItem(e -> {
                        LOG.severe("Recovered from error: " + e.getMessage());
                        return "{\"error\":\"" + e.getMessage() + "\"}";
                    });

        } catch (Exception e) {
            LOG.severe("Error in handleSendMessage: " + e.getMessage());
            e.printStackTrace();
            return Uni.createFrom().item("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private Uni<Void> handleTyping(JsonNode data, UUID userId, String tenantId, WebSocketConnection connection) {
        try {
            UUID roomId = data.has("roomId") ? UUID.fromString(data.get("roomId").asText()) : null;
            boolean isTyping = data.has("isTyping") ? data.get("isTyping").asBoolean() : false;

            if (roomId != null) {
                LOG.info("Handling TYPING: roomId=" + roomId + ", isTyping=" + isTyping);
                TypingData typingData = new TypingData();
                typingData.setRoomId(roomId);
                typingData.setTyping(isTyping);
                ChatEvent event = new ChatEvent("TYPING", typingData);
                event.setTraceId(UUID.randomUUID());
                return redisService.publishTyping(roomId, objectMapper.writeValueAsString(event));
            }
            return Uni.createFrom().voidItem();
        } catch (Exception e) {
            LOG.severe("Error handling TYPING: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    private Uni<Void> handleWebRTCSignal(JsonNode data, UUID userId, String tenantId, WebSocketConnection connection) {
        try {
            UUID targetId = data.has("targetId") ? UUID.fromString(data.get("targetId").asText()) : null;
            String type = data.has("type") ? data.get("type").asText() : null;
            String sdp = data.has("sdp") ? data.get("sdp").asText() : null;

            if (targetId != null && type != null) {
                LOG.info("Handling SIGNAL_SDP: targetId=" + targetId + ", type=" + type);
                if ("offer".equals(type)) {
                    return redisService.isUserInCall(targetId)
                            .chain(isBusy -> {
                                if (isBusy) {
                                    sendCallRejectedEvent(connection, userId, targetId, "busy", "User is busy");
                                    return Uni.createFrom().voidItem();
                                }
                                return redisService.setCallBusy(userId)
                                        .chain(() -> forwardSignalSdp(targetId, type, sdp));
                            });
                }
                return forwardSignalSdp(targetId, type, sdp);
            }
            return Uni.createFrom().voidItem();
        } catch (Exception e) {
            LOG.severe("Error handling SIGNAL_SDP: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    private Uni<Void> handleSignalIce(JsonNode data, UUID userId, String tenantId, WebSocketConnection connection) {
        try {
            UUID targetId = data.has("targetId") ? UUID.fromString(data.get("targetId").asText()) : null;
            String candidate = data.has("candidate") ? data.get("candidate").asText() : null;

            if (targetId != null && candidate != null) {
                SignalIceData iceData = new SignalIceData();
                iceData.setTargetId(targetId);
                iceData.setCandidate(candidate);
                ChatEvent event = new ChatEvent("SIGNAL_ICE", iceData);
                event.setTraceId(UUID.randomUUID());
                return redisService.publishToUser(targetId, objectMapper.writeValueAsString(event));
            }
            return Uni.createFrom().voidItem();
        } catch (Exception e) {
            LOG.severe("Error handling SIGNAL_ICE: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    private Uni<Void> forwardSignalSdp(UUID targetId, String type, String sdp) {
        try {
            SignalSdpData signalData = new SignalSdpData();
            signalData.setTargetId(targetId);
            signalData.setType(type);
            signalData.setSdp(sdp);
            ChatEvent event = new ChatEvent("SIGNAL_SDP", signalData);
            event.setTraceId(UUID.randomUUID());
            return redisService.publishToUser(targetId, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            LOG.severe("Error forwarding SIGNAL_SDP: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    private void sendCallRejectedEvent(WebSocketConnection connection, UUID callerId, UUID targetId, String status, String reason) {
        try {
            ObjectNode eventNode = objectMapper.createObjectNode();
            eventNode.put("event", "CALL_REJECTED");
            eventNode.put("traceId", UUID.randomUUID().toString());

            ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("callerId", callerId.toString());
            dataNode.put("targetId", targetId.toString());
            dataNode.put("status", status);
            dataNode.put("reason", reason);

            eventNode.set("data", dataNode);
            connection.sendText(objectMapper.writeValueAsString(eventNode));
        } catch (Exception e) {
            LOG.severe("Error sending CALL_REJECTED event: " + e.getMessage());
        }
    }

    private Uni<Void> handleAck(JsonNode data, UUID userId, WebSocketConnection connection) {
        return Uni.createFrom().voidItem();
    }

    private String createNewMessageEvent(UUID id, UUID roomId, UUID senderId, String type, String contentText, String createdAt) {
        try {
            ObjectNode eventNode = objectMapper.createObjectNode();
            eventNode.put("event", "NEW_MESSAGE");
            eventNode.put("traceId", UUID.randomUUID().toString());

            ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("id", id.toString());
            dataNode.put("roomId", roomId.toString());
            dataNode.put("senderId", senderId.toString());
            dataNode.put("type", type);
            if (contentText != null) {
                dataNode.put("contentText", contentText);
            }
            dataNode.put("createdAt", createdAt);

            eventNode.set("data", dataNode);
            return objectMapper.writeValueAsString(eventNode);
        } catch (Exception e) {
            LOG.severe("Error creating NEW_MESSAGE event: " + e.getMessage());
            return "{}";
        }
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        UserSession session = sessions.remove(connection.id());
        if (session != null) {
            UUID userId = session.userId;
            userConnections.remove(userId, connection);
            for (Map<String, WebSocketConnection> roomMap : roomConnections.values()) {
                roomMap.remove(connection.id());
            }
            LOG.info("WebSocket closed for user: " + userId);
            // Clear call state in background (fire and forget)
            redisService.clearCallState(userId).subscribe().with(
                unused -> {},
                failure -> LOG.warning("Failed to clear call state: " + failure.getMessage())
            );
        }
    }

    @OnError
    public void onError(Throwable error) {
        LOG.severe("WebSocket error: " + error.getMessage());
        error.printStackTrace();
    }

    public static void sendToUser(UUID userId, String message) {
        WebSocketConnection connection = userConnections.get(userId);
        if (connection != null) {
            try {
                connection.sendText(message);
            } catch (Exception e) {
                LOG.warning("Failed to send message to user " + userId + ": " + e.getMessage());
            }
        }
    }

    public static void broadcastToRoom(UUID roomId, String message) {
        Map<String, WebSocketConnection> connections = roomConnections.get(roomId);
        if (connections != null) {
            for (WebSocketConnection connection : connections.values()) {
                try {
                    connection.sendText(message);
                } catch (Exception e) {
                    LOG.warning("Failed to broadcast to room " + roomId + ": " + e.getMessage());
                }
            }
        }
    }

    private static record UserSession(UUID userId, String tenantId, String name) {}
}
