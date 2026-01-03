# NexusChat Engine - Project Structure Summary

> Quick reference for bug fixing and code navigation.

## Architecture Overview

- **Framework**: Quarkus 3.30.5 + Java 21
- **Dual Concurrency**:
  - REST: Virtual Threads (`@RunOnVirtualThread`) + Hibernate ORM (JDBC)
  - WebSocket: Reactive (Mutiny) + Hibernate Reactive
- **Cross-Node**: Redis Pub/Sub
- **Multi-Tenant**: RSA JWT validation per tenant (`iss` claim = `tenant_id`)
- **Stateless**: No session storage - all data from JWT claims

---

## Package Structure (src/main/java/tech/limaxs/chat/)

### 📁 api/ - API Layer

#### REST Endpoints (api.rest.resource) - Virtual Threads
| File | Purpose | Endpoints |
|------|---------|-----------|
| `RoomResource.java` | Chat room CRUD | GET/POST `/api/rooms`, GET `/api/rooms/{id}/messages` |
| `MessageResource.java` | Message operations | POST `/api/messages`, PUT/DELETE `/api/messages/{id}` |
| `FileResource.java` | File upload/download | POST `/api/files/upload`, GET `/api/files/download/{id}` |
| `ArchiveResource.java` | Chat archiving | GET/POST `/api/archive` |
| `ConfigResource.java` | WebRTC config | GET `/api/config/webrtc` |
| `AdminResource.java` | Admin functions | Admin operations |
| `DevResource.java` | Dev utilities | Development tools |

#### REST DTOs (api.rest.dto)
- `CreateRoomRequest.java`, `SendMessageRequest.java` - Request payloads
- `RoomResponse.java`, `MessageResponse.java` - Response DTOs
- `FileUploadResponse.java`, `DownloadUrlResponse.java` - File responses
- `ArchivedRoomResponse.java`, `ArchivedMessageResponse.java` - Archive DTOs
- `IceServerConfig.java`, `WebRtcConfigResponse.java` - WebRTC DTOs
- `ErrorResponse.java` - Error handling

#### WebSocket (api.websocket) - Reactive
| File | Purpose |
|------|---------|
| `ChatWebSocketHandler.java` | Main WebSocket handler at `/ws/chat` - JWT auth, message routing, WebRTC signaling |

**WebSocket DTOs (api.websocket.dto)**:
- `ChatEvent.java` - Base envelope `{event, traceId, data}`
- Event types: `SEND_MSG`, `TYPING`, `SIGNAL_SDP`, `SIGNAL_ICE`, `ACK`
- `SendMessageData.java`, `SignalSdpData.java`, `SignalIceData.java`, `TypingData.java`

---

### 📁 core/ - Core Business Logic

#### JPA Entities (core.model) - Shared by ORM & Reactive
| Entity | Table | Key Fields |
|--------|-------|------------|
| `ChatUser.java` | `chat_users` | `id`, `tenant_id`, `username`, `display_name` |
| `Room.java` | `rooms` | `id`, `tenant_id`, `type` (DIRECT/GROUP), `created_by` |
| `RoomParticipant.java` | `room_participants` | `room_id`, `user_id`, `role` (MEMBER/ADMIN) |
| `RoomParticipantId.java` | - | Composite ID class |
| `Message.java` | `messages` | `id`, `room_id`, `sender_id`, `type`, `content` |
| `FileMetadata.java` | `file_metadata` | `id`, `room_id`, `storage_path`, `filename` |
| `RoomReadState.java` | `room_read_states` | `room_id`, `user_id`, `last_read_message_id` |
| `ArchivedRoom.java` | `archived_rooms` | Archive copy of rooms |
| `ArchivedMessage.java` | `archived_messages` | Archive copy of messages |

#### Repository - Imperative (core.repository.imperative) - For REST
Used by `@RunOnVirtualThread` endpoints with JDBC/Hibernate ORM:
- `ChatUserRepository.java`
- `RoomRepository.java`
- `MessageRepository.java`
- `RoomParticipantRepository.java`
- `FileMetadataRepository.java`
- `RoomReadStateRepository.java`
- `ArchivedRoomRepository.java`, `ArchivedMessageRepository.java`

#### Repository - Reactive (core.repository.reactive) - For WebSocket
Used by reactive WebSocket handlers with Mutiny/Hibernate Reactive:
- `ReactiveChatUserRepository.java`
- `ReactiveRoomRepository.java`
- `ReactiveMessageRepository.java`
- `ReactiveRoomParticipantRepository.java`

#### Service (core.service)
- `RoomArchiveService.java` - Room archiving business logic

---

### 📁 infra/ - Infrastructure Layer

#### Authentication (infra.auth)
| File | Purpose |
|------|---------|
| `JwtPrincipal.java` | JWT principal extraction (`sub` → `user_id`, `iss` → `tenant_id`) |

#### Configuration (infra.config)
| File | Purpose |
|------|---------|
| `FileCleanupConfig.java` | Scheduled file cleanup configuration |
| `JsonbStringConverter.java` | JPA attribute converter for JSONB columns |

#### Redis (infra.redis)
| File | Purpose |
|------|---------|
| `RedisService.java` | Presence, unread counters, pub/sub operations |
| `RedisPubSubListener.java` | Redis pub/sub message listener |

**Redis Keys**:
- `user:presence:{user_id}` - TTL 60s
- `unread:{room_id}:{user_id}` - Integer counter
- Pub/Sub channels: `chat:room:{room_id}`, `signal:user:{user_id}`, `typing:room:{room_id}`

#### Storage (infra.storage)
| File | Purpose |
|------|---------|
| `MinioService.java` | MinIO S3-compatible storage integration |
| `FileCleanupService.java` | Scheduled cleanup of unconfirmed files |

---

### 📁 util/ - Utilities
| File | Purpose |
|------|---------|
| `JWTGenerator.java` | JWT token generation utility |

---

## Configuration Files (src/main/resources/)

| File | Purpose |
|------|---------|
| `application.properties` | Main config: datasources (JDBC+Reactive), Redis, JWT, MinIO, OpenAPI |
| `schema.sql` | Database schema with indexes |
| `import.sql` | Initial data import |
| `test-data.sql` | Test dataset |
| `keys/private-key.pem` | RSA private key for JWT signing |
| `keys/public-key.pem` | RSA public key for JWT verification |

---

## Key Patterns for Bug Fixing

### Error Location Guide

| Symptom | Likely Location |
|---------|-----------------|
| REST endpoint error | `api/rest/resource/*.java` → Check corresponding `imperative/` repository |
| WebSocket connection/message error | `api/websocket/ChatWebSocketHandler.java` → Check `reactive/` repository |
| Database query error | `core/repository/imperative/` or `core/repository/reactive/` |
| Authentication failure | `infra/auth/JwtPrincipal.java` |
| Redis synchronization issue | `infra/redis/RedisService.java` |
| File upload/download error | `infra/storage/MinioService.java` |
| WebRTC signaling error | `api/websocket/ChatWebSocketHandler.java` (SIGNAL_SDP/SIGNAL_ICE) |
| Room archiving error | `core/service/RoomArchiveService.java` |

### Repository Usage Rules

- **REST Endpoints**: Use `core.repository.imperative.*` (blocking, JDBC)
- **WebSocket Handlers**: Use `core.repository.reactive.*` (non-blocking, Mutiny)
- **Never mix**: Don't call imperative repositories from reactive contexts

### Entity Relationships

```
ChatUser (tenant_id) ──┐
                      ├── RoomParticipant ── Room (tenant_id)
                      │                      │
                      └──────────────────────┴── Message ── FileMetadata
                                                      │
                                               RoomReadState
```

### WebSocket Event Flow

```
Client → ChatWebSocketHandler.onMessage()
  → Parse ChatEvent (event type)
  → ReactiveRepository (persist)
  → RedisService.publish (cross-node)
  → Broadcast to room participants
```

---

## Common File Paths for Quick Access

### REST API Issues
```
src/main/java/tech/limaxs/chat/api/rest/resource/
src/main/java/tech/limaxs/chat/core/repository/imperative/
```

### WebSocket Issues
```
src/main/java/tech/limaxs/chat/api/websocket/
src/main/java/tech/limaxs/chat/core/repository/reactive/
```

### Database Issues
```
src/main/java/tech/limaxs/chat/core/model/
src/main/resources/schema.sql
src/main/resources/application.properties (datasource config)
```

### Authentication/Authorization
```
src/main/java/tech/limaxs/chat/infra/auth/JwtPrincipal.java
src/main/resources/keys/
```

### Redis/Cache Issues
```
src/main/java/tech/limaxs/chat/infra/redis/
src/main/resources/application.properties (redis config)
```

### File Storage Issues
```
src/main/java/tech/limaxs/chat/infra/storage/
src/main/java/tech/limaxs/chat/api/rest/resource/FileResource.java
```

---

## Build & Run Commands

```bash
# Dev mode
./gradlew quarkusDev

# Build
./gradlew build

# Test
./gradlew test

# Native build
./gradlew build -Dquarkus.native.enabled=true
```

---

## Endpoints Summary

| Method | Path | Handler |
|--------|------|---------|
| GET | `/api/rooms` | `RoomResource.listRooms()` |
| POST | `/api/rooms` | `RoomResource.createRoom()` |
| GET | `/api/rooms/{id}/messages` | `RoomResource.getMessages()` |
| POST | `/api/messages` | `MessageResource.sendMessage()` |
| PUT | `/api/messages/{id}` | `MessageResource.editMessage()` |
| DELETE | `/api/messages/{id}` | `MessageResource.deleteMessage()` |
| POST | `/api/files/upload` | `FileResource.upload()` |
| GET | `/api/files/download/{id}` | `FileResource.download()` |
| GET | `/api/config/webrtc` | `ConfigResource.getWebRtcConfig()` |
| WS | `/ws/chat` | `ChatWebSocketHandler` |
