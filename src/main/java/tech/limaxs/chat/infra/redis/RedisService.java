package tech.limaxs.chat.infra.redis;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class RedisService {

    private final ReactiveRedisDataSource redisDataSource;
    private ReactiveValueCommands<String, String> valueCommands;
    private ReactivePubSubCommands<String> pubSubCommands;
    private static final Logger LOG = Logger.getLogger(RedisService.class.getName());

    public RedisService(ReactiveRedisDataSource redisDataSource) {
        this.redisDataSource = redisDataSource;
        // Initialize commands lazily
        this.valueCommands = null;
        this.pubSubCommands = null;
    }

    private ReactiveValueCommands<String, String> getValueCommands() {
        if (valueCommands == null) {
            this.valueCommands = redisDataSource.value(String.class);
        }
        return valueCommands;
    }

    private ReactivePubSubCommands<String> getPubSubCommands() {
        if (pubSubCommands == null) {
            this.pubSubCommands = redisDataSource.pubsub(String.class);
        }
        return pubSubCommands;
    }

    // Presence: user:presence:{user_id} -> "online", TTL 60s
    public Uni<Void> updatePresence(UUID userId) {
        return getValueCommands().setex("user:presence:" + userId, 60, "online").replaceWithVoid();
    }

    public Uni<String> getPresence(UUID userId) {
        return getValueCommands().get("user:presence:" + userId);
    }

    // Unread count: unread:{room_id}:{user_id} -> Integer
    public Uni<Long> incrementUnread(UUID roomId, UUID userId) {
        return getValueCommands().incr("unread:" + roomId + ":" + userId);
    }

    public Uni<Void> resetUnread(UUID roomId, UUID userId) {
        return getValueCommands().set("unread:" + roomId + ":" + userId, "0").replaceWithVoid();
    }

    public Uni<Long> getUnreadCount(UUID roomId, UUID userId) {
        return getValueCommands().get("unread:" + roomId + ":" + userId)
                .onItem().ifNull().continueWith("0")
                .map(Long::parseLong);
    }

    // Pub/Sub: chat:room:{room_id} - messages, edits, deletes
    public Uni<Void> publishToRoom(UUID roomId, String message) {
        String channel = "chat:room:" + roomId.toString();
        LOG.info("Publishing to room channel: " + channel);
        return getPubSubCommands().publish(channel, message).replaceWithVoid();
    }

    // Pub/Sub: signal:user:{user_id} - WebRTC signaling (P2P)
    public Uni<Void> publishToUser(UUID userId, String message) {
        String channel = "signal:user:" + userId.toString();
        LOG.info("Publishing to user channel: " + channel);
        return getPubSubCommands().publish(channel, message).replaceWithVoid();
    }

    // Pub/Sub: typing:room:{room_id} - typing indicators
    public Uni<Void> publishTyping(UUID roomId, String message) {
        String channel = "typing:room:" + roomId.toString();
        LOG.info("Publishing to typing channel: " + channel);
        return getPubSubCommands().publish(channel, message).replaceWithVoid();
    }

    // Pub/Sub: presence:room:{room_id} - user online/offline events for room members
    public Uni<Void> publishPresence(UUID roomId, String message) {
        String channel = "presence:room:" + roomId.toString();
        LOG.info("Publishing to presence channel: " + channel);
        return getPubSubCommands().publish(channel, message).replaceWithVoid();
    }

    // Call State: user:call:{user_id} -> "busy", TTL 300s (5 minutes)
    public Uni<Void> setCallBusy(UUID userId) {
        return getValueCommands().setex("user:call:" + userId, 300, "busy").replaceWithVoid();
    }

    public Uni<Void> clearCallState(UUID userId) {
        return getValueCommands().set("user:call:" + userId, "idle").replaceWithVoid();
    }

    public Uni<String> getCallState(UUID userId) {
        return getValueCommands().get("user:call:" + userId)
                .onItem().ifNull().continueWith("idle");
    }

    public Uni<Boolean> isUserInCall(UUID userId) {
        return getCallState(userId)
                .map(state -> "busy".equals(state));
    }
}
