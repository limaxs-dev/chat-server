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
Production:  https://api.nexuschat.com
Development: http://localhost:8080
```

## Authentication

All backend API requests require a valid JWT token in the `Authorization` header:

```
Authorization: Bearer <JWT_TOKEN>
```

**Note:** Same JWT structure as frontend API. This is a single-application service with NO multi-tenancy support.

```json
{
  "sub": "user-uuid",     // User ID
  "name": "User Name",    // User display name
  "iss": "nexus-chat-engine",
  "iat": 1234567890,
  "exp": 1234567890
}
```

---

## 1. Admin APIs

### 1.1 Archive Room

```http
POST /api/back/admin/archive/{roomId}
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
  "originalRoomId": "650e8400-e29b-41d4-a16-446655440000",
  "archivedRoomId": "750e8400-e29b-41d4-a16-446655440001",
  "messagesArchived": 150,
  "participantsArchived": 5
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| originalRoomId | UUID | ID of the original room (now deleted) |
| archivedRoomId | UUID | ID of the newly created archived room |
| messagesArchived | int | Number of messages moved to archive |
| participantsArchived | int | Number of participants moved to archive |

**Error Responses:**
| Status | Description |
|--------|-------------|
| 401 | Unauthorized |
| 404 | Room not found |
| 409 | Room already archived |
| 500 | Transaction failed |

**Archive Process Flow:**
```
1. START TRANSACTION
2. Validate room exists
3. Check room is not already archived
4. Copy room → archived_rooms
5. Copy messages → archived_messages (with sender names denormalized)
6. Copy participants → archived_participants (with user names denormalized)
7. Calculate message count and participant count
8. Record first_message_at and last_message_at timestamps
9. Record archived_by (admin user ID)
10. Delete from main tables (CASCADE deletes messages, participants)
11. COMMIT TRANSACTION
```

**ArchivedRoom Fields:**
| Field | Type | Description |
|-------|------|-------------|
| id | UUID | New archived room ID |
| originalRoomId | UUID | Original room ID (reference) |
| type | string | Room type (DIRECT/GROUP) |
| name | string | Room name (nullable for DIRECT) |
| participantCount | int | Number of participants |
| messageCount | int | Number of messages |
| firstMessageAt | LocalDateTime | **nullable** - First message timestamp |
| lastMessageAt | LocalDateTime | **nullable** - Last message timestamp |
| archivedAt | LocalDateTime | When archive was created |
| archivedBy | string | Admin user ID who performed archive |

---

### 1.2 Check Archive Status

```http
GET /api/back/admin/archived/{roomId}
```

**Description:** Check if a room has been archived.

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| roomId    | UUID   | Original (non-archived) room ID |

**Response (200 OK):**
```json
{
  "roomId": "650e8400-e29b-41d4-a16-446655440000",
  "archived": true
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| roomId | UUID | Original room ID |
| archived | boolean | Whether room is archived |

**Error Responses:**
| Status | Description |
|--------|-------------|
| 401 | Unauthorized |
| 404 | Room not found in archive (may not be archived yet) |

---

## 2. Dev/Test APIs

### 2.1 Generate Test JWT Token

```http
GET /api/dev/token/{userId}?name=John+Doe
```

**⚠️ WARNING:** This endpoint is for **development/testing only**.

**Description:** Generate a valid JWT token for testing without going through an authentication service.

**Path Parameters:**
| Parameter | Type   | Default | Description |
|-----------|--------|---------|-------------|
| userId    | string | - | User ID (will be converted to UUID) |

**Query Parameters:**
| Parameter | Type   | Default | Description |
|-----------|--------|---------|-------------|
| name      | string | "Test User" | Display name |

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9...",
  "userId": "550e8400-e29b-41d4-a16-446655440000",
  "name": "John Doe",
  "expiresAt": "2024-01-15T10:00:00Z"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| token | string | Signed JWT token (RS256) |
| userId | string | User ID (UUID format) |
| name | string | User display name |
| expiresAt | string | ISO 8601 timestamp (+7 days from now) |

**JWT Token Details:**
- **Algorithm:** RS256 (RSA with SHA-256)
- **Expiration:** 7 days from issuance
- **Issuer:** `nexus-chat-engine` (fixed)
- **Claims:**
  - `sub`: User ID
  - `name`: User display name
  - `iss`: `nexus-chat-engine`
  - `iat`: Issued at timestamp
  - `exp`: Expiration timestamp

**Error Responses:**
| Status | Description |
|--------|-------------|
| 500 | Failed to generate token |

**Security Notes:**
1. ⚠️ **NEVER enable this endpoint in production**
2. Consider IP whitelist for dev environments
3. Replace with proper OAuth2/OIDC integration for production

---

## 3. Room Management APIs

### 3.1 Create Room

```http
POST /api/back/rooms
Content-Type: application/json
```

**Description:** Create a new room and add participants. Used by backend services to create rooms on behalf of users.

**Request Body:**
```json
{
  "type": "GROUP",
  "name": "Engineering Team",
  "participantIds": [
    "550e8400-e29b-41d4-a16-446655440000",
    "650e8400-e29b-41d4-a16-446655440001"
  ]
}
```

**Request Fields:**
| Field | Type | Required? | Validation |
|-------|------|-----------|------------|
| type | string | YES | Must be `DIRECT` or `GROUP` |
| name | string | **NO** | Required for GROUP type, null for DIRECT |
| participantIds | array of UUID | YES | At least 1 user ID |

**Room Type Rules:**
| Type | name Field | participantIds |
|------|------------|------------------|
| DIRECT | Must be null | Exactly 2 users |
| GROUP | Required | 2+ users |

**Response (201 Created):**
```json
{
  "id": "650e8400-e29b-41d4-a16-446655440000",
  "name": "Engineering Team",
  "type": "GROUP",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Response Fields:**
| Field | Type | Nullable? | Description |
|-------|------|-----------|-------------|
| id | UUID | NO | Newly created room ID |
| name | string | **YES** | Room name (null for DIRECT) |
| type | string | NO | Room type |
| createdAt | string | NO | ISO 8601 timestamp |

**Behavior:**
- Creator is automatically added as a participant
- Users that don't exist in `chat_users` are auto-created with name "User {prefix}"
- All participants get `MEMBER` role initially
- Returns 201 with `Location: /api/back/rooms/{roomId}` header

**Error Responses:**
| Status | Description |
|--------|-------------|
| 400 | Invalid request (wrong type, empty participants, etc.) |
| 401 | Unauthorized |

---

### 3.2 Get Room (Admin)

```http
GET /api/back/rooms/{id}
```

**Description:** Get room details (admin endpoint - no participant check required).

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| id | UUID | Room ID |

**Response (200 OK):** Same as Create Room response

**Error Responses:**
| Status | Description |
|--------|-------------|
| 401 | Unauthorized |
| 404 | Room not found |

---

## 4. Message Operations

### 4.1 Send Message

```http
POST /api/back/messages
Content-Type: application/json
```

**Description:** Send a message to a room. Used by backend services to send messages on behalf of users.

**Request Body:**
```json
{
  "roomId": "650e8400-e29b-41d4-a16-446655440000",
  "type": "TEXT",
  "contentText": "Hello from backend!",
  "contentMeta": null,
  "clientRef": "msg-12345"
}
```

**Request Fields:**
| Field | Type | Required? | Validation |
|-------|------|-----------|------------|
| roomId | UUID | YES | Must exist |
| type | string | YES | Must be valid MessageType |
| contentText | string | **NO** | Required for TEXT type |
| contentMeta | string/JSON | **NO** | File metadata for FILE/IMAGE types |
| clientRef | string/UUID | **NO** | For idempotency |

**Message Types:**
| Value | contentText | contentMeta |
|-------|--------------|-------------|
| TEXT | Required | Optional |
| IMAGE | Optional (caption) | **Required** - JSON with file info |
| FILE | Optional (description) | **Required** - JSON with file info |
| AUDIO | Optional (transcription) | **Required** - JSON with file info |
| VIDEO | Optional (caption) | **Required** - JSON with file info |
| SYSTEM | Varies | Optional |
| VOICE_CALL | Call summary | Optional - Call metadata |

**Idempotency:**
- If `clientRef` is provided and already used, returns existing message (200 OK)
- If `clientRef` is null, always creates new message (201 Created)
- `clientRef` must be unique across all messages

**Response (201 Created):**
```json
{
  "id": "750e8400-e29b-41d4-a16-446655440000",
  "roomId": "650e8400-e29b-41d4-a16-446655440000",
  "senderId": "550e8400-e29b-41d4-a16-446655440000",
  "type": "TEXT",
  "contentText": "Hello from backend!",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Response (200 OK):** When `clientRef` was already used (idempotent)

**Response Fields:**
| Field | Type | Nullable? | Description |
|-------|------|-----------|-------------|
| id | UUID | NO | Message ID |
| roomId | UUID | NO | Room ID |
| senderId | UUID | NO | Sender user ID |
| type | string | NO | Message type |
| contentText | string | **YES** | Message text |
| createdAt | string | NO | ISO 8601 timestamp |

**Behavior:**
- Message is persisted to database
- **NOT broadcast via WebSocket** (use WebSocket SEND_MSG event for real-time)
- Sender is auto-created if doesn't exist

**Error Responses:**
| Status | Description |
|--------|-------------|
| 400 | Invalid request (room not found, etc.) |
| 404 | Room not found |
| 409 | Duplicate clientRef (if same clientRef sent twice without 200 OK response) |

---

## 5. File Upload APIs

### 5.1 Get Upload URL

```http
POST /api/back/files/upload-url?fileName=document.pdf&fileSize=1024000&contentType=application/pdf
```

**Description:** Get a presigned URL for direct file upload to MinIO/S3.

**Query Parameters:**
| Parameter | Type | Required? | Constraints |
|-----------|------|-----------|------------|
| fileName | string | YES | Original filename |
| fileSize | long | YES | 1 - 104,857,600 bytes (100 MB) |
| contentType | string | YES | MIME type (e.g., application/pdf) |

**Constraints:**
- **Max file size:** 100 MB (104,857,600 bytes)
- **Min file size:** 1 byte
- URL expires in 1 hour (3600 seconds)

**Response (200 OK):**
```json
{
  "fileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "uploadUrl": "https://storage.example.com/files/...?expires=1704123456&signature=...",
  "objectKey": "550e8400-e29b-41d4-a16-446655440000/document.pdf"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| fileId | UUID | File metadata ID |
| uploadUrl | string | Presigned PUT URL (valid for 1 hour) |
| objectKey | string | MinIO object key |

**Error Responses:**
| Status | Description |
|--------|-------------|
| 400 | Invalid file size (must be 1-100MB) |
| 401 | Unauthorized |
| 404 | User not found |

**Upload Flow:**
```
1. POST /api/back/files/upload-url → Get fileId and uploadUrl
2. Client PUT {uploadUrl} with file data → Direct to MinIO/S3
3. POST /api/back/files/{fileId}/confirm → Mark upload complete
```

---

### 5.2 Confirm Upload

```http
POST /api/back/files/{fileId}/confirm
```

**Description:** Mark file upload as complete. Required after uploading to presigned URL.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|--------|-------------|
| fileId | UUID | File ID from upload-url response |

**Response (204 No Content):** Upload confirmed successfully

**Behavior:**
- Sets `is_confirmed = true` in database
- Only uploader can confirm their own uploads
- Unconfirmed files may be cleaned up by scheduled jobs

**Error Responses:**
| Status | Description |
|--------|-------------|
| 401 | Unauthorized |
| 404 | File not found or not owned by user |

---

## 6. Archive APIs (Read-Only)

### 6.1 List Archived Rooms

```http
GET /api/back/archive/rooms?page=0&size=20
```

**Description:** Get paginated list of archived rooms.

**Query Parameters:**
| Parameter | Type | Default | Min | Max | Description |
|-----------|--------|---------|-----|-----|-----------------------|
| page | int | 0 | 0 | - | Page number (0-based) |
| size | int | 20 | 1 | 100 | Items per page |

**Response (200 OK):**
```json
[
  {
    "id": "750e8400-e29b-41d4-a16-446655440001",
    "originalRoomId": "650e8400-e29b-41d4-a16-446655440000",
    "name": "Engineering Team",
    "type": "GROUP",
    "archivedAt": "2024-01-20T15:45:00Z",
    "messageCount": 150,
    "participantCount": 5
  }
]
```

**Response Fields:**
| Field | Type | Nullable? | Description |
|-------|------|-----------|-------------|
| id | UUID | NO | Archived room ID |
| originalRoomId | UUID | NO | Original room ID (deleted) |
| name | string | **YES** | Room name (null for DIRECT) |
| type | string | NO | Room type |
| archivedAt | string | NO | When archive was created |
| messageCount | int | NO | Number of messages in archive |
| participantCount | int | NO | Number of participants in archive |

---

### 6.2 Get Archived Room Details

```http
GET /api/back/archive/rooms/{archivedRoomId}
```

**Description:** Get detailed archived room information.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|--------|-------------|
| archivedRoomId | UUID | Archived room ID |

**Response (200 OK):**
```json
{
  "id": "750e8400-e29b-41d4-a16-446655440001",
  "originalRoomId": "650e8400-e29b-41d4-a16-446655440000",
  "name": "Engineering Team",
  "type": "GROUP",
  "archivedAt": "2024-01-20T15:45:00Z",
  "messageCount": 150,
  "participantCount": 5,
  "archivedBy": "550e8400-e29b-41d4-a16-446655440000",
  "firstMessageAt": "2024-01-15T10:00:00Z",
  "lastMessageAt": "2024-01-20T14:30:00Z"
}
```

**Additional Response Fields:**
| Field | Type | Nullable? | Description |
|-------|------|-----------|-------------|
| archivedBy | string | NO | Admin user ID who performed archive |
| firstMessageAt | string | **YES** | First message timestamp |
| lastMessageAt | string | **YES** | Last message timestamp |

**Error Responses:**
| Status | Description |
|--------|-------------|
| 404 | Archived room not found |

---

### 6.3 Get Archived Messages

```http
GET /api/back/archive/rooms/{archivedRoomId}/messages?page=0&size=50
```

**Description:** Get paginated archived messages (oldest first - unlike main API).

**Path Parameters:**
| Parameter | Type | Description |
|-----------|--------|-------------|
| archivedRoomId | UUID | Archived room ID |

**Query Parameters:**
| Parameter | Type | Default | Min | Max | Description |
|-----------|--------|---------|-----|-----|-----------------------|
| page | int | 0 | 0 | - | Page number (0-based) |
| size | int | 50 | 1 | 100 | Items per page |

**Response (200 OK):**
```json
[
  {
    "id": "archived-message-1",
    "archivedRoomId": "750e8400-e29b-41d4-a16-446655440001",
    "originalRoomId": "650e8400-e29b-41d4-a16-446655440000",
    "senderId": "550e8400-e29b-41d4-a16-446655440000",
    "senderName": "Alice",
    "type": "TEXT",
    "contentText": "First message in room",
    "createdAt": "2024-01-15T10:00:00Z"
  }
]
```

**ArchivedMessage Fields:**
| Field | Type | Nullable? | Description |
|-------|------|-----------|-------------|
| id | UUID | NO | Archived message ID |
| archivedRoomId | UUID | NO | Parent archived room ID |
| originalRoomId | UUID | NO | Original room ID |
| senderId | UUID | NO | Sender user ID |
| senderName | string | NO | **Denormalized** sender name |
| type | string | NO | Message type |
| contentText | string | **YES** | Message text |
| createdAt | string | NO | Original creation time |

**Note:** `contentMeta` is not included in archived message response (only `contentText`).

---

### 6.4 Get Archived Participants

```http
GET /api/back/archive/rooms/{archivedRoomId}/participants
```

**Description:** Get all participants of an archived room.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|--------|-------------|
| archivedRoomId | UUID | Archived room ID |

**Response (200 OK):**
```json
[
  {
    "id": "archived-participant-1",
    "archivedRoomId": "750e8400-e29b-41d4-a16-446655440001",
    "userId": "550e8400-e29b-41d4-a16-446655440000",
    "userName": "Alice",
    "role": "ADMIN",
    "joinedAt": "2024-01-15T10:00:00Z"
  }
]
```

**ArchivedParticipant Fields:**
| Field | Type | Nullable? | Description |
|-------|------|-----------|-------------|
| id | UUID | NO | Archived participant ID |
| archivedRoomId | UUID | NO | Parent archived room ID |
| userId | UUID | NO | User ID |
| userName | string | NO | **Denormalized** user name |
| role | string | NO | Participant role |
| joinedAt | string | NO | When user joined |

**Role Values:**
- `MEMBER` - Regular participant
- `ADMIN` - Room owner/admin

---

### 6.5 Find Archive by Original Room ID

```http
GET /api/back/archive/search/by-original-room/{originalRoomId}
```

**Description:** Check if a room has been archived and get its archive details.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|--------|-------------|
| originalRoomId | UUID | Original (non-archived) room ID |

**Response (200 OK):**
```json
{
  "id": "750e8400-e29b-41d4-a16-446655440001",
  "originalRoomId": "650e8400-e29b-41d4-a16-446655440000",
  "name": "Engineering Team",
  "archivedAt": "2024-01-20T15:45:00Z",
  "messageCount": 150,
  "participantCount": 5
}
```

**Error Response (404):**
```json
"Room not found in archive (may not be archived yet)"
```

---

## WebSocket Event Flow (Internal)

### Cross-Node Message Distribution via Redis Pub/Sub

The chat server uses Redis Pub/Sub for message distribution across multiple server instances.

**Pub/Sub Channels:**
| Channel Pattern | Purpose | Event Types |
|-----------------|---------|-------------|
| chat:room:{roomId} | Messages, edits, deletes | NEW_MESSAGE |
| signal:user:{userId} | WebRTC signaling (P2P) | SIGNAL_SDP, SIGNAL_ICE |
| typing:room:{roomId} | Typing indicators | TYPING |
| presence:room:{roomId} | User online/offline | PRESENCE |

**Flow Diagram (SEND_MSG):**
```
Client A (Node 1)
    │
    |-- WebSocket: SEND_MSG
    │
ChatWebSocketHandler (Node 1)
    │
    ├── 1. Authenticate JWT
    │       └── Extract userId from "sub" claim
    │
    ├── 2. Validate request
    │       └── Verify room exists, user is participant
    │
    ├── 3. Persist to PostgreSQL (reactive)
    │       └── INSERT INTO messages (...)
    │
    ├── 4. Publish to Redis Pub/Sub
    │       └── CHANNEL: chat:room:{roomId}
    │       └── PAYLOAD: {event: "NEW_MESSAGE", data: {...}}
    │
    └── 5. Echo to sender (WebSocket)
            └── NEW_MESSAGE event

Redis Pub/Sub Broker
    │
    └── Broadcasts to all subscribers
            ├── Node 2 → Client B (WebSocket: NEW_MESSAGE)
            └── Node 3 → Client C (WebSocket: NEW_MESSAGE)
```

---

## Database Schema Reference

### Main Tables (Hot Data)

```sql
chat_users          -- User profiles
rooms               -- Active rooms
room_participants   -- Room membership (composite key)
messages            -- Active messages (with soft delete & edit fields)
room_read_states    -- Per-user read markers (NOT IMPLEMENTED)
files               -- File upload records
```

### Archive Tables (Cold Data)

```sql
archived_rooms      -- Archived room metadata
archived_messages   -- Archived messages (read-only)
archived_participants -- Archived participants (read-only)
```

---

## Redis Data Structures

### Keys (with TTL)

| Pattern | Value | TTL | Purpose |
|---------|-------|-----|---------|
| user:presence:{userId} | "online" | 60s | User online status (auto-expire) |
| user:call:{userId} | "busy" or "idle" | 300s | WebRTC call state |
| unread:{roomId}:{userId} | Integer (count) | None | Unread message count (NOT IMPLEMENTED) |

### Pub/Sub Channels

| Channel Pattern | Purpose | Events |
|-----------------|---------|--------|
| chat:room:{roomId} | Messages, edits, deletes | NEW_MESSAGE |
| signal:user:{userId} | WebRTC signaling | SIGNAL_SDP, SIGNAL_ICE |
| typing:room:{roomId} | Typing indicators | TYPING |
| presence:room:{roomId} | User presence | PRESENCE (✅ FIXED) |

---

## File Storage (MinIO/S3)

### Presigned URL Expiration

| Operation | Expiration |
|-----------|------------|
| Upload URL | 3600s (1 hour) |
| Download URL | 3600s (1 hour) |

### File Size Constraints

| Constraint | Value |
|------------|-------|
| Max file size | 100 MB (104,857,600 bytes) |
| Min file size | 1 byte |

### Object Key Format

```
{userId}/{timestamp}-{random}-{filename}
```

Example:
```
550e8400-e29b-41d4-a16-4466554400/1704304000-abc123-document.pdf
```

---

## JWT Token Reference

### Generation (Dev API)

```bash
curl "http://localhost:8080/api/dev/token/550e8400-e29b-41d4-a16-446655440?name=Alice"
```

### Token Claims

```json
{
  "sub": "550e8400-e29b-41d4-a16-446655440000",
  "name": "Alice",
  "iss": "nexus-chat-engine",
  "iat": 1703980800,
  "exp": 1704585600
}
```

### Validation

All JWT tokens are validated using:
1. **Signature verification** - RSA-256 with public key
2. **Expiration check** - `exp` claim must be in future
3. **Issuer check** - `iss` must be `nexus-chat-engine`

**Note:** No `tenantId` validation - this is a single-application service.

---

## Monitoring & Observability

### Health Check

```http
GET /q/health
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

### Metrics

```http
GET /q/metrics
```

Available metrics:
- `vendor.database.pool.size` - JDBC connection pool size
- `vendor.http.requests.count` - HTTP request count
- Custom metrics for message throughput, Redis pub/sub

---

## Deployment Considerations

### Environment Variables

```bash
# Database
DB_USERNAME=myuser
DB_PASSWORD=postgres
DB_URL=jdbc:postgresql://localhost:5432/nexuschat
REACTIVE_DB_URL=postgresql://localhost:5432/nexuschat

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=admin
MINIO_SECRET_KEY=password123
MINIO_BUCKET=development

# Redis
QUARKUS_REDIS_HOSTS=redis://localhost:6379

# Profile
QUARKUS_PROFILE=dev  # or prod
```

---

## Security Best Practices

### Production Checklist

- [ ] Disable `/api/dev/*` endpoints in production
- [ ] Use strong RSA keys (4096-bit recommended)
- [ ] Enable HTTPS/TLS
- [ ] Configure rate limiting
- [ ] Enable audit logging
- [ ] Set up monitoring/alerting
- [ ] Configure Redis persistence
- [ ] Enable database backups
- [ ] Use secret management (Vault, AWS Secrets Manager)
- [ ] Enable CORS for trusted domains only

---

## Changelog

### 2025-01-05
- Fixed API paths (changed from `/api/admin/*` to `/api/back/admin/*`)
- Added complete enum values for all types
- Added nullable field markers throughout
- Added file size constraints (1-100MB)
- Added URL expiration times (3600s)
- Documented archived_by field
- Documented first_message_at and last_message_at tracking
- Updated JWT structure (removed tenantId)
- Added complete WebSocket event documentation
- Added Redis data structure reference
- Fixed PRESENCE event documentation
