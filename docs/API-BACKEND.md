# NexusChat Engine - Backend API Documentation

> Internal APIs for backend-to-backend communication and admin operations

---

## Overview

This document covers APIs that are primarily used for:
1. **Admin Operations** - Room archiving, system management
2. **Dev/Test Utilities** - Token generation for testing
3. **Internal Service Communication** - Cross-service data exchange

## Base URL

```
Production:  https://api.nexuschat.com/internal
Development: http://localhost:8080
```

## Authentication

Admin/Backend APIs require JWT tokens with **admin privileges** or **service account** credentials.

```
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

---

## 1. Admin APIs

### 1.1 Archive Room

```http
POST /api/admin/archive/{roomId}
```

**Description:** Move a room and all its data to archive tables. This is a **destructive operation** - the original room and messages are deleted from main tables after being copied to archive.

**Use Cases:**
- Room is no longer active (e.g., project completed)
- Compliance requirements (long-term storage)
- Database optimization (move old data to cold storage)

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| roomId    | UUID   | Room ID to archive |

**Response (201 Created):**
```json
{
  "originalRoomId": "650e8400-e29b-41d4-a716-446655440000",
  "archivedRoomId": "750e8400-e29b-41d4-a716-446655440001",
  "messagesArchived": 150,
  "participantsArchived": 5
}
```

**Field Descriptions:**
| Field                | Type   | Description                                |
|----------------------|--------|--------------------------------------------|
| originalRoomId       | UUID   | ID of the original room (now deleted)      |
| archivedRoomId       | UUID   | ID of the archived room                    |
| messagesArchived     | int    | Number of messages moved to archive        |
| participantsArchived | int    | Number of participants moved to archive    |

**Flow Diagram:**
```
Admin Service           RoomArchiveService         Main Tables              Archive Tables
      |                         |                      |                       |
      |-- POST /api/admin/archive/{roomId} ------>|
      |                         |                      |                       |
      |                         |  START TRANSACTION                           |
      |                         |                      |                       |
      |                         |-- 1. Copy Room to archived_rooms ---------->|
      |                         |                      |                       |
      |                         |-- 2. Copy Messages to archived_messages --->|
      |                         |                      |                       |
      |                         |-- 3. Copy Participants to archived_participants ->|
      |                         |                      |                       |
      |                         |-- 4. Delete from main tables (cascade) ---->|
      |                         |                      |                       |
      |                         |  COMMIT TRANSACTION                          |
      |                         |                      |                       |
      |<-- 201 ArchivedRoomResponse ---------------------|
```

**Detailed Service Flow:**
```
RoomArchiveService.archiveRoom(roomId, adminUserId)
    |
    |-- 1. Validate room exists
    |       |-- RoomRepository.findById(roomId)
    |
    |-- 2. Check if room already archived
    |       |-- ArchivedRoomRepository.findByOriginalRoomId(roomId)
    |       |-- If exists: throw error
    |
    |-- 3. Begin @Transactional
    |
    |-- 4. Copy to Archive
    |       |-- Create ArchivedRoom entity
    |       |-- Copy all Messages → ArchivedMessage
    |       |-- Copy all Participants → ArchivedParticipant
    |       |-- ArchivedRoomRepository.persist(all)
    |
    |-- 5. Delete from Main (CASCADE)
    |       |-- RoomRepository.deleteById(roomId)
    |       |-- CASCADE deletes: messages, participants, read_states
    |
    |-- 6. Commit transaction
    |
    `-- 7. Return ArchiveSummaryResponse
```

**Error Responses:**
| Status | Description                  |
|--------|------------------------------|
| 404    | Room not found               |
| 409    | Room already archived        |
| 500    | Transaction failed           |

**Database Impact:**
```sql
-- Tables affected
1. rooms              -- DELETE ( CASCADE to messages, room_participants, room_read_states)
2. messages           -- DELETE
3. room_participants  -- DELETE
4. room_read_states   -- DELETE

1. archived_rooms     -- INSERT
2. archived_messages  -- INSERT
3. archived_participants -- INSERT
```

---

### 1.2 Check Archive Status

```http
GET /api/admin/archived/{roomId}
```

**Description:** Check if a room has been archived.

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| roomId    | UUID   | Original room ID |

**Response (200 OK):**
```json
{
  "roomId": "650e8400-e29b-41d4-a716-446655440000",
  "archived": true
}
```

**Flow Diagram:**
```
Admin Service           RoomArchiveService         Archive Tables
      |                         |                      |
      |-- GET /api/admin/archived/{roomId} ------>|
      |                         |                      |
      |                         |-- Check archived_rooms -->|
      |                         |   WHERE original_room_id = roomId
      |                         |                      |
      |                         |<-- exists? -----------|
      |                         |                      |
      |<-- 200 ArchivedCheckResponse ----------------|
```

---

## 2. Dev/Test APIs

### 2.1 Generate Test JWT Token

```http
GET /api/dev/token/{userId}?name=John+Doe&tenant=tenant-001
```

**⚠️ WARNING:** This endpoint is for **development/testing only** and should be **disabled in production**.

**Description:** Generate a valid JWT token for testing without going through an authentication service.

**Path Parameters:**
| Parameter | Type   | Description           |
|-----------|--------|-----------------------|
| userId    | string | User ID (will be converted to UUID) |

**Query Parameters:**
| Parameter | Type   | Default       | Description           |
|-----------|--------|---------------|-----------------------|
| name      | string | "Test User"   | Display name          |
| tenant    | string | "test-tenant" | Tenant ID             |

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpc3MiOiJ0ZW5hbnQtMDAxIiwidGVuYW50SWQiOiJ0ZW5hbnQtMDAxIiwibmFtZSI6IkpvaG4gRG9lIiwiZXhwIjoxNzA0MDY3MjAwLCJpYXQiOjE3MDM5ODA4MDB9...",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "tenantId": "tenant-001",
  "expiresAt": "2024-01-15T10:00:00Z"
}
```

**Flow Diagram:**
```
Dev Tool/Test Script        JWTGenerator              Keystore
      |                         |                      |
      |-- GET /api/dev/token/{userId} -------------->|
      |                         |                      |
      |                         |-- Load RSA private key -->|
      |                         |                      |
      |                         |-- Sign JWT with RS256 -->|
      |                         |                      |
      |<-- 200 TokenResponse -------------------------|
      |  { token, userId, name, tenantId, expiresAt }
```

**Implementation Details:**
```java
// JWTGenerator.java
public static String generateToken(String userId, String name, String tenantId) {
    // Load RSA private key from resources/keys/private-key.pem
    PrivateKey privateKey = readPrivateKey();

    // Build JWT with claims
    return Jwts.builder()
        .subject(userId)                    // sub: User ID
        .issuer(tenantId)                   // iss: Tenant ID (for validation)
        .claim("tenantId", tenantId)        // Custom claim
        .claim("name", name)                // User display name
        .expiration(Date.from(Instant.now().plus(7, ChronoUnit.DAYS))) // 7 days
        .issuedAt(Date.from(Instant.now())) // iat: Issued at
        .signWith(privateKey, Jwts.SIG.RS256) // Sign with RSA-256
        .compact();
}
```

**Security Notes:**
1. ⚠️ **NEVER enable this endpoint in production**
2. Use environment variable to toggle: `QUARKUS_PROFILE=prod` should disable
3. Replace with proper OAuth2/OIDC integration for production
4. Consider IP whitelist for dev environments

---

## 3. WebSocket Event Flow (Internal)

### Cross-Node Message Distribution via Redis Pub/Sub

The chat server uses Redis Pub/Sub for message distribution across multiple server instances.

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│  Server Node 1  │         │  Server Node 2  │         │  Server Node 3  │
│  (Port 8080)    │         │  (Port 8080)    │         │  (Port 8080)    │
└────────┬────────┘         └────────┬────────┘         └────────┬────────┘
         │                           │                           │
         │         ┌─────────────────┴─────────────────┐        │
         │         │                                   │        │
         └─────────┤           Redis Cluster           ├────────┘
                   │   (Pub/Sub + Presence + State)   │
                   │                                   │
                   └─────────────────┬─────────────────┘
                                     │
                  ┌──────────────────┴──────────────────┐
                  │                                     │
         ┌────────┴────────┐                   ┌────────┴────────┐
         │ RedisPubSub    │                   │  RedisService   │
         │ Listener       │                   │  (Pub/Sub)      │
         └────────────────┘                   └────────────────┘
```

### 3.1 SEND_MSG Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Client A (Node 1)                             │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     |-- WebSocket: SEND_MSG
                                     │
┌────────────────────────────────────┴────────────────────────────────────────┐
│                          ChatWebSocketHandler (Node 1)                      │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     ├── 1. Authenticate JWT
                                     │
                                     ├── 2. Validate request
                                     │
                                     ├── 3. Persist to PostgreSQL (reactive)
                                     │       INSERT INTO messages (...)
                                     │
                                     ├── 4. Publish to Redis Pub/Sub
                                     │       CHANNEL: chat:room:{roomId}
                                     │       PAYLOAD: {event: "NEW_MESSAGE", ...}
                                     │
                                     ├── 5. Echo to sender (WebSocket)
                                     │       NEW_MESSAGE event
                                     │
                                     │
         ┌───────────────────────────┴───────────────────────────┐
         │                                                         │
         │  ┌────────────────────────────────────────────────┐   │
         │  │          Redis Pub/Sub Broker                   │   │
         │  │  CHANNEL: chat:room:650e8400-...               │   │
         │  └────────────────────┬───────────────────────────┘   │
         │                       │                               │
         │         ┌─────────────┴─────────────┐               │
         │         │                           │               │
         │  ┌──────┴─────────┐         ┌──────┴─────────┐     │
         │  │ RedisPubSub    │         │ RedisPubSub    │     │
         │  │ Listener (Node 2)│       │ Listener (Node 3)│     │
         │  └──────┬─────────┘         └──────┬─────────┘     │
         │         │                           │               │
         │         │                           │               │
         │  ┌──────┴───────────────────────────┴──────┐       │
         │  │        ChatWebSocketHandler             │       │
         │  │        sendToUser(userId)               │       │
         │  └──────┬──────────────────────────────────┘       │
         │         │                                          │
         └─────────┼──────────────────────────────────────────┘
                   │
         ┌─────────┴─────────┐       ┌─────────────────────┐
         │  Client B (Node 2)│       │  Client C (Node 3)  │
         └───────────────────┘       └─────────────────────┘
                   │                              │
                   <-- WebSocket: NEW_MESSAGE     <-- WebSocket: NEW_MESSAGE
```

### 3.2 SIGNAL_SDP Flow (WebRTC)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Alice (Node 1)                                   │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     |-- WebSocket: SIGNAL_SDP
                                     │   {targetId: "bob-id", type: "offer", sdp: "..."}
                                     │
┌────────────────────────────────────┴────────────────────────────────────────┐
│                       ChatWebSocketHandler (Node 1)                         │
│                                                                            │
│  handleWebRTCSignal():                                                     │
│    1. Extract targetId, type, sdp                                          │
│    2. If type == "offer":                                                  │
│       a. Check if Bob is busy (Redis GET user:call:{bobId})                │
│       b. If busy: Send CALL_REJECTED to Alice                              │
│       c. If not busy:                                                      │
│          - Set Alice as busy (Redis SETEX user:call:{aliceId} 300 "busy")  │
│          - Forward to Bob via Redis Pub/Sub                                │
│    3. Else (type == "answer"):                                             │
│       - Forward to Bob via Redis Pub/Sub                                   │
│                                                                            │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     |-- Publish to Redis
                                     │   CHANNEL: signal:user:{bobId}
                                     │   PAYLOAD: {event: "SIGNAL_SDP", ...}
                                     │
         ┌───────────────────────────┴───────────────────────────┐
         │                                                         │
         │  ┌────────────────────────────────────────────────┐   │
         │  │          Redis Pub/Sub Broker                   │   │
         │  │  CHANNEL: signal:user:650e8400-...             │   │
         │  └────────────────────┬───────────────────────────┘   │
         │                       │                               │
         │         ┌─────────────┴─────────────┐               │
         │         │   Check: Bob connected?   │               │
         │         └─────────────┬─────────────┘               │
         │                       │ Yes                          │
         │  ┌────────────────────┴────────────┐                │
         │  │ RedisPubSubListener (Any Node)  │                │
         │  │ handleUserSignal()              │                │
         │  │   - Parse targetId from data    │                │
         │  │   - ChatWebSocketHandler        │                │
         │  │       .sendToUser(bobId, msg)   │                │
         │  └────────────┬────────────────────┘                │
         │               │                                     │
         └───────────────┼─────────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         │    Bob (Connected to Node 2)  │
         └───────────────────────────────┘
                         │
                         <-- WebSocket: SIGNAL_SDP
                         {targetId: "alice-id", type: "answer", sdp: "..."}
                         │
                         |-- Bob sends Answer back
                         │
                         ... (same flow in reverse)
```

### 3.3 Redis Data Structures

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Redis Keys                                     │
└─────────────────────────────────────────────────────────────────────────────┘

1. Presence (TTL: 60s)
   Key:    user:presence:{userId}
   Value:  "online"
   Purpose: Track online users, auto-expire after 60s

2. Call State (TTL: 300s)
   Key:    user:call:{userId}
   Value:  "busy" | "idle"
   Purpose: WebRTC call state management

3. Unread Count
   Key:    unread:{roomId}:{userId}
   Value:  "42" (incrementing integer)
   Purpose: Track unread message count per user per room

4. Pub/Sub Channels
   Channel Pattern    Purpose
   ───────────────────────────────────────────────────────────────────────────
   chat:room:{roomId}      Messages, edits, deletes for room members
   signal:user:{userId}    WebRTC signaling (SDP/ICE) for P2P calls
   typing:room:{roomId}    Typing indicators for room members
```

---

## 4. Database Schema (Internal Reference)

### Main Tables (Hot Data)

```sql
-- Primary tables for active chat operations
chat_users          -- User profiles
rooms               -- Active rooms
room_participants   -- Room membership
messages            -- Active messages
room_read_states    -- Per-user read markers
file_metadata       -- File upload records
```

### Archive Tables (Cold Data)

```sql
-- Archived data (read-only, long-term storage)
archived_rooms      -- Archived room metadata
archived_participants -- Archived room membership
archived_messages   -- Archived messages
```

---

## 5. Service Integration Points

### 5.1 MinIO/S3 Storage Service

**Purpose:** File storage for images, videos, documents

**Integration Pattern:** Presigned URLs (client → MinIO direct)

```
Chat Server                     MinIO/S3
    |                              |
    |-- 1. Generate Presigned URL -->|
    |    PUT /files/{objectKey}     |
    |    (expires in 3600s)         |
    |                              |
    |<-- Presigned URL -------------|
    |    https://s3.../put?sig=...  |
    |                              |
    |-- 2. Client uploads directly ----------------> MinIO/S3
    |    PUT {presignedURL}         |
    |    (file data)                |
    |                              |
    |-- 3. Confirm upload --------->|
    |    POST /files/{id}/confirm   |
    |                              |
    |                              |
    |-- 4. Generate download URL -->|
    |    GET /files/{id}/download   |
    |                              |
    |<-- Presigned Download URL ----|
    |    https://s3.../get?sig=...  |
    |                              |
    |-- 5. Client downloads directly -----------> MinIO/S3
    |    GET {presignedURL}         |
```

### 5.2 PostgreSQL Database

**Connection Pattern:** Dual datasources (JDBC + Reactive)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Quarkus Datasource Configuration                   │
└─────────────────────────────────────────────────────────────────────────────┘

1. JDBC Datasource (Hibernate ORM)
   Purpose: REST API endpoints (@RunOnVirtualThread)
   Driver:  PostgreSQL JDBC Driver
   Pool:    Agroal (blocking)

2. Reactive Datasource (Hibernate Reactive)
   Purpose: WebSocket handlers (reactive)
   Driver:  Vert.x PostgreSQL Client
   Pool:    Vert.x Pool (non-blocking)

Both connect to the SAME PostgreSQL database but maintain separate pools.
```

---

## 6. Monitoring & Observability

### 6.1 Health Check Endpoints

```http
GET /q/health
GET /q/health/live
GET /q/health/ready
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "Database",
      "status": "UP"
    },
    {
      "name": "Redis",
      "status": "UP"
    }
  ]
}
```

### 6.2 Metrics Endpoints

```http
GET /q/metrics
```

**Available Metrics:**
- `vendor.database.pool.size` - JDBC connection pool size
- `vendor.http.requests.count` - HTTP request count
- `vendor.websocket.connections.count` - Active WebSocket connections
- Custom metrics for message throughput, Redis pub/sub, etc.

### 6.3 Logging

**Key Log Patterns:**

```log
# WebSocket Connection
INFO: WebSocket opened for user: {userId} ({name})
INFO: WebSocket closed for user: {userId}

# Message Handling
INFO: Handling SEND_MSG: roomId={roomId}, type={type}, userId={userId}
INFO: SQL executed, rowSet size: {size}

# Redis Pub/Sub
INFO: Publishing to room channel: chat:room:{roomId}
INFO: Forwarding {eventType} to user: {userId}

# WebRTC Signaling
INFO: Handling SIGNAL_SDP: targetId={targetId}, type={type}
INFO: Forwarding SIGNAL_SDP to user: {userId}

# Archive Operation
INFO: Archiving room {roomId} with {messageCount} messages
```

---

## 7. Deployment Considerations

### 7.1 Environment Variables

```bash
# Database
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/chatdb
QUARKUS_DATASOURCE_REACTIVE_URL=postgresql://localhost:5432/chatdb

# Redis
QUARKUS.REDIS.HOSTS=redis://localhost:6379

# MinIO/S3
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=chat-files

# JWT
JWT_PUBLIC_KEY_PATH=keys/public-key.pem
JWT_PRIVATE_KEY_PATH=keys/private-key.pem

# Profile
QUARKUS_PROFILE=dev  # or prod
```

### 7.2 Scaling Considerations

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Horizontal Scaling                                 │
└─────────────────────────────────────────────────────────────────────────────┘

Stateless Components (can scale horizontally):
  - REST API endpoints (@RunOnVirtualThread)
  - WebSocket handlers
  - Redis pub/sub listeners

Shared State (requires external services):
  - PostgreSQL (database)
  - Redis (presence, pub/sub, call state)
  - MinIO/S3 (file storage)

Session Management:
  - NO server-side sessions
  - All state in JWT claims
  - WebSocket connections tracked in memory (non-shared)
    - User can connect to ANY node
    - Pub/sub ensures cross-node message delivery
```

---

## 8. Security Best Practices

### 8.1 JWT Validation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      JWT Validation Flow                                    │
└─────────────────────────────────────────────────────────────────────────────┘

1. Extract token from Authorization header or WebSocket query param
2. Verify signature using public key (RSA-256)
3. Validate claims:
   - exp (expiration) - Must be in future
   - iss (issuer) - Must match registered tenant
   - sub (subject) - Must be valid UUID
4. Extract user context:
   - userId = sub claim
   - tenantId = iss claim (or custom claim)
   - name = custom claim

5. Multi-tenant isolation:
   - All queries filtered by tenantId
   - Cross-tenant access forbidden (403)
```

### 8.2 Production Checklist

- [ ] Disable `/api/dev/*` endpoints
- [ ] Use strong RSA keys (4096-bit)
- [ ] Enable HTTPS/TLS
- [ ] Configure rate limiting
- [ ] Enable audit logging
- [ ] Set up monitoring/alerting
- [ ] Configure Redis persistence
- [ ] Enable database backups
- [ ] Use secret management (Vault, AWS Secrets Manager)
- [ ] Enable CORS for trusted domains only
- [ ] Configure MinIO/S3 lifecycle policies
