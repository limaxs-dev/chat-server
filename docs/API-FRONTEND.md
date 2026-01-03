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
  "sub": "550e8400-e29b-41d4-a716-446655440000",  // User ID
  "iss": "tenant-001",                             // Tenant ID
  "tenantId": "tenant-001",                        // Tenant ID (custom claim)
  "name": "John Doe",                              // User display name
  "exp": 1704067200,                               // Expiration timestamp
  "iat": 1703980800                                // Issued at timestamp
}
```

---

## API Endpoints

### 1. Room Management

#### 1.1 List Rooms

```http
GET /api/rooms?page=0&size=20
```

**Description:** Get paginated list of rooms for the current tenant.

**Query Parameters:**
| Parameter | Type   | Default | Description           |
|-----------|--------|---------|-----------------------|
| page      | int    | 0       | Page number (0-based) |
| size      | int    | 20      | Items per page        |

**Response (200 OK):**
```json
[
  {
    "id": "650e8400-e29b-41d4-a716-446655440000",
    "name": "Engineering Team",
    "type": "GROUP",
    "tenantId": "tenant-001",
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### 1.2 Create Room

```http
POST /api/rooms
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Engineering Team",
  "type": "GROUP",
  "participantIds": [
    "550e8400-e29b-41d4-a716-446655440000",
    "650e8400-e29b-41d4-a716-446655440000"
  ]
}
```

**Field Descriptions:**
| Field           | Type   | Required | Description                           |
|-----------------|--------|----------|---------------------------------------|
| name            | string | yes      | Room display name                     |
| type            | string | yes      | `DIRECT` or `GROUP`                   |
| participantIds  | array  | yes      | List of user IDs to add to the room   |

**Response (201 Created):**
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440000",
  "name": "Engineering Team",
  "type": "GROUP",
  "tenantId": "tenant-001",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Flow Diagram:**
```
Frontend                    RoomResource              Database
    |                            |                         |
    |-- POST /api/rooms ------>|                         |
    |  (CreateRoomRequest)      |                         |
    |                            |-- 1. Create Room --->|
    |                            |                         |
    |                            |-- 2. Create Participants for each user --->|
    |                            |                         |
    |                            |<-- Room saved ---------|
    |<-- 201 Created ------------|                         |
    |  (RoomResponse)            |                         |
```

---

#### 1.3 Get Room Details

```http
GET /api/rooms/{roomId}
```

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| roomId    | UUID   | Room ID     |

**Response (200 OK):**
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440000",
  "name": "Engineering Team",
  "type": "GROUP",
  "tenantId": "tenant-001",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

#### 1.4 Get Room Messages

```http
GET /api/rooms/{roomId}/messages?page=0&size=50
```

**Description:** Get paginated message history for a room (newest first).

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| roomId    | UUID   | Room ID     |

**Query Parameters:**
| Parameter | Type   | Default | Description           |
|-----------|--------|---------|-----------------------|
| page      | int    | 0       | Page number (0-based) |
| size      | int    | 50      | Items per page        |

**Response (200 OK):**
```json
[
  {
    "id": "ce1625b3-2f3f-46cb-a36a-84a026e4c07b",
    "roomId": "650e8400-e29b-41d4-a716-446655440000",
    "senderId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "TEXT",
    "contentText": "Hello everyone!",
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

**Flow Diagram:**
```
Frontend                    RoomResource              Database
    |                            |                         |
    |-- GET /api/rooms/{id}/messages ------>|
    |                            |                         |
    |                            |-- Verify room tenant ->|
    |                            |                         |
    |                            |-- Get messages ------->|
    |                            |   (ORDER BY created_at DESC)  |
    |                            |                         |
    |                            |<-- Messages ----------|
    |<-- 200 OK -----------------|                         |
    |  (List<MessageResponse>)   |                         |
```

---

### 2. Message Operations

#### 2.1 Send Message

```http
POST /api/messages
Content-Type: application/json
```

**Request Body:**
```json
{
  "roomId": "650e8400-e29b-41d4-a716-446655440000",
  "type": "TEXT",
  "contentText": "Hello from frontend!",
  "contentMeta": null,
  "clientRef": "msg-12345"
}
```

**Field Descriptions:**
| Field        | Type   | Required | Description                                                      |
|--------------|--------|----------|------------------------------------------------------------------|
| roomId       | UUID   | yes      | Target room ID                                                   |
| type         | string | yes      | `TEXT`, `FILE`, `IMAGE`, `VIDEO`, `AUDIO`                        |
| contentText  | string | no       | Message text content                                             |
| contentMeta  | json   | no       | Additional metadata (e.g., file info)                            |
| clientRef    | string | no       | Client-side unique ID for idempotency (prevents duplicates)      |

**Response (201 Created):**
```json
{
  "id": "ce1625b3-2f3f-46cb-a36a-84a026e4c07b",
  "roomId": "650e8400-e29b-41d4-a716-446655440000",
  "senderId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "TEXT",
  "contentText": "Hello from frontend!",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Error Responses:**
| Status | Description                  |
|--------|------------------------------|
| 404    | Room not found               |
| 409    | Duplicate client ref (idempotency) |

**Flow Diagram:**
```
Frontend                    MessageResource            Database
    |                            |                         |
    |-- POST /api/messages ---->|                         |
    |  (SendMessageRequest)      |                         |
    |                            |                         |
    |                            |-- Verify room exists ->|
    |                            |                         |
    |                            |-- Check idempotency -->|
    |                            |   (if clientRef provided)     |
    |                            |                         |
    |                            |-- Create message ---->|
    |                            |                         |
    |                            |<-- Message saved -----|
    |<-- 201 Created ------------|                         |
    |  (MessageResponse)         |                         |

Note: Message is NOT broadcast via WebSocket when sent via REST API.
For real-time delivery, use WebSocket: SEND_MSG event.
```

---

#### 2.2 Get Messages

```http
GET /api/messages/{roomId}?page=0&size=50
```

**Description:** Alias for GET /api/rooms/{roomId}/messages

**Response (200 OK):** Same as Get Room Messages

---

### 3. File Upload/Download

#### 3.1 Get Upload URL

```http
POST /api/files/upload-url?fileName=document.pdf&fileSize=1024000&contentType=application/pdf
```

**Description:** Get a presigned URL for direct file upload to MinIO/S3.

**Query Parameters:**
| Parameter   | Type   | Required | Description                    |
|-------------|--------|----------|--------------------------------|
| fileName    | string | yes      | Original filename              |
| fileSize    | long   | yes      | File size in bytes             |
| contentType | string | yes      | MIME type (e.g., application/pdf) |

**Constraints:**
- Maximum file size: 100 MB (104,857,600 bytes)
- Minimum file size: 1 byte

**Response (200 OK):**
```json
{
  "fileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "uploadUrl": "https://storage.example.com/files/...",
  "objectKey": "tenant-001/user-123/4567890-document.pdf"
}
```

**Flow Diagram:**
```
Frontend                   FileResource              Database                MinIO
    |                            |                         |                     |
    |-- POST /upload-url ------>|                         |                     |
    |  (fileName, size, type)    |                         |                     |
    |                            |                         |                     |
    |                            |-- 1. Create FileMetadata record ------------>|
    |                            |                         |                     |
    |                            |-- 2. Generate presigned URL ------------------>|
    |                            |                         |                     |
    |                            |<-- Presigned URL ------|                     |
    |<-- 200 OK (uploadUrl) -----|                         |                     |
    |                            |                         |                     |
    |-- PUT {uploadUrl} ------------------------------------------------------->|
    |  (Direct upload to MinIO)  |                         |                     |
    |                            |                         |                     |
    |-- POST /files/{fileId}/confirm -->|                 |                     |
    |                            |                         |                     |
    |                            |-- Update confirmed=true -------------->|
    |                            |                         |                     |
    |<-- 204 No Content ---------|                         |                     |
```

---

#### 3.2 Confirm Upload

```http
POST /api/files/{fileId}/confirm
```

**Description:** Mark file upload as complete. Required after uploading to presigned URL.

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| fileId    | UUID   | File ID     |

**Response (204 No Content)**

---

#### 3.3 Get Download URL

```http
GET /api/files/{fileId}/download-url
```

**Description:** Get a presigned URL for file download.

**Path Parameters:**
| Parameter | Type   | Description |
|-----------|--------|-------------|
| fileId    | UUID   | File ID     |

**Response (200 OK):**
```json
{
  "downloadUrl": "https://storage.example.com/files/..."
}
```

---

### 4. WebRTC Configuration

#### 4.1 Get WebRTC Config

```http
GET /api/config/webrtc
```

**Description:** Get STUN/TURN server configuration for WebRTC calls.

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

**Flow Diagram:**
```
Frontend                   ConfigResource
    |                            |
    |-- GET /api/config/webrtc ->|
    |                            |
    |<-- 200 OK -----------------|
    |  { iceServers: [...] }     |
    |                            |
    |-- Use ICE servers for WebRTC PeerConnection
```

---

### 5. Archive (Read-Only)

#### 5.1 List Archived Rooms

```http
GET /api/archive/rooms?page=0&size=20
```

**Description:** Get paginated list of archived rooms for the current tenant.

**Response (200 OK):**
```json
[
  {
    "id": "750e8400-e29b-41d4-a716-446655440001",
    "originalRoomId": "650e8400-e29b-41d4-a716-446655440000",
    "name": "Engineering Team",
    "type": "GROUP",
    "archivedAt": "2024-01-20T15:45:00Z",
    "messageCount": 150,
    "participantCount": 5
  }
]
```

---

#### 5.2 Get Archived Room Details

```http
GET /api/archive/rooms/{archivedRoomId}
```

**Response (200 OK):**
```json
{
  "id": "750e8400-e29b-41d4-a716-446655440001",
  "originalRoomId": "650e8400-e29b-41d4-a716-446655440000",
  "name": "Engineering Team",
  "type": "GROUP",
  "archivedAt": "2024-01-20T15:45:00Z",
  "messageCount": 150,
  "participantCount": 5,
  "archivedBy": "admin-user-id"
}
```

---

#### 5.3 Get Archived Messages

```http
GET /api/archive/rooms/{archivedRoomId}/messages?page=0&size=50
```

**Description:** Get paginated archived messages (oldest first).

**Response (200 OK):**
```json
[
  {
    "id": "message-id-1",
    "archivedRoomId": "750e8400-e29b-41d4-a716-446655440001",
    "senderId": "550e8400-e29b-41d4-a716-446655440000",
    "senderName": "John Doe",
    "type": "TEXT",
    "contentText": "First message in room",
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### 5.4 Get Archived Participants

```http
GET /api/archive/rooms/{archivedRoomId}/participants
```

**Response (200 OK):**
```json
[
  {
    "id": "participant-id-1",
    "archivedRoomId": "750e8400-e29b-41d4-a716-446655440001",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "userName": "John Doe",
    "role": "ADMIN"
  }
]
```

---

#### 5.5 Find Archive by Original Room ID

```http
GET /api/archive/search/by-original-room/{originalRoomId}
```

**Description:** Check if a room has been archived and get its archive details.

**Path Parameters:**
| Parameter        | Type   | Description               |
|------------------|--------|---------------------------|
| originalRoomId   | UUID   | Original (non-archived) room ID |

**Response (200 OK):**
```json
{
  "id": "750e8400-e29b-41d4-a716-446655440001",
  "originalRoomId": "650e8400-e29b-41d4-a716-446655440000",
  "name": "Engineering Team",
  "archivedAt": "2024-01-20T15:45:00Z",
  "messageCount": 150,
  "participantCount": 5
}
```

**Error Response (404 Not Found):**
```json
"Room not found in archive (may not be archived yet)"
```

---

## WebSocket API

### Connection

```
ws://localhost:8080/ws/chat?token=<JWT_TOKEN>
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
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "roomId": "650e8400-e29b-41d4-a716-446655440000",
    "type": "TEXT",
    "contentText": "Hello everyone!",
    "contentMeta": null,
    "clientRef": "msg-12345"
  }
}
```

**Flow Diagram:**
```
Client A                 ChatWebSocketHandler         RedisPubSub           Client B
    |                            |                         |                     |
    |-- SEND_MSG -------------->|                         |                     |
    |                            |                         |                     |
    |                            |-- 1. Persist to PostgreSQL ------------------>|
    |                            |                         |                     |
    |                            |-- 2. Publish to Redis pub/sub ------------->|
    |                            |   (chat:room:{roomId})  |                     |
    |                            |                         |                     |
    |<-- NEW_MESSAGE (echo) -----|                         |                     |
    |                            |                         |                     |
    |                            |                         |-- NEW_MESSAGE ---->|
    |                            |                         |   (via Redis)       |
```

#### TYPING

Send typing indicator.

```json
{
  "event": "TYPING",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "roomId": "650e8400-e29b-41d4-a716-446655440000",
    "isTyping": true
  }
}
```

**Flow Diagram:**
```
Client A                 ChatWebSocketHandler         RedisPubSub           Client B
    |                            |                         |                     |
    |-- TYPING ---------------->|                         |                     |
    |                            |                         |                     |
    |                            |-- Publish to Redis (typing:room:{roomId}) --->|
    |                            |                         |                     |
    |                            |                         |-- TYPING --------->|
    |                            |                         |   (via Redis)       |
```

#### SIGNAL_SDP

Send WebRTC SDP offer/answer for P2P calls.

```json
{
  "event": "SIGNAL_SDP",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "targetId": "750e8400-e29b-41d4-a716-446655440000",
    "type": "offer",
    "sdp": "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n..."
  }
}
```

**Flow Diagram:**
```
Alice                    ChatWebSocketHandler         RedisPubSub           Bob
    |                            |                         |                     |
    |-- SIGNAL_SDP (offer) ---->|                         |                     |
    |  (targetId: Bob)           |                         |                     |
    |                            |                         |                     |
    |                            |-- 1. Check if Bob is busy (Redis) --------->|
    |                            |                         |                     |
    |                            |-- 2. Set Alice as busy (Redis) ----------->|
    |                            |                         |                     |
    |                            |-- 3. Publish to Redis (signal:user:{bobId}) --->|
    |                            |                         |                     |
    |                            |                         |-- SIGNAL_SDP ---->|
    |                            |                         |   (via Redis)       |
```

#### SIGNAL_ICE

Send WebRTC ICE candidate.

```json
{
  "event": "SIGNAL_ICE",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "targetId": "750e8400-e29b-41d4-a716-446655440000",
    "candidate": "candidate:1 1 UDP 2130706431 192.168.1.100 54400 typ host",
    "sdpMid": "audio",
    "sdpMLineIndex": 0
  }
}
```

#### ACK

Acknowledge message receipt.

```json
{
  "event": "ACK",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "messageId": "ce1625b3-2f3f-46cb-a36a-84a026e4c07b"
  }
}
```

### Server → Client Events

#### PRESENCE

User presence notification (sent on connection).

```json
{
  "event": "PRESENCE",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "userName": "John Doe",
    "status": "online"
  }
}
```

#### NEW_MESSAGE

New message in a room (broadcast via Redis pub/sub).

```json
{
  "event": "NEW_MESSAGE",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "id": "ce1625b3-2f3f-46cb-a36a-84a026e4c07b",
    "roomId": "650e8400-e29b-41d4-a716-446655440000",
    "senderId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "TEXT",
    "contentText": "Hello everyone!",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

#### SIGNAL_SDP

WebRTC SDP offer/answer received.

#### SIGNAL_ICE

WebRTC ICE candidate received.

#### TYPING

Typing indicator received.

#### CALL_REJECTED

Call rejected because target user is busy.

```json
{
  "event": "CALL_REJECTED",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "callerId": "550e8400-e29b-41d4-a716-446655440000",
    "targetId": "750e8400-e29b-41d4-a716-446655440000",
    "status": "busy",
    "reason": "User is busy"
  }
}
```

---

## Error Responses

All errors follow this format:

```json
{
  "error": "Error message description"
}
```

| HTTP Status | Description                  |
|-------------|------------------------------|
| 400         | Bad Request                  |
| 401         | Unauthorized (missing/invalid token) |
| 403         | Forbidden (tenant mismatch)  |
| 404         | Not Found                    |
| 409         | Conflict (duplicate)         |
| 500         | Internal Server Error        |

---

## Rate Limiting

Currently no rate limiting is implemented. Consider adding for production:

| Endpoint  | Recommended Limit |
|-----------|-------------------|
| POST /api/messages | 100/minute per user |
| POST /api/files/upload-url | 20/minute per user |
| WebSocket | 1000 messages/minute per connection |

---

## Common Response Headers

| Header            | Description                  |
|-------------------|------------------------------|
| Content-Type      | application/json             |
| X-Request-ID      | Unique request identifier    |
| X-RateLimit-Remaining | Requests left in window  |
