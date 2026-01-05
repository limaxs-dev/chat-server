# NexusChat Engine - Frontend API Documentation

> RESTful APIs consumed by frontend/client applications

---

## Base URL

```
Production:  https://api.nexuschat.com
Development: http://localhost:8080
```

## Authentication

All API requests require a valid JWT token in the `Authorization` header:

```
Authorization: Bearer <JWT_TOKEN>
```

### JWT Token Structure

```json
{
  "sub": "550e8400-e29b-41d4-a16-446655440000",  // User ID (required)
  "name": "John Doe",                              // User display name (optional)
  "iss": "nexus-chat-engine",                       // Issuer (fixed)
  "iat": 1703980800,                               // Issued at timestamp
  "exp": 1704067200                                // Expiration timestamp
}
```

**Note:** This is a single-application service. There is NO `tenantId` claim or multi-tenancy support.

---

## API Endpoints

### 1. Room Management

#### 1.1 List Rooms

```http
GET /api/front/rooms?page=0&size=20
```

**Description:** Get paginated list of rooms where the current user is a participant. Results ordered by `updated_at` descending (most recently active first).

**Authentication:** Required (JWT token)

**Query Parameters:**
| Parameter | Type   | Default | Min | Max | Description           |
|-----------|--------|---------|-----|-----|-----------------------|
| page      | int    | 0       | 0   | -   | Page number (0-based) |
| size      | int    | 20      | 1   | 100 | Items per page        |

**Response (200 OK):**
```json
[
  {
    "id": "650e8400-e29b-41d4-a16-446655440000",
    "name": "Engineering Team",
    "type": "GROUP",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "id": "650e8400-e29b-41d4-a16-446655440002",
    "name": null,
    "type": "DIRECT",
    "createdAt": "2024-01-15T11:00:00Z"
  }
]
```

**Response Fields:**
| Field | Type | Nullable? | Description |
|-------|------|-----------|-------------|
| id | UUID | NO | Room unique identifier |
| name | string | **YES** | Room display name (null for DIRECT type rooms) |
| type | string | NO | Enum: `DIRECT` or `GROUP` |
| createdAt | string | NO | ISO 8601 timestamp |

**Room Types:**
| Value | Description | name field |
|-------|-------------|------------|
| DIRECT | 1-on-1 private chat | Always null |
| GROUP | Group chat (3+ users) | Required |

**Error Responses:**
| Status | Description |
|--------|-------------|
| 401 | Unauthorized (missing/invalid token) |

---

#### 1.2 Get Room Details

```http
GET /api/front/rooms/{id}
```

**Description:** Get details of a specific room. User must be a participant.

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| id | UUID | Room ID |

**Response (200 OK):**
```json
{
  "id": "650e8400-e29b-41d4-a16-446655440000",
  "name": "Engineering Team",
  "type": "GROUP",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Error Responses:**
| Status | Description |
|--------|-------------|
| 401 | Unauthorized |
| 404 | Room not found OR user is not a participant |

---

#### 1.3 Get Room Messages

```http
GET /api/front/rooms/{roomId}/messages?page=0&size=50
```

**Description:** Get paginated message history for a room (newest first). User must be a participant.

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| roomId | UUID | Room ID |

**Query Parameters:**
| Parameter | Type   | Default | Min | Max | Description |
|-----------|--------|---------|-----|-----|-------------|
| page | int | 0 | 0 | - | Page number (0-based) |
| size | int | 50 | 1 | 100 | Items per page |

**Response (200 OK):**
```json
[
  {
    "id": "750e8400-e29b-41d4-a16-446655440000",
    "roomId": "650e8400-e29b-41d4-a16-446655440000",
    "senderId": "550e8400-e29b-41d4-a16-446655440000",
    "type": "TEXT",
    "contentText": "Hello everyone!",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "id": "750e8400-e29b-41d4-a16-446655440001",
    "roomId": "650e8400-e29b-41d4-a16-446655440000",
    "senderId": "550e8400-e29b-41d4-a16-446655440001",
    "type": "IMAGE",
    "contentText": "Check this out!",
    "createdAt": "2024-01-15T10:31:00Z"
  }
]
```

**Response Fields:**
| Field | Type | Nullable? | Description |
|-------|------|-----------|-------------|
| id | UUID | NO | Message ID |
| roomId | UUID | NO | Room ID |
| senderId | UUID | NO | Sender user ID |
| type | string | NO | Message type (see enum below) |
| contentText | string | **YES** | Message text (null for FILE/IMAGE types) |
| createdAt | string | NO | ISO 8601 timestamp |

**Message Types:**
| Value | Description | contentText |
|-------|-------------|-------------|
| TEXT | Plain text message | Required |
| IMAGE | Image message | Optional (caption) |
| FILE | File attachment | Optional (description) |
| AUDIO | Audio/voice note | Optional (transcription) |
| VIDEO | Video message | Optional (caption) |
| SYSTEM | System-generated | Varies |
| VOICE_CALL | WebRTC call metadata | Call summary |

**Error Responses:**
| Status | Description |
|--------|-------------|
| 401 | Unauthorized |
| 404 | Room not found OR access denied |

**Note:** For FILE/IMAGE messages, file metadata is in `contentMeta` field (not included in this response for brevity). Use the file download endpoint to get the actual file.

---

### 2. Message Operations

#### 2.1 Get Messages (Alias)

```http
GET /api/front/messages/{roomId}?page=0&size=50
```

**Description:** Alias for `GET /api/front/rooms/{roomId}/messages`. Returns the same response.

---

### 3. File Download

#### 3.1 Get Download URL

```http
GET /api/front/files/{fileId}/download-url
```

**Description:** Get a presigned URL for file download. URL expires after 1 hour (3600 seconds).

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| fileId | UUID | File ID |

**Response (200 OK):**
```json
{
  "downloadUrl": "https://storage.example.com/files/...?expires=1704123456"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| downloadUrl | string | Presigned URL (valid for 1 hour) |

**Error Responses:**
| Status | Description |
|--------|-------------|
| 401 | Unauthorized |
| 404 | File not found |

**Note:** The presigned URL can be used directly with a GET request to download the file from MinIO/S3 without going through the chat server.

---

### 4. WebRTC Configuration

#### 4.1 Get WebRTC Config

```http
GET /api/front/config/webrtc
```

**Description:** Get STUN/TURN server configuration for WebRTC peer connections.

**Response (200 OK):**
```json
{
  "iceServers": [
    {
      "urls": ["stun:stun1.l.google.com:19302"]
    },
    {
      "urls": ["stun:stun2.l.google.com:19302"]
    }
  ]
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| iceServers | array | List of ICE server configurations |
| iceServers[].urls | array of string | STUN/TURN server URLs |

**Current Configuration:**
- Google STUN servers (public, no credentials)
- No TURN servers configured yet (ready for CoTurn integration)

**Usage:**
```javascript
const config = await fetch('/api/front/config/webrtc').then(r => r.json());
const pc = new RTCPeerConnection({ iceServers: config.iceServers });
```

---

### 5. Archive (Read-Only)

Archived rooms are NOT accessible via these frontend endpoints. Use the backend API (`/api/back/archive/*`) to access archived data.

---

## WebSocket API

### Connection

```http
WS /ws/chat?token=<JWT_TOKEN>
```

**Example:**
```javascript
const token = "eyJhbGciOiJSUzI1NiJ9...";
const ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${token}`);
```

### Event Format

All WebSocket messages use this envelope:

```json
{
  "event": "EVENT_TYPE",
  "traceId": "uuid-for-tracking",
  "data": { ... }
}
```

### Client → Server Events

#### SEND_MSG

Send a message to a room (real-time broadcast).

```json
{
  "event": "SEND_MSG",
  "traceId": "550e8400-e29b-41d4-a16-446655440000",
  "data": {
    "roomId": "650e8400-e29b-41d4-a16-446655440000",
    "type": "TEXT",
    "contentText": "Hello everyone!",
    "contentMeta": null,
    "clientRef": "msg-12345"
  }
}
```

**Data Fields:**
| Field | Type | Required? | Validation |
|-------|------|-----------|------------|
| roomId | UUID | YES | Must exist, user must be participant |
| type | string | YES | Must be valid MessageType |
| contentText | string | NO | Required for TEXT type |
| contentMeta | string/JSON | NO | File metadata for FILE/IMAGE types |
| clientRef | string | NO | For idempotency (optional) |

#### TYPING

Send typing indicator.

```json
{
  "event": "TYPING",
  "traceId": "uuid",
  "data": {
    "roomId": "650e8400-e29b-41d4-a16-446655440000",
    "isTyping": true
  }
}
```

**Data Fields:**
| Field | Type | Required? |
|-------|------|-----------|
| roomId | UUID | YES |
| isTyping | boolean | YES |

#### SIGNAL_SDP

Send WebRTC SDP offer/answer for P2P calls.

```json
{
  "event": "SIGNAL_SDP",
  "traceId": "uuid",
  "data": {
    "targetId": "750e8400-e29b-41d4-a16-446655440000",
    "type": "offer",
    "sdp": "v=0\\r\\no=- 123456789 2 IN IP4 127.0.0.1\\r\\n..."
  }
}
```

**Data Fields:**
| Field | Type | Required? | Values |
|-------|------|-----------|--------|
| targetId | UUID | YES | User to call |
| type | string | YES | `offer` or `answer` |
| sdp | string | YES | SDP content |

**Call Flow:**
1. Alice sends `SIGNAL_SDP` with `type: "offer"`
2. Server checks if Bob is busy (Redis: `user:call:{bobId}`)
3. If busy: Server sends `CALL_REJECTED` to Alice
4. If not busy: Server forwards to Bob, sets Alice as busy

#### SIGNAL_ICE

Send WebRTC ICE candidate.

```json
{
  "event": "SIGNAL_ICE",
  "traceId": "uuid",
  "data": {
    "targetId": "750e8400-e29b-41d4-a16-446655440000",
    "candidate": "candidate:1 1 UDP 2130706431 192.168.1.100 54400 typ host"
  }
}
```

**Data Fields:**
| Field | Type | Required? |
|-------|------|-----------|
| targetId | UUID | YES |
| candidate | string | YES |

#### ACK

Acknowledge message receipt.

```json
{
  "event": "ACK",
  "traceId": "uuid",
  "data": {
    "messageId": "750e8400-e29b-41d4-a16-446655440000"
  }
}
```

### Server → Client Events

#### PRESENCE (✅ FIXED)

User presence notification. Sent when users connect/disconnect.

```json
{
  "event": "PRESENCE",
  "traceId": "uuid",
  "data": {
    "userId": "550e8400-e29b-41d4-a16-446655440000",
    "userName": "Alice",
    "status": "online",
    "roomId": "650e8400-e29b-41d4-a16-446655440000"
  }
}
```

**Data Fields:**
| Field | Type | Values |
|-------|------|--------|
| userId | UUID | User who changed status |
| userName | string | User display name |
| status | string | `online` or `offline` |
| roomId | UUID | Room where event occurred |

**When Sent:**
- Immediately after WebSocket connects (for each room user is in)
- When another user in the same room connects/disconnects
- Immediately before WebSocket closes (offline event for self)

#### NEW_MESSAGE

New message in a room (broadcast via Redis pub/sub).

```json
{
  "event": "NEW_MESSAGE",
  "traceId": "uuid",
  "data": {
    "id": "750e8400-e29b-41d4-a16-446655440000",
    "roomId": "650e8400-e29b-41d4-a16-446655440000",
    "senderId": "550e8400-e29b-41d4-a16-446655440000",
    "type": "TEXT",
    "contentText": "Hello everyone!",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

**When Sent:**
- Immediately after message is persisted to database
- Echoed back to sender
- Broadcast to all participants in room via Redis pub/sub

#### TYPING

Typing indicator received.

```json
{
  "event": "TYPING",
  "traceId": "uuid",
  "data": {
    "roomId": "650e8400-e29b-41d4-a16-446655440000",
    "userId": "550e8400-e29b-41d4-a16-446655440000",
    "isTyping": true
  }
}
```

#### SIGNAL_SDP / SIGNAL_ICE

WebRTC signaling received. Same format as client → server.

#### CALL_REJECTED

Call rejected because target user is busy.

```json
{
  "event": "CALL_REJECTED",
  "traceId": "uuid",
  "data": {
    "callerId": "550e8400-e29b-41d4-a16-446655440000",
    "targetId": "750e8400-e29b-41d4-a16-446655440000",
    "status": "busy",
    "reason": "User is busy"
  }
}
```

**When Sent:**
- When trying to call a user who is already in a call
- Server checks Redis: `user:call:{targetId}`

---

## Error Responses

All errors follow this format:

```json
{
  "error": "Error message description"
}
```

| HTTP Status | Description |
|-------------|------------------------------|
| 400 | Bad Request |
| 401 | Unauthorized (missing/invalid token) |
| 403 | Forbidden (not implemented, no tenant support) |
| 404 | Not Found |
| 500 | Internal Server Error |

---

## Common Response Headers

| Header | Description |
|--------|-------------|
| Content-Type | application/json |

---

## Data Type Reference

### UUID Format
All UUIDs follow standard RFC 4122 format:
```
550e8400-e29b-41d4-a16-446655440000
```

### Timestamp Format
All timestamps are ISO 8601 in UTC:
```
2024-01-15T10:30:00Z
```

### Enum Values Summary

**Room Types:**
- `DIRECT` - 1-on-1 chat
- `GROUP` - Group chat

**Message Types:**
- `TEXT` - Plain text
- `IMAGE` - Image
- `FILE` - File attachment
- `AUDIO` - Audio/voice
- `VIDEO` - Video
- `SYSTEM` - System message
- `VOICE_CALL` - WebRTC call

**Participant Roles:**
- `MEMBER` - Regular participant
- `ADMIN` - Room admin

**Presence Status:**
- `online` - User connected
- `offline` - User disconnected

---

## Backend API Note

Room creation and message sending are **backend-only operations**:

- `POST /api/back/rooms` - Create room (used by backend services)
- `POST /api/back/messages` - Send message (used by backend services)

Frontend clients should use WebSocket for real-time message sending via `SEND_MSG` event.

---

## Limitations & Constraints

| Endpoint | Limit |
|----------|-------|
| List rooms | Max 100 per page |
| List messages | Max 100 per page |
| File upload | Max 100 MB, Min 1 byte |
| Download URL | Expires in 1 hour (3600s) |
| JWT token | Expires in 7 days |

---

## Changelog

### 2025-01-05
- Fixed API paths (changed from `/api/rooms` to `/api/front/rooms`)
- Removed tenantId references (single-application service)
- Added complete enum values for all types
- Added nullable field markers
- Documented PRESENCE event fix
- Updated JWT token structure
