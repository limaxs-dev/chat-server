# NexusChat Engine - K6 Integration Tests

This directory contains K6 performance and integration tests for the NexusChat Engine.

## Prerequisites

1. **K6 installed** - Download from https://k6.io/
2. **Server running** on `http://localhost:8080`
3. **PostgreSQL** running with test data loaded
4. **Redis** running
5. **MinIO** running (for file upload tests)

### Installation

```bash
# macOS
brew install k6

# Linux
sudo apt-get install k6

# Windows
# Download from https://k6.io/ or use chocolatey
choco install k6
```

## Test Files

| File | Description |
|------|-------------|
| `config.js` | Shared configuration and helper functions |
| `01-rest-api.js` | REST API endpoint tests |
| `02-websocket.js` | WebSocket connection and event tests |
| `03-file-upload.js` | File upload/download via MinIO tests |
| `04-integration-test.js` | Full end-to-end integration test |

## Running Tests

### Run All Tests

```bash
k6 run 01-rest-api.js
k6 run 02-websocket.js
k6 run 03-file-upload.js
k6 run 04-integration-test.js
```

### Run Specific Test

```bash
k6 run 01-rest-api.js
```

### With Custom Environment Variables

```bash
export BASE_URL=http://localhost:8080
export WS_URL=ws://localhost:8080/ws/chat
k6 run 01-rest-api.js
```

## Test Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost:8080` | Server base URL |
| `WS_URL` | `ws://localhost:8080/ws/chat` | WebSocket endpoint |

### Test Users

Tests use predefined users from `test-data.sql`:

| User ID | Name | Tenant |
|---------|------|--------|
| 550e8400-e29b-41d4-a716-446655440000 | Alice | test-tenant |
| 550e8400-e29b-41d4-a716-446655440001 | Bob | test-tenant |
| 550e8400-e29b-41d4-a716-446655440002 | Charlie | test-tenant |
| 550e8400-e29b-41d4-a716-446655440003 | Diana | test-tenant |

### Test Rooms

Tests use predefined rooms from `test-data.sql`:

| Room ID | Type | Name |
|---------|------|------|
| 650e8400-e29b-41d4-a716-446655440000 | GROUP | Engineering Team |
| 650e8400-e29b-41d4-a716-446655440001 | GROUP | Product Discussion |
| 650e8400-e29b-41d4-a716-446655440002 | DIRECT | Alice & Bob |

## Test Coverage

### 01-rest-api.js

- JWT token generation
- List rooms
- Get room by ID
- Create room (GROUP and DIRECT)
- Get messages with pagination
- Send messages (TEXT type)
- Message idempotency (clientRef)
- Unauthorized access handling
- Invalid room ID handling

### 02-websocket.js

- WebSocket connection with JWT
- Send messages via WebSocket
- Receive NEW_MESSAGE events
- Typing indicators
- WebRTC signaling (SIGNAL_SDP)
- Message acknowledgment (ACK)
- Presence events
- Multiple rapid messages

### 03-file-upload.js

- Get upload URL for various file types (PDF, PNG, MP4)
- Confirm file upload
- Get download URL
- Large file rejection
- Invalid content type handling
- Multiple sequential uploads
- Non-existent file ID handling

### 04-integration-test.js

Complete end-to-end workflow:
1. Authentication
2. Room creation and management
3. File upload
4. Messaging via REST API
5. WebSocket connection and messaging
6. Typing indicators
7. File download
8. Multi-user scenario (Alice & Bob)
9. Error handling

## Database Verification

After running tests, verify data in the database:

```sql
-- Check newly created rooms
SELECT id, type, name, tenant_id FROM rooms ORDER BY created_at DESC LIMIT 5;

-- Check messages
SELECT id, room_id, sender_id, type, LEFT(content_text, 50) as content
FROM messages ORDER BY created_at DESC LIMIT 10;

-- Check files
SELECT id, file_name, mime, size, is_confirmed FROM files ORDER BY created_at DESC;

-- Check room participants
SELECT room_id, user_id, role FROM room_participants;

-- Message count per room
SELECT room_id, COUNT(*) as message_count
FROM messages GROUP BY room_id;
```

## Expected Results

### Successful Test Run

```
✓ GET /api/dev/token/{userId} - Status 200
✓ GET /api/rooms - Status 200
✓ POST /api/rooms - Status 201
✓ POST /api/messages - Status 201
✓ WebSocket - Connection successful
✓ WebSocket - Received NEW_MESSAGE event
...

checks.........................: 98% ✓ 49  ✗ 1
data_received..................: 12 KB 12 kB
http_req_blocked...............: avg=2.5ms min=1ms med=2ms max=10ms
http_req_connecting............: avg=1ms min=0 med=1ms max=5ms
http_req_duration..............: avg=150ms min=50ms med=120ms max=500ms
http_req_failed................: 0.00% ✓ 50  ✗ 0
http_req_receiving.............: avg=10ms min=1ms med=8ms max=50ms
http_req_sending...............: avg=2ms min=0ms med=1ms max=10ms
http_req_tls_handshaking.......: avg=0s min=0s med=0s max=0s
http_req_waiting...............: avg=138ms min=45ms med=110ms max=450ms
iteration_duration.............: avg=15s min=15s med=15s max=15s
iterations.....................: 1
vus............................: 1
vus_max........................: 1
```

## Troubleshooting

### Connection Refused

Make sure the server is running:
```bash
./gradlew quarkusDev
```

### Authentication Failed

Verify JWT is configured correctly in `application.properties`:
```properties
mp.jwt.verify.publickey.location=keys/public-key.pem
mp.jwt.verify.issuer=nexus-chat-engine
```

### WebSocket Connection Failed

Ensure WebSocket endpoint is accessible:
```bash
curl -i -N \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Host: localhost:8080" \
  -H "Origin: http://localhost:8080" \
  http://localhost:8080/ws/chat
```

### File Upload Failed

Verify MinIO is running:
```bash
curl http://localhost:9000
```

### Database Connection Failed

Check PostgreSQL is running and database exists:
```bash
psql -U myuser -d nexuschat -c "SELECT 1;"
```

## Performance Baselines

These are expected performance metrics on a development machine:

| Endpoint | p(95) Duration |
|----------|----------------|
| GET /api/rooms | < 100ms |
| POST /api/rooms | < 200ms |
| GET /api/messages/{roomId} | < 150ms |
| POST /api/messages | < 200ms |
| GET /api/files/upload-url | < 100ms |
| WebSocket connection | < 500ms |
| WebSocket message | < 100ms |

## CI/CD Integration

Add to your CI pipeline:

```yaml
# Example GitHub Actions
- name: Run K6 Tests
  run: |
    k6 run k6test/01-rest-api.js
    k6 run k6test/02-websocket.js
    k6 run k6test/03-file-upload.js
    k6 run k6test/04-integration-test.js
```

## Additional Resources

- K6 Documentation: https://k6.io/docs/
- Integration Guide: `../integration.md`
- API Documentation: http://localhost:8080/swagger-ui
