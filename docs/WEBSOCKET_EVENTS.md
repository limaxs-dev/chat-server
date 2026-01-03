# WebSocket Event Schemas

This document describes all WebSocket events used in NexusChat Engine for client synchronization.

## Event Envelope

All WebSocket messages use a JSON envelope with the following structure:

```json
{
  "event": "EVENT_TYPE",
  "traceId": "uuid",
  "data": { /* event-specific payload */ }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `event` | string | The event type identifier |
| `traceId` | string (UUID) | Unique identifier for tracking the message flow |
| `data` | object | Event-specific payload (varies by event type) |

---

## Client → Server Events

### SEND_MSG
Send a text or media message to a room.

**Event Type:** `SEND_MSG`

**Data Schema:**
```json
{
  "roomId": "uuid",
  "type": "TEXT | IMAGE | VIDEO | AUDIO | FILE",
  "contentText": "string (optional)",
  "contentMeta": "object (optional)",
  "clientRef": "uuid (optional)"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `roomId` | UUID | Yes | Target room ID |
| `type` | enum | Yes | Message type |
| `contentText` | string | No | Text content for TEXT messages |
| `contentMeta` | object | No | Metadata for media messages (URL, dimensions, etc.) |
| `clientRef` | UUID | No | Client-side reference for idempotency |

**Server Response:** `NEW_MESSAGE` event

---

### TYPING
Send typing indicator to a room.

**Event Type:** `TYPING`

**Data Schema:**
```json
{
  "roomId": "uuid",
  "isTyping": boolean
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `roomId` | UUID | Yes | Target room ID |
| `isTyping` | boolean | Yes | Whether user is currently typing |

**Server Broadcast:** `TYPING` event to all room subscribers

---

### SIGNAL_SDP
Send WebRTC SDP offer/answer for peer-to-peer call.

**Event Type:** `SIGNAL_SDP`

**Data Schema:**
```json
{
  "targetId": "uuid",
  "type": "offer | answer | rollback",
  "sdp": "string"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `targetId` | UUID | Yes | Recipient user ID |
| `type` | enum | Yes | SDP type (offer/answer/rollback) |
| `sdp` | string | Yes | SDP payload |

**Behavior:**
- Server acts as a pure relay, forwarding to `targetId`
- If `targetId` is busy (in another call), server responds with `CALL_REJECTED`
- On successful `offer`, caller is marked as busy (TTL: 5 minutes)

---

### SIGNAL_ICE (NEW)
Send WebRTC ICE candidate for peer-to-peer connection establishment.

**Event Type:** `SIGNAL_ICE`

**Data Schema:**
```json
{
  "targetId": "uuid",
  "candidate": "string",
  "sdpMid": "string (optional)",
  "sdpMLineIndex": 0
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `targetId` | UUID | Yes | Recipient user ID |
| `candidate` | string | Yes | ICE candidate string |
| `sdpMid` | string | No | SDP media identifier |
| `sdpMLineIndex` | number | Yes | SDP media line index |

**Behavior:**
- Server acts as a pure relay, forwarding candidate to `targetId` via Redis pub/sub
- Used during WebRTC connection establishment for NAT traversal

---

### ACK
Acknowledge message receipt.

**Event Type:** `ACK`

**Data Schema:**
```json
{
  "messageId": "uuid"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `messageId` | UUID | Yes | Message ID being acknowledged |

**Behavior:**
- Used for read receipt tracking (implementation pending)

---

## Server → Client Events

### NEW_MESSAGE
Broadcasted when a new message is sent to a room.

**Event Type:** `NEW_MESSAGE`

**Data Schema:**
```json
{
  "id": "uuid",
  "roomId": "uuid",
  "senderId": "uuid",
  "type": "TEXT | IMAGE | VIDEO | AUDIO | FILE",
  "contentText": "string (optional)",
  "contentMeta": "object (optional)",
  "createdAt": "ISO-8601 datetime"
}
```

---

### TYPING
Broadcasted when a user starts/stops typing in a room.

**Event Type:** `TYPING`

**Data Schema:**
```json
{
  "roomId": "uuid",
  "userId": "uuid",
  "isTyping": boolean
}
```

---

### PRESENCE
Sent when a user comes online or goes offline.

**Event Type:** `PRESENCE`

**Data Schema:**
```json
{
  "userId": "uuid",
  "userName": "string",
  "status": "online | offline"
}
```

**Presence TTL:** 60 seconds (auto-renewed on WebSocket activity)

---

### SIGNAL_SDP
Relayed WebRTC SDP signal from another user.

**Event Type:** `SIGNAL_SDP`

**Data Schema:**
```json
{
  "targetId": "uuid",
  "type": "offer | answer | rollback",
  "sdp": "string"
}
```

---

### SIGNAL_ICE (NEW)
Relayed WebRTC ICE candidate from another user.

**Event Type:** `SIGNAL_ICE`

**Data Schema:**
```json
{
  "targetId": "uuid",
  "candidate": "string",
  "sdpMid": "string (optional)",
  "sdpMLineIndex": 0
}
```

---

### CALL_REJECTED (NEW)
Sent when a call offer is rejected because the target user is busy.

**Event Type:** `CALL_REJECTED`

**Data Schema:**
```json
{
  "callerId": "uuid",
  "targetId": "uuid",
  "status": "busy",
  "reason": "string"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `callerId` | UUID | User who initiated the call |
| `targetId` | UUID | User who was called |
| `status` | string | Rejection reason (`"busy"` for in-call) |
| `reason` | string | Human-readable explanation |

**Triggered when:**
- Caller sends `SIGNAL_SDP` with `type: "offer"`
- Target user's call state is `"busy"` (stored in Redis: `user:call:{user_id}`)

---

## Call State Management

### Redis Keys

| Key Pattern | Value | TTL |
|-------------|-------|-----|
| `user:call:{user_id}` | `"busy"` | 300s (5 min) |

### Call Lifecycle

1. **Call Started:**
   - Caller sends `SIGNAL_SDP` with `type: "offer"`
   - Server checks if `targetId` is busy
   - If not busy, server sets `user:call:{callerId}` = `"busy"` (TTL 5min)

2. **Call Ended:**
   - Client should send explicit end-call signal (implementation TBD)
   - Or wait for Redis TTL expiration

3. **Busy State:**
   - Any new `offer` to a busy user is automatically rejected
   - Caller receives `CALL_REJECTED` event with `status: "busy"`

---

## REST API Endpoints

### GET /api/config/webrtc
Get WebRTC ICE server configuration.

**Response:**
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

**Future CoTurn Integration:**
```json
{
  "iceServers": [
    {
      "urls": ["turn:your-turn-server.com:3478"],
      "username": "username",
      "credential": "credential"
    }
  ]
}
```

---

### GET /api/rooms/{roomId}/messages
Get paginated message history for a room.

**Query Parameters:**
- `page` (default: 0) - Page number
- `size` (default: 50) - Messages per page

**Response:**
```json
[
  {
    "id": "uuid",
    "roomId": "uuid",
    "senderId": "uuid",
    "type": "TEXT",
    "contentText": "string",
    "contentMeta": null,
    "createdAt": "2024-01-01T00:00:00",
    "editedAt": null
  }
]
```

**Notes:**
- Messages are ordered by `createdAt` DESC (newest first)
- Tenant isolation enforced (user can only access their tenant's rooms)
